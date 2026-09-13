package simplemessaging;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.LongString;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * The consumer half of the Messaging Gateway. It declares the topology described in
 * {@link Channel} and hands the pump five things it can do with a message.
 * <p>
 * <b>The plumbing is given to you and it is correct.</b> Declaring exchanges and binding queues is
 * AMQP vocabulary, not judgement, and you can read it here at your leisure. The exercise is
 * deciding <i>which of these five to call, and when</i> -- and that lives in the pump.
 */
public final class DataTypeChannelConsumer<T extends IAmAMessage> implements AutoCloseable {
    private final Connection connection;
    private final com.rabbitmq.client.Channel channel;
    private final String queueName;
    private final String retryQueueName;
    private final String invalidQueueName;
    private final String deadLetterQueueName;

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
        retryQueueName = Channel.retryQueueNameFor(type);
        invalidQueueName = Channel.invalidQueueNameFor(type);
        deadLetterQueueName = Channel.deadLetterQueueNameFor(type);

        channel.exchangeDeclare(Channel.EXCHANGE_NAME, BuiltinExchangeType.DIRECT, true);
        channel.exchangeDeclare(Channel.DEAD_LETTER_EXCHANGE_NAME, BuiltinExchangeType.DIRECT, true);

        // The work queue. Rejecting a message from here (nack, requeue false) sends it to the
        // dead-letter exchange with the *retry* routing key -- so a rejection lands in the retry
        // queue without us publishing anything.
        //
        // Note what this means: the queue's dead-letter routing key is fixed at declare time.
        // One reject, one destination. Anything else you want to do with a message, you do by
        // publishing it somewhere yourself.
        Map<String, Object> workArguments = new HashMap<>();
        workArguments.put("x-dead-letter-exchange", Channel.DEAD_LETTER_EXCHANGE_NAME);
        workArguments.put("x-dead-letter-routing-key", retryQueueName);
        channel.queueDeclare(queueName, true, false, false, workArguments);
        channel.queueBind(queueName, Channel.EXCHANGE_NAME, routingKey);

        // Bodies we could not read. A terminal destination: no TTL, no dead-letter exchange.
        channel.queueDeclare(invalidQueueName, true, false, false, null);
        channel.queueBind(invalidQueueName, Channel.DEAD_LETTER_EXCHANGE_NAME, invalidQueueName);

        // Work we gave up on. Also terminal. This is the one an operator looks in.
        channel.queueDeclare(deadLetterQueueName, true, false, false, null);
        channel.queueBind(deadLetterQueueName, Channel.DEAD_LETTER_EXCHANGE_NAME, deadLetterQueueName);

        // The retry queue: a waiting room with a clock on the door.
        // Nothing consumes it. Every message in it expires after RETRY_DELAY, and an expired
        // message is dead-lettered -- back to the main exchange, and so back to the work queue.
        // RabbitMQ stamps an x-death header on the way through, which is how we count attempts.
        Map<String, Object> retryArguments = new HashMap<>();
        retryArguments.put("x-message-ttl", (int) Channel.RETRY_DELAY.toMillis());
        retryArguments.put("x-dead-letter-exchange", Channel.EXCHANGE_NAME);
        retryArguments.put("x-dead-letter-routing-key", routingKey);
        channel.queueDeclare(retryQueueName, true, false, false, retryArguments);
        channel.queueBind(retryQueueName, Channel.DEAD_LETTER_EXCHANGE_NAME, retryQueueName);
    }

    /** Ask the broker for one message. Null means the queue was empty. */
    public GetResponse receive() throws IOException {
        return channel.basicGet(queueName, false);
    }

    /** Done. The broker may forget it. */
    public void acknowledge(long deliveryTag) throws IOException {
        channel.basicAck(deliveryTag, false);
    }

    /**
     * Put it back on the queue, right now, for someone to try again immediately.
     * There is no limit on this and no delay. Think about what that means before you use it.
     */
    public void requeue(long deliveryTag) throws IOException {
        channel.basicNack(deliveryTag, false, true);
    }

    /**
     * Reject it. Because of the work queue's arguments, the broker routes it to the
     * <b>retry queue</b>, where it waits and then comes back on its own. One call, and RabbitMQ
     * does the moving -- and because RabbitMQ owns both hops, RabbitMQ counts them for you.
     */
    public void rejectForRetry(long deliveryTag) throws IOException {
        channel.basicNack(deliveryTag, false, false);
    }

    /**
     * Publish it to the invalid message queue. Terminal: a body nobody can read.
     * <p>
     * This is a publish, not a reject -- so the original delivery is still outstanding and it is
     * still your problem. Headers are carried forward, because x-death is the attempt count and
     * losing it resets the clock.
     */
    public void sendToInvalidMessageQueue(GetResponse delivery) throws IOException {
        republish(delivery, invalidQueueName);
    }

    /** Send it to the dead letter queue. Terminal: somebody has to come and look. */
    public void sendToDeadLetter(GetResponse delivery) throws IOException {
        republish(delivery, deadLetterQueueName);
    }

    private void republish(GetResponse delivery, String routingKey) throws IOException {
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .deliveryMode(2)                                        // persistent
                .headers(delivery.getProps().getHeaders())
                .build();

        channel.basicPublish(
                Channel.DEAD_LETTER_EXCHANGE_NAME, routingKey, properties, delivery.getBody());
    }

    /**
     * How many times has this message been round the retry loop?
     * <p>
     * RabbitMQ records every dead-lettering in an {@code x-death} header: an array of entries, one
     * per (queue, reason) pair, each with a count. A message that has expired out of the retry
     * queue twice has an entry for that queue with count 2. A message arriving for the first time
     * has no x-death header at all, so it has had no attempts yet.
     * <p>
     * Look at this header in the management console. It is the most useful thing RabbitMQ will tell
     * you about a message's history and almost nobody knows it is there.
     */
    public int retriesSoFar(GetResponse delivery) {
        Map<String, Object> headers = delivery.getProps().getHeaders();
        if (headers == null || !(headers.get("x-death") instanceof List<?> deaths)) {
            return 0;
        }

        for (Object death : deaths) {
            if (!(death instanceof Map<?, ?> entry)) {
                continue;
            }
            if (!retryQueueName.equals(text(entry.get("queue")))) {
                continue;
            }
            if (entry.get("count") instanceof Number count) {
                return count.intValue();
            }
        }

        return 0;
    }

    /**
     * AMQP's wire format has no plain string, so the client hands header values back as
     * {@link LongString} or bytes depending on length. Worth knowing the first time a header
     * comparison fails for no visible reason.
     */
    private static String text(Object value) {
        return switch (value) {
            case null -> null;
            case LongString longString -> longString.toString();
            case byte[] bytes -> new String(bytes, StandardCharsets.UTF_8);
            default -> value.toString();
        };
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
