package simplemessaging;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * The consumer half of the Messaging Gateway. Again, the only class that knows this is RabbitMQ.
 * <p>
 * Under RMQ, to receive, we:
 * <ol>
 *     <li>open a socket connection to the broker</li>
 *     <li>create a channel on that socket</li>
 *     <li>declare the same direct exchange the producer publishes to</li>
 *     <li>declare a queue to hold our messages</li>
 *     <li>bind the queue to the routing key on that exchange</li>
 * </ol>
 * Both ends declare the exchange, so it does not matter which starts first. Only we declare the
 * queue -- which does mean that anything published before the first run of the consumer went
 * nowhere. Start the consumer once before you send anything.
 * <p>
 * This is a Polling Consumer: {@link #receive()} asks the broker whether there is anything there.
 * It costs us a thread while idle and buys us not having to hold a connection open for the broker
 * to call back on.
 * <p>
 * <b>This class is given to you and it is correct.</b> Read it for the AMQP vocabulary; the
 * exercise is not here.
 */
public final class DataTypeChannelConsumer<T extends IAmAMessage> implements AutoCloseable {
    private final Connection connection;
    private final com.rabbitmq.client.Channel channel;
    private final String queueName;

    public DataTypeChannelConsumer(Class<T> type) throws IOException, TimeoutException {
        this(type, Channel.HOST_NAME);
    }

    public DataTypeChannelConsumer(Class<T> type, String hostName) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(hostName);
        factory.setAutomaticRecoveryEnabled(true);
        connection = factory.newConnection();
        channel = connection.createChannel();

        String routingKey = Channel.routingKeyFor(type);
        queueName = Channel.queueNameFor(type);

        channel.exchangeDeclare(Channel.EXCHANGE_NAME, BuiltinExchangeType.DIRECT, true);

        // Durable queue to go with the persistent messages: no point writing a message to disk
        // and then keeping it in a queue that evaporates on restart.
        channel.queueDeclare(queueName, true, false, false, null);

        channel.queueBind(queueName, Channel.EXCHANGE_NAME, routingKey);
    }

    /**
     * Ask the broker for one message. Null means the queue was empty.
     * <p>
     * autoAck is false, so what comes back is <i>locked to us</i> and not yet removed from the
     * queue. The broker is now waiting to be told what happened. Until we tell it, this message
     * shows in the management console as "unacked".
     */
    public GetResponse receive() throws IOException {
        return channel.basicGet(queueName, false);
    }

    /** I am done with this message. The broker may forget it. */
    public void acknowledge(long deliveryTag) throws IOException {
        channel.basicAck(deliveryTag, false);
    }

    /**
     * I am not done with this message.
     * <p>
     * requeue true  -- put it back, someone will try again (possibly us, immediately).
     * <br>
     * requeue false -- reject it. On a plain queue that deletes it.
     */
    public void reject(long deliveryTag, boolean requeue) throws IOException {
        channel.basicNack(deliveryTag, false, requeue);
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
