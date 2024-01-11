import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;


public class DataTypeChannelConsumer<T extends IAmAMessage> implements AutoCloseable {
    private final SerDerOperation<String, T> messageDeserializer;
    private final String queueName;
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
     * We support an invalid message queue, for items that we cannot deserialize into the datatype on the channel correctly.
     * RMQ gets this wrong, and calls this dead-letter when it is in fact invalid message
     * But the principle works, create an exchange for 'invalid' messages and route failed to send to application
     * code messages to it
     * We are disposable so that we can be used within a using statement; connections are unmanaged resources
     * and we want to remember to close them.
     * We are following an RAI pattern here: Resource Acquisition is Initialization
     *
     * @param messageDeserializer A method that deserializes JSON into a type
     * @param routingKey The topic the queue we are using subscribes to (same name mirrors P2P)
     * @param hostName The name of the host (i.e. localhost)
     */
    public DataTypeChannelConsumer(SerDerOperation<String, T> messageDeserializer, String routingKey, String hostName) throws IOException, TimeoutException {
        this.messageDeserializer = messageDeserializer;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(hostName);
        factory.setAutomaticRecoveryEnabled(true);
        connection = factory.newConnection();
        channel = connection.createChannel();

        queueName = routingKey;
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

    /*
     * Receive a message from the queue.
     * The queue should have received all message published because we create it in both the producer and consumer.
     *  We can do this in P2P as we are only expecting one consumer to receive the message.
     */
    public T receive() throws IOException {
        GetResponse result = channel.basicGet(queueName, false);
        if (result != null) {
            try {
                T message = messageDeserializer.apply(new String(result.getBody(), StandardCharsets.UTF_8));
                channel.basicAck(result.getEnvelope().getDeliveryTag(), false);
                return message;
            }
            catch (SerDerException e){
                ///put format errors onto the invalid message queue
                channel.basicReject(result.getEnvelope().getDeliveryTag(), false);
            }
        }

        return null;
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
