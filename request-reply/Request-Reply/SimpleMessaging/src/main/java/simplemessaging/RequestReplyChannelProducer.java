package simplemessaging;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;


public class RequestReplyChannelProducer<T extends IAmAMessage, TResponse extends IAmAResponse> implements AutoCloseable {
    private final Function<T, String> messageSerializer;
    private final Function<String, TResponse> messageDeserializer;
    private final String routingKey;
    private static final String EXCHANGE_NAME = "practical-messaging-request-reply";
    private static final String INVALID_EXCHANGE_NAME = "practical-messaging-invalid";
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
     * @param messageDeserializer A method that deserializes a type from JSON
     * @param routingKey The topic the queue we are using subscribes to (same name mirrors P2P)
     * @param hostName The name of the host (i.e. localhost)
     */
    public RequestReplyChannelProducer(
            Function<T, String> messageSerializer,
            Function<String, TResponse> messageDeserializer,
            String routingKey,
            String hostName) throws IOException, TimeoutException {
        this.messageSerializer = messageSerializer;
        this.messageDeserializer = messageDeserializer;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(hostName);
        factory.setAutomaticRecoveryEnabled(true);
        connection = factory.newConnection();
        channel = connection.createChannel();

        this.routingKey = routingKey;
        String queueName = routingKey;

        var invalidRoutingKey = "invalid" + routingKey;
        var invalidMessageQueueName = invalidRoutingKey;

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT, false);

        var arguments = new HashMap<String, Object>();
        arguments.put("x-dead-letter-exchange", INVALID_EXCHANGE_NAME);
        arguments.put("x-dead-letter-routing-key", invalidRoutingKey);

        channel.queueDeclare(queueName, false, false, false, arguments);
        channel.queueBind(queueName, EXCHANGE_NAME, routingKey);

        channel.exchangeDeclare(INVALID_EXCHANGE_NAME, BuiltinExchangeType.DIRECT, false);
        channel.queueDeclare(invalidMessageQueueName, false, false, false, null);
        channel.queueBind(invalidMessageQueueName, INVALID_EXCHANGE_NAME, invalidRoutingKey);
    }

    /**
     *  Call another process and wait for the response. This blocks, as it has function call semantics
     *  We make two choices: (a) a queue per call. This has overhead but makes correlation of message
     *  between call and response trivial; (b) a queue per client, we would need to correlate responses to
     *  ensure we handled out-of-order messages (might be enough to drop ones we don't recognize). We block
     *  awaiting the response as that is an RPC semantic, over allowing a separate consumer to receive responses
     *  and handle them via a handler. That alternative uses routing keys over queues to work and is less true RPC
     *  than request-reply
     * @param message The message to send
     * @param timeoutInMilliseconds How long to wait for a response§
     */
    public TResponse call(T message, int timeoutInMilliseconds) throws IOException, InterruptedException {

        //declare a queue for replies, we can auto-delete this as it should die with us
        //auto-generate a queue name; we don't need a routing key as we just send/receive from this queue
        //Note that we do not need bind to the default exchange; any queue declared on the default exchange
        //automatically has a routing key that is the queue name. Because we choose a random
        //queue name this means we avoid any collisions
        // TODO: Declare a queue for replies, non-durable, exclusive, auto-deleting. no queue name
        // TODO: Assign auto generated queuename to variable for later use

        // TODO: serialize the body, and turn it into a byte[] with URF8 encoding
        //In order to do guaranteed delivery, we want to use the broker's message store to hold the message,
        //so that it will be available even if the broker restarts
        // TODO: Create basic properties for the channel
        // TODO: Make the message persistent
        // TODO: Set reply to on the props to the random queue name from above
        // TODO: Publish to the consumer on ExchangeName with _routingKey and props and body

        //now we want to listen
        /*
         * TODO
         * whilst a time the timeout period is not up
         *     read from the reply queue
         *     if we have a message
         *         serialize the message (hint: convert UTF8 byte array to string
         *         ack the message
         *         break
         *     else
         *         yield briefy, but not too long as we have a timeout
         * delete the reply queue when done
         * return the response
         */
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
