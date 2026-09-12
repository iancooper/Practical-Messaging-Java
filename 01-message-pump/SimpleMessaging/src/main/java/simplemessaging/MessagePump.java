package simplemessaging;

import com.rabbitmq.client.GetResponse;

import java.time.Duration;

/**
 * The Message Pump: take a message off a channel, get it to application code, repeat until
 * cancelled.
 * <pre>
 *     Get -&gt; Translate -&gt; Dispatch -&gt; Handle
 * </pre>
 * Each of those four stages fails in its own way, which is why a message that is never going to
 * be handled has four different places it can end up.
 *
 * <hr>
 * <b>THIS PUMP IS THE EXERCISE.</b>
 * <p>
 * It compiles, it runs, and messages flow through it. It is also wrong, in more than one way, and
 * every way it is wrong is something that has shipped to production somewhere.
 * <p>
 * Read it before you run it. Then read PROBE.md. Do not copy this file into anything.
 * <hr>
 */
public final class MessagePump<T extends IAmAMessage> {
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    private final Class<T> type;
    private final IAmAHandler<T> handler;
    private final String hostName;

    private volatile boolean stopping;

    public MessagePump(Class<T> type, IAmAHandler<T> handler) {
        this(type, handler, Channel.HOST_NAME);
    }

    public MessagePump(Class<T> type, IAmAHandler<T> handler, String hostName) {
        this.type = type;
        this.handler = handler;
        this.hostName = hostName;
    }

    /** Ask the pump to stop after the message it is working on. The polite ending. */
    public void stop() {
        stopping = true;
    }

    public void run() throws Exception {
        try (DataTypeChannelConsumer<T> consumer = new DataTypeChannelConsumer<>(type, hostName)) {

            System.out.println("Pump running on " + Channel.queueNameFor(type));

            while (!stopping) {
                // GET
                GetResponse delivery = consumer.receive();

                if (delivery == null) {
                    // Nothing there. Yield, so a Polling Consumer does not spin the CPU.
                    Thread.sleep(POLL_INTERVAL.toMillis());
                    continue;
                }

                System.out.println("Got delivery " + delivery.getEnvelope().getDeliveryTag());

                // We have the message in our hands, so the broker does not need to hold it for us
                // any more. Tell it we are done and let it free the slot.
                consumer.acknowledge(delivery.getEnvelope().getDeliveryTag());

                // TRANSLATE, DISPATCH and HANDLE
                handler.handle(delivery);
            }
        }
    }
}
