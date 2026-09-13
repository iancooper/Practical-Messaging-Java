package simplemessaging;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * The producer half of the Messaging Gateway: the only class here that knows this is RabbitMQ.
 * <p>
 * Under RMQ, to send, we:
 * <ol>
 *     <li>open a socket connection to the broker</li>
 *     <li>create a channel (a lightweight logical connection) on that socket</li>
 *     <li>declare a direct exchange to publish to</li>
 * </ol>
 * We do not declare the queue. The consumer does that, and binds it to our routing key. That is
 * the asymmetry AMQP has and a queue API does not: we publish to an exchange and have no idea
 * who, if anyone, is listening.
 * <p>
 * We implement {@link AutoCloseable} so we can be used in a try-with-resources block;
 * connections are unmanaged resources and we want to remember to close them.
 * <p>
 * <b>This class is given to you and it is correct.</b> Read it for the AMQP vocabulary; the
 * exercise is not here.
 */
public final class DataTypeChannelProducer<T extends IAmAMessage> implements AutoCloseable {
    private final Function<T, String> serializer;
    private final Connection connection;
    private final com.rabbitmq.client.Channel channel;
    private final String routingKey;

    /**
     * @param serializer  turns a T into the string we put in the body
     * @param type        the type this channel carries, which is what the routing key is derived from
     */
    public DataTypeChannelProducer(Function<T, String> serializer, Class<T> type)
            throws IOException, TimeoutException {
        this(serializer, type, Channel.HOST_NAME);
    }

    public DataTypeChannelProducer(Function<T, String> serializer, Class<T> type, String hostName)
            throws IOException, TimeoutException {
        this.serializer = serializer;
        this.routingKey = Channel.routingKeyFor(type);

        // Defaults: user guest, password guest, port 5672, virtual host /
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(hostName);
        factory.setAutomaticRecoveryEnabled(true);
        connection = factory.newConnection();
        channel = connection.createChannel();

        // Durable, so the exchange survives a broker restart.
        channel.exchangeDeclare(Channel.EXCHANGE_NAME, BuiltinExchangeType.DIRECT, true);
    }

    /**
     * Send a message. The routing key is derived from the type, so sender and receiver match up
     * without either knowing about the other.
     */
    public void send(T message) throws IOException {
        publish(serializer.apply(message));
    }

    /** Send a body we did not serialize -- used to put something unmappable on the channel. */
    public void sendRaw(String body) throws IOException {
        publish(body);
    }

    private void publish(String body) throws IOException {
        // PERSISTENT_TEXT_PLAIN sets deliveryMode 2: the broker writes the message to its message
        // store, so it survives a broker restart. This is the producer-side half of guaranteed
        // delivery, and it is the cheap half.
        AMQP.BasicProperties properties = MessageProperties.PERSISTENT_TEXT_PLAIN;

        channel.basicPublish(
                Channel.EXCHANGE_NAME, routingKey, properties, body.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() {
        try {
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            System.err.println("could not close the connection cleanly: " + e.getMessage());
        }
    }
}
