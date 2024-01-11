import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;


public class DataTypeChannelProducer<T extends IAmAMessage> implements AutoCloseable {
    private final SerDerOperation<T, String> messageSerializer;
    private final String routingKey;
    private static final String exchangeName = "practical-messaging";
    private static final String invalidExchangeName = "practical-messaging-invalid";
    private final Connection connection;
    private final Channel channel;

    /**
     * Create a new channel for sending point-to-point messages.
     * Under RMQ we:
     *     1. Create a socket connection to the broker
     *     2. Create a channel on that socket
     *     3. Create a direct exchange on the server for point-to-point messaging
     *     4. Create a queue to hold messages
     *     5. Bind the queue to listen to a routing key on that exchange
     * We have split producer and consumer, as they need separate serialization/de-serialization of the message
     * We are disposable so that we can be used within a using statement; connections are unmanaged resources and we
     * want to remember to close them.
     * We are following an RAI pattern here: Resource Acquisition is Initialization
     * @param messageSerializer A method that serializes a type into JSON
     * @param routingKey The topic the queue we are using subscribes to (same name mirrors P2P)
     * @param hostName The name of the host (i.e. localhost)
     */
    public DataTypeChannelProducer(SerDerOperation<T, String> messageSerializer, String routingKey, String hostName) throws IOException, TimeoutException {
        this.messageSerializer = messageSerializer;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(hostName);
        factory.setAutomaticRecoveryEnabled(true);
        connection = factory.newConnection();
        channel = connection.createChannel();

        this.routingKey = routingKey;
        String queueName = routingKey;

        var invalidRoutingKey = "invalid" + routingKey;
        var invalidMessageQueueName = invalidRoutingKey;

        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT, false);

        var arguments = new HashMap<String, Object>();
        arguments.put("x-dead-letter-exchange", invalidExchangeName);
        arguments.put("x-dead-letter-routing-key", invalidRoutingKey);

        channel.queueDeclare(queueName, false, false, false, arguments);
        channel.queueBind(queueName, exchangeName, routingKey);

        channel.exchangeDeclare(invalidExchangeName, BuiltinExchangeType.DIRECT, false);
        channel.queueDeclare(invalidMessageQueueName, false, false, false, null);
        channel.queueBind(invalidMessageQueueName, invalidExchangeName, invalidRoutingKey);
    }

    /**
     *  Send a message over the channel
     *   Uses the shared routing key to ensure the sender and receiver match up
     * @param message The message we want to send
     * @throws IOException An error publishing the message to RMQ
     */
    public void send(T message) throws IOException, SerDerException {
        byte[] body = messageSerializer.apply(message).getBytes(StandardCharsets.UTF_8);
        channel.basicPublish(exchangeName, routingKey, null, body);
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
