package simplemessaging;

import java.time.Duration;

/**
 * The topology, in one place -- and there is more of it than there was in exercise 1.
 * <p>
 * Four queues now, because a message that is never going to be handled has more than one place it
 * can end up, and <i>which</i> place is how you find out what went wrong.
 * <pre>
 *   practical-messaging-streams              (direct)
 *     `-- streams.&lt;T&gt;                  the work
 *            x-dead-letter-exchange:    ...dlx
 *            x-dead-letter-routing-key: retry.streams.&lt;T&gt;
 *
 *   practical-messaging-streams.dlx          (direct)
 *     |-- retry.streams.&lt;T&gt;            work waiting to be tried again
 *     |      x-message-ttl:             5000
 *     |      x-dead-letter-exchange:    practical-messaging-streams
 *     |      x-dead-letter-routing-key: streams.&lt;T&gt;
 *     |
 *     |-- invalid.streams.&lt;T&gt;          a body we could not read
 *     `-- dead.streams.&lt;T&gt;             work we retried and gave up on
 * </pre>
 * <b>The retry queue is the part worth understanding, because RabbitMQ does the work and you do
 * not.</b> Nothing consumes it, and every message in it has five seconds to live. So each message
 * expires -- and an expired message is dead-lettered, and this queue's dead-letter exchange points
 * back at the main exchange. It comes home on a timer nobody wrote.
 * <p>
 * That is <i>Requeue with Delay</i>, built out of a TTL and a dead-letter exchange. Stock
 * RabbitMQ: no plugin, no scheduler, no code.
 * <p>
 * <b>And notice which way round the two hops go.</b> Rejecting a message from the work queue sends
 * it to <i>retry</i>, not to the dead letter queue -- because the round trip work -&gt; retry -&gt;
 * work is a cycle RabbitMQ manages end to end, and a cycle it manages is a cycle it will count for
 * you in the {@code x-death} header. The other two destinations are terminal, so nothing needs
 * counting and we can publish to them directly.
 * <p>
 * The two terminal queues should be empty. When they are not, that is the alert.
 */
public final class Channel {
    private Channel() {
    }

    public static final String HOST_NAME = "localhost";
    public static final String EXCHANGE_NAME = "practical-messaging-streams";
    public static final String DEAD_LETTER_EXCHANGE_NAME = EXCHANGE_NAME + ".dlx";

    /** How long a message waits in the retry queue before it comes back. */
    public static final Duration RETRY_DELAY = Duration.ofSeconds(5);

    public static String routingKeyFor(Class<?> type) {
        return "streams." + type.getName();
    }

    public static String queueNameFor(Class<?> type) {
        return routingKeyFor(type);
    }

    /** Work waiting for its next attempt. Should always be nearly empty. */
    public static String retryQueueNameFor(Class<?> type) {
        return "retry." + queueNameFor(type);
    }

    /** Bodies we could not read. Terminal: nothing here is ever retried. */
    public static String invalidQueueNameFor(Class<?> type) {
        return "invalid." + queueNameFor(type);
    }

    /** Work we retried and gave up on. Terminal: an operator's in-tray. */
    public static String deadLetterQueueNameFor(Class<?> type) {
        return "dead." + queueNameFor(type);
    }
}
