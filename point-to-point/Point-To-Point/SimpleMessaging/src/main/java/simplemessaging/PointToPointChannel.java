package simplemessaging;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class PointToPointChannel implements AutoCloseable {
    private final String routingKey;
    private final String queueName;
    private static final String EXCHANGE_NAME = "practical-messaging";
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
     * We are disposable so that we can be used within a using statement; connections
     * are unmanaged resources and we want to remember to close them.
     * We are following an RAI pattern here: Resource Acquisition is Initialization
     * @param queueName The name of the queue we are using for a p2p channel
     * @param hostName The name of the host (i.e. localhost)
     */
    public PointToPointChannel(String queueName, String hostName) throws IOException, TimeoutException {
        //just use defaults: usr: guest pwd: guest port:5672 virtual host: /
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(hostName);
        factory.setAutomaticRecoveryEnabled(true);
        connection = factory.newConnection();
        channel = connection.createChannel();

        // Because we are point to point, we are just going to use queueName for the routing key
        routingKey = queueName;
        this.queueName = queueName;

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT, false);
        channel.queueDeclare(this.queueName, false, false, false, null);
        channel.queueBind(this.queueName, EXCHANGE_NAME, routingKey);
    }

    /**
     * Send a message over the channel.
     * Uses the shared routing key to ensure the sender and receiver match up
     *  Note that we set queue name to routing key so this can only have one consumer i.e. point-to-point
     * @param message The message to send
     */
    public void send(String message) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);
        channel.basicPublish(EXCHANGE_NAME, routingKey, null, body);
    }

    /**
     * Receive a message from the queue.
     * The queue should have received all message published because we create it in the constructor, so the
     * producer will create as well as the consumer making the ordering unimportant
     */
    public String receive() throws IOException {
        GetResponse result = channel.basicGet(queueName, true);
        if (result != null) {
            return new String(result.getBody(), StandardCharsets.UTF_8);
        } else {
            return null;
        }
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