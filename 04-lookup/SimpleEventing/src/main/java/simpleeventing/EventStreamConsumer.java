package simpleeventing;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import simplemessaging.IAmAHandler;
import simplemessaging.IAmAMessage;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

/**
 * Reads records from a Kafka topic and hands them to application code.
 * <p>
 * It is the same four stages as the message pump -- Get, Translate, Dispatch, Handle -- and then the
 * fifth thing, the one that decides everything about failure, is different:
 * <pre>
 *   On a queue:  acknowledge this message. The broker holds the others.
 *   On a stream: commit this offset. It means "I am past everything up to here."
 * </pre>
 * <b>An offset is a bookmark, not a lock.</b> There is no per-record acknowledgement, so there is
 * nothing to withhold for one record and grant for another. You are either past a point in the log
 * or you are not.
 * <p>
 * Which means every mechanism exercise 2 relied on is simply absent:
 * <pre>
 *   requeue                 -- nothing to hand back
 *   requeue with delay      -- nothing holding it, so nothing to hold it longer
 *   reject                  -- nothing to route it away
 *   dead letter queue       -- nothing to move it there
 *   redelivery count        -- nothing counting
 * </pre>
 * This consumer takes the default answer, which is the one most frameworks take for you:
 * <b>retry in place.</b> Read what that does to a partition, then read PROBE.md.
 */
public final class EventStreamConsumer<T extends IAmAMessage> implements AutoCloseable {
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(1);

    private final Function<String, T> mapper;
    private final IAmAHandler<T> handler;
    private final Consumer<String, String> consumer;
    private final String topic;
    private final String consumerGroup;

    private volatile boolean stopping;

    public EventStreamConsumer(Function<String, T> mapper, IAmAHandler<T> handler, Class<T> type) {
        this(mapper, handler, type, Stream.CONSUMER_GROUP, Stream.BOOTSTRAP_SERVERS);
    }

    public EventStreamConsumer(Function<String, T> mapper, IAmAHandler<T> handler, Class<T> type,
                               String consumerGroup, String bootstrapServers) {
        this.mapper = mapper;
        this.handler = handler;
        this.topic = Stream.topicFor(type);
        this.consumerGroup = consumerGroup;

        // Either end may create the topic, so it does not matter which you start first.
        Stream.ensureTopicExists(topic, bootstrapServers);

        Properties config = new Properties();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        // Start at the beginning of the log the first time this group ever reads it.
        // A queue has no equivalent of this setting, because a queue has no past.
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Commit when we say so. Auto-commit on a timer would move the bookmark past records we
        // have not finished with, which is the stream's version of acking early.
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // Notice a change to the topic within seconds rather than within the default five minutes.
        // reset.sh deletes and recreates the topic, and a consumer left running across that would
        // otherwise go deaf for as long as its cached metadata says the old topic is still there.
        config.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, 5000);

        consumer = new KafkaConsumer<>(config);
        consumer.subscribe(List.of(topic));
    }

    /** Ask the consumer to stop. {@code wakeup()} is the only method safe to call from elsewhere. */
    public void stop() {
        stopping = true;
        consumer.wakeup();
    }

    public void run() throws Exception {
        System.out.printf("Following %s as group '%s'%n", topic, consumerGroup);

        try {
            while (!stopping) {
                // GET. A poll returns a batch, which is the first difference from basicGet: the
                // unit the broker hands you is a page of the log, not one message.
                ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);

                // A partition we have wound back is done for this batch: the records we already
                // hold for it are the ones we are about to re-read, so processing them now would be
                // processing them out of order.
                Set<TopicPartition> stalled = new HashSet<>();

                for (ConsumerRecord<String, String> record : records) {
                    TopicPartition partition = new TopicPartition(record.topic(), record.partition());
                    if (stalled.contains(partition)) {
                        continue;
                    }

                    String where = "p" + record.partition() + "@" + record.offset();

                    try {
                        // TRANSLATE
                        T message = mapper.apply(record.value());

                        // DISPATCH and HANDLE
                        handler.handle(message);

                        // Move the bookmark. Everything up to and including this offset is done --
                        // which is why the committed offset is this record's offset plus one.
                        consumer.commitSync(Map.of(partition, new OffsetAndMetadata(record.offset() + 1)));
                        System.out.println("  committed " + where);
                    } catch (Exception e) {
                        System.out.printf("  FAILED %s: %s%n", where, e.getMessage());
                        System.out.println("  there is no nack, no requeue and no dead letter topic, so: retry in place");

                        // Wind the bookmark back to this record and read it again. The partition
                        // stops here until this record succeeds -- which, if it never can, is
                        // forever. The other partitions carry on, perfectly happily.
                        consumer.seek(partition, record.offset());
                        stalled.add(partition);
                        Thread.sleep(RETRY_DELAY.toMillis());
                    }
                }
            }
        } catch (WakeupException e) {
            // stop() was called. The expected ending.
        } finally {
            // Leave the group tidily so the next run does not wait for a session timeout.
            consumer.close();
        }
    }

    @Override
    public void close() {
        // close() is idempotent, and run() has already closed it on the normal path.
        consumer.close();
    }
}
