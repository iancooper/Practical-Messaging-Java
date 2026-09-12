package simplemessaging;

import com.rabbitmq.client.GetResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * The Message Pump: Get -&gt; Translate -&gt; Dispatch -&gt; Handle, in a loop, until cancelled.
 *
 * <hr>
 * <b>THIS IS EXERCISE 1's ANSWER, AND EXERCISE 2's PROBLEM.</b>
 * <p>
 * Everything exercise 1 asked for is here and correct:
 * <ul>
 *     <li>the Translate stage is back, and it goes through a Message Mapper</li>
 *     <li>the handler takes a domain type; nothing in Model/ knows a broker exists</li>
 *     <li>the acknowledgement happens after the work, not before it</li>
 *     <li>a failure no longer kills the loop</li>
 * </ul>
 * So it does not lose messages any more, and it does not fall over. It is still wrong, and the way
 * it is wrong is worse than falling over, because it will not show up in your logs as a crash.
 * Read PROBE.md.
 * <hr>
 */
public final class MessagePump<T extends IAmAMessage> {
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    private final Class<T> type;
    private final IAmAMessageMapper<T> mapper;
    private final IAmAHandler<T> handler;
    private final String hostName;

    private volatile boolean stopping;

    public MessagePump(Class<T> type, IAmAMessageMapper<T> mapper, IAmAHandler<T> handler) {
        this(type, mapper, handler, Channel.HOST_NAME);
    }

    public MessagePump(Class<T> type, IAmAMessageMapper<T> mapper, IAmAHandler<T> handler, String hostName) {
        this.type = type;
        this.mapper = mapper;
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
                    Thread.sleep(POLL_INTERVAL.toMillis());
                    continue;
                }

                try {
                    // TRANSLATE
                    String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    T message = mapper.mapToRequest(body);

                    // DISPATCH and HANDLE
                    handler.handle(message);

                    // Only now are we done with it.
                    consumer.acknowledge(delivery.getEnvelope().getDeliveryTag());
                } catch (Exception e) {
                    // Something went wrong, and we must not lose the message. Put it back on the
                    // queue so it gets tried again.
                    System.out.println("  FAILED: " + e.getMessage() + " -- putting it back");
                    consumer.requeue(delivery.getEnvelope().getDeliveryTag());
                }
            }
        }
    }
}
