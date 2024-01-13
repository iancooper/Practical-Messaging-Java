package simplemessaging;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;


public class DataTypeChannelProducer<T extends IAmAMessage> implements AutoCloseable {
    private final Function<T, String> messageSerializer;
    private final String routingKey;
    private static final String EXCHANGE_NAME = "pipes-practical-messaging";
    private final Connection connection;
    private final Channel channel;

    /**
     * We have split producer and consumer, as they need separate serialization/de-serialization of the message
     * We are disposable so that we can be used within a using statement; connections are unmanaged resources and we
     * want to remember to close them.
     * We are following an RAI pattern here: Resource Acquisition is Initialization
     * @param messageSerializer A method that serializes a type into JSON
     * @param routingKey The topic the queue we are using subscribes to (same name mirrors P2P)
     * @param hostName The name of the host (i.e. localhost)
     */
    public DataTypeChannelProducer(Function<T, String> messageSerializer, String routingKey, String hostName) throws IOException, TimeoutException {
        this.messageSerializer = messageSerializer;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(hostName);
        factory.setAutomaticRecoveryEnabled(true);
        connection = factory.newConnection();
        channel = connection.createChannel();

        this.routingKey = routingKey;

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT, false);

        //With a polling consumer, we don't tend to know who our subscribers are, so we stop creating consumer
        //queues, we keep the exchange as a target, but messages will be dropped if there is no subscribing queue
    }

    /**
     *  Send a message over the channel
     *   Uses the shared routing key to ensure the sender and receiver match up
     * @param message The message we want to send
     * @throws IOException An error publishing the message to RMQ
     */
    public void send(T message) throws IOException {
        byte[] body = messageSerializer.apply(message).getBytes(StandardCharsets.UTF_8);
        channel.basicPublish(EXCHANGE_NAME, routingKey, null, body);
    }

    @Override
    public void close() {
        releaseResources();
    }

    private void releaseResources() {
        try {
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
