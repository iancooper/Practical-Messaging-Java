package simpleeventing;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.function.Function;

public class DataTypeChannelProducer<T extends  IAmAMessage> implements AutoCloseable {
    private final Function<T, String> _messageSerializer;
    private final Producer<String, String> _producer;
    private final String _topic;

    public DataTypeChannelProducer(Function<T, String> messageSerializer, String hostName) {
        _messageSerializer = messageSerializer;

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        _producer = new KafkaProducer<>(producerProps);
        _topic = "Pub-Sub-Stream-Biography";
    }

    public void send(T message) {
        String body = _messageSerializer.apply(message);
        _producer.send(new ProducerRecord<>(_topic, message.getId(), body));
    }

    public void flush() {
        _producer.flush();
    }

    @Override
    public void close() {
        _producer.close();
    }
}
