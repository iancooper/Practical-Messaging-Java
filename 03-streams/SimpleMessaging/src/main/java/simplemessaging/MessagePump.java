package simplemessaging;

import com.rabbitmq.client.GetResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * The Message Pump: Get -&gt; Translate -&gt; Dispatch -&gt; Handle, in a loop, until cancelled.
 *
 * <hr>
 * <b>THIS IS EXERCISE 2's ANSWER, AND IT IS CORRECT. Nothing here needs fixing.</b>
 * <ul>
 *     <li>Translate goes through a Message Mapper; the handler takes a domain type</li>
 *     <li>the acknowledgement happens after the work, not before it</li>
 *     <li>a body we cannot read goes to the invalid message queue, and is never retried</li>
 *     <li>work that failed is retried with a delay, up to a limit, then dead-lettered</li>
 *     <li>the retry count comes from RabbitMQ's x-death header; we count nothing ourselves</li>
 * </ul>
 * Every one of those five is something the broker does for you. Exercise 3 is about what happens to
 * this list when the channel is a stream instead of a queue.
 * <p>
 * There is one line in here that is now a problem, and it is not a problem with the pump.
 * Read PROBE.md.
 * <hr>
 */
public final class MessagePump<T extends IAmAMessage> {
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    /** How many times we retry before giving up. This is n. */
    private static final int MAX_RETRIES = 3;

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

                long deliveryTag = delivery.getEnvelope().getDeliveryTag();

                try {
                    // TRANSLATE
                    String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    T message = mapper.mapToRequest(body);

                    // DISPATCH and HANDLE
                    handler.handle(message);

                    // Only now are we done with it.
                    consumer.acknowledge(deliveryTag);
                } catch (UnmappableMessageException e) {
                    // A failure to UNDERSTAND. The bytes are not going to change, so there is
                    // nothing to retry -- retrying this is the definition of a poison pill.
                    // Publish it to the invalid message queue, where someone can go and look at
                    // it. A reject would send it to the *retry* queue, which is the one thing this
                    // message must never go to.
                    System.out.println("  INVALID: " + e.getMessage());
                    System.out.println("  -> " + Channel.invalidQueueNameFor(type));
                    consumer.sendToInvalidMessageQueue(delivery);
                    consumer.acknowledge(deliveryTag);
                } catch (Exception e) {
                    // A failure to PROCESS. The message was perfectly readable; the work failed.
                    // That may have been bad luck, so it is worth trying again -- but not forever.
                    int retries = consumer.retriesSoFar(delivery);

                    if (retries < MAX_RETRIES) {
                        System.out.printf("  FAILED on attempt %d: %s%n", retries + 1, e.getMessage());
                        System.out.printf("  -> retrying in %ds%n", Channel.RETRY_DELAY.toSeconds());
                        consumer.rejectForRetry(deliveryTag);
                    } else {
                        System.out.printf("  GIVING UP after %d attempts: %s%n", retries + 1, e.getMessage());
                        System.out.println("  -> " + Channel.deadLetterQueueNameFor(type));
                        consumer.sendToDeadLetter(delivery);
                        consumer.acknowledge(deliveryTag);
                    }
                }
            }
        }
    }
}
