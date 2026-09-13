package simpleeventing;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import simplemessaging.IAmAMessage;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

/**
 * The same gateway as {@link EventStreamConsumer}, with one difference that is the whole reason it
 * exists: <b>the caller commits.</b>
 * <p>
 * EventStreamConsumer does Get, Translate, Dispatch, Handle and then commits for you, which is the
 * right shape when the ordering is not what you are studying. In exercise 4 the ordering <i>is</i>
 * what you are studying -- Probe D is "apply the record, then commit the offset, and die in
 * between" -- so the commit has to be a line in the application that you can move.
 * <p>
 * Notice that this is a <i>gateway</i> decision, not an application one. The application still
 * names no Kafka type: it gets a {@link StreamRecord} and it says commit, and the gateway keeps
 * every {@code TopicPartition} and {@code OffsetAndMetadata} to itself.
 */
public final class EventStreamReader<T extends IAmAMessage> implements AutoCloseable {
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(1);

    private final Function<String, T> mapper;
    private final Consumer<String, String> consumer;
    private final String topic;

    /**
     * Kafka hands you a page of the log rather than one record, so a reader that offers records one
     * at a time has to hold the rest of the batch somewhere. That is all this is -- and it is worth
     * noticing, because the records sitting in here are ones the broker already thinks you have.
     */
    private final Deque<ConsumerRecord<String, String>> batch = new ArrayDeque<>();

    public EventStreamReader(Function<String, T> mapper, Class<T> type, String consumerGroup) {
        this(mapper, type, consumerGroup, Stream.BOOTSTRAP_SERVERS);
    }

    public EventStreamReader(Function<String, T> mapper, Class<T> type, String consumerGroup,
                             String bootstrapServers) {
        this.mapper = mapper;
        this.topic = Stream.topicFor(type);

        // Either end may create the topic, so it does not matter which you start first.
        Stream.ensureTopicExists(topic, bootstrapServers);

        Properties config = new Properties();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        // The local copy is built by replaying the whole log, which is the thing a stream can do
        // and a queue cannot. A new consumer with an empty database reads from the start and
        // catches up; that is Archive and Replay from exercise 3, earning its keep.
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // Notice a change to the topic within seconds rather than within the default five minutes.
        config.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, 5000);

        consumer = new KafkaConsumer<>(config);
        consumer.subscribe(List.of(topic));
    }

    public String topic() {
        return topic;
    }

    /** Ask the reader to stop. {@code wakeup()} is the only method safe to call from elsewhere. */
    public void stop() {
        consumer.wakeup();
    }

    /**
     * Get and Translate. Returns null when the poll came back empty, and when we were woken to
     * stop. Both are ordinary and the caller should simply come round again -- a poll that returns
     * nothing is the normal state of a consumer that is caught up.
     *
     * @throws IllegalStateException if the record could not be mapped. There is no invalid-record
     *                               topic on a stream and nothing will move it to one for you,
     *                               which is exercise 3's finding and not this exercise's problem
     *                               to solve -- so this is fatal, loudly, rather than quietly
     *                               skipped.
     */
    public StreamRecord<T> read() {
        if (batch.isEmpty()) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);
                records.forEach(batch::add);
            } catch (WakeupException e) {
                // stop() was called. The caller's own loop decides that this is the ending, which
                // is why we return nothing rather than throwing a Kafka type at the application.
                return null;
            }
        }

        if (batch.isEmpty()) {
            return null;
        }

        ConsumerRecord<String, String> record = batch.poll();
        String where = "p" + record.partition() + "@" + record.offset();
        TopicPartition partition = new TopicPartition(record.topic(), record.partition());

        try {
            return new StreamRecord<>(mapper.apply(record.value()), where, partition, record.offset());
        } catch (Exception e) {
            throw new IllegalStateException("cannot read the record at " + where + ": "
                    + e.getMessage() + ". There is no invalid-record topic on a stream -- see "
                    + "exercise 3.", e);
        }
    }

    /**
     * Move the bookmark. Everything up to and including this record is done -- which is why the
     * offset we send is {@link StreamRecord#next()} and not the record's own.
     */
    public void commit(StreamRecord<T> record) {
        consumer.commitSync(Map.of(record.partition(), new OffsetAndMetadata(record.next())));
    }

    // There is deliberately no seek() here, unlike EventStreamConsumer's retry-in-place. Nothing
    // in exercise 4 retries a record: a price that will not map is fatal, and a price that applies
    // is committed. Winding back would also have to reckon with the records still sitting in the
    // batch above, which is the sort of accounting a gateway should not offer until somebody needs it.

    @Override
    public void close() {
        // Leave the group tidily so the next run does not wait for a session timeout.
        consumer.close();
    }
}
