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

    public DataTypeChannelProducer(Function<T, String> messageSerializer) {
         this(messageSerializer, "localhost:9092");
    }
    public DataTypeChannelProducer(Function<T, String> messageSerializer, String bootStrapServer) {
        _messageSerializer = messageSerializer;

        //TODO: Create a ProducerConfig file to configure Kafka. You will need to set:
        // BootstrapServers
        // Key and Value String Serializers

        //Create the Kafka Producer
        _topic = "Pub-Sub-Stream-Biography";
    }

    public void send(T message) {
        String body = _messageSerializer.apply(message);
        //TODO: Send the message to Kafka
    }

    public void flush() {
        //TODO: Flush the producer
    }

    @Override
    public void close() {
        //TODO: Close the producer
    }
}
