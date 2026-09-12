package simpleeventing;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import simplemessaging.IAmAMessage;

import java.time.Duration;
import java.util.Properties;
import java.util.function.Function;

/**
 * Appends records to a Kafka topic. The producer half of the eventing gateway.
 * <p>
 * Note what is <i>not</i> here, compared with the RabbitMQ producer: no exchange, no binding, no
 * routing key. You append to a log, and the key decides which partition the record lands in. Same
 * key, same partition, so records that must stay in order must share a key.
 */
public final class EventStreamProducer<T extends IAmAMessage> implements AutoCloseable {
    private final Function<T, String> serializer;
    private final Function<T, String> partitionKey;
    private final Producer<String, String> producer;
    private final String topic;

    /**
     * @param serializer   turns a T into the record's value
     * @param partitionKey which partition this record belongs in -- and therefore what it is ordered
     *                     with respect to. Records sharing a key share a partition and stay in
     *                     order; records with different keys have no order between them at all.
     *                     <p>
     *                     <b>This is a design decision and there is no safe default</b>, which is
     *                     why you have to pass it. Key an order's events by the order and they
     *                     arrive in sequence. Key them by the event's own id and every event is
     *                     independent -- which is fine right up until two events about the same
     *                     thing are processed out of order by different consumers.
     * @param type         the type this stream carries, which is what the topic name comes from
     */
    public EventStreamProducer(Function<T, String> serializer, Function<T, String> partitionKey, Class<T> type) {
        this(serializer, partitionKey, type, Stream.BOOTSTRAP_SERVERS);
    }

    public EventStreamProducer(Function<T, String> serializer, Function<T, String> partitionKey,
                               Class<T> type, String bootstrapServers) {
        this.serializer = serializer;
        this.partitionKey = partitionKey;
        this.topic = Stream.topicFor(type);

        Stream.ensureTopicExists(topic, bootstrapServers);

        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // Wait for the leader and all in-sync replicas before calling a write done. This is the
        // producer-side half of guaranteed delivery, and it is the cheap half -- exactly as it was
        // on RabbitMQ, where it was one 'persistent' flag.
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        producer = new KafkaProducer<>(config);
    }

    /**
     * Append a record, and wait until the broker has acknowledged it.
     * <p>
     * We block on the returned future rather than firing and forgetting: fire-and-forget would
     * return before the record was durable, and then "I produced the event" would be a claim about a
     * buffer in this process rather than about anything the broker has.
     */
    public void send(T message) throws Exception {
        append(partitionKey.apply(message), serializer.apply(message));
    }

    /** Append a record we did not serialize -- used to put something unreadable on the stream. */
    public void sendRaw(String key, String body) throws Exception {
        append(key, body);
    }

    private void append(String key, String value) throws Exception {
        RecordMetadata result = producer.send(new ProducerRecord<>(topic, key, value)).get();
        System.out.printf("  -> %s partition %d offset %d%n",
                result.topic(), result.partition(), result.offset());
    }

    @Override
    public void close() {
        producer.flush();
        producer.close(Duration.ofSeconds(5));
    }
}
