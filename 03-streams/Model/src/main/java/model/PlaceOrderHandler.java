package model;

import simplemessaging.IAmAHandler;
import simplemessaging.IPublishEvents;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Application code: price the order, place it, and tell the world it happened.
 * <p>
 * A command came in on a queue; a fact goes out on a stream. That is an entirely ordinary shape and
 * you have probably written it -- which is the point.
 *
 * <hr>
 * Nothing in this class is wrong, and that is what makes exercise 3 worth doing. The handler is
 * clean, the domain has no broker in it, the ordering is the sensible one. Read PROBE.md before you
 * run it, and predict what a crash costs you.
 * <hr>
 */
public class PlaceOrderHandler implements IAmAHandler<PlaceOrder> {
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance();

    /**
     * How long to pause between publishing the event and returning to the pump -- which is to say,
     * between the Kafka write and the RabbitMQ acknowledgement.
     * <p>
     * In a real service that gap is microseconds wide. It is still a gap, and a service that handles
     * a million orders will fall into it. Widening it to fifteen seconds does not create the
     * problem; it just means you can aim at it.
     * <pre>
     *   DUAL_WRITE_WINDOW=15 java -jar Receiver/target/Receiver.jar
     * </pre>
     */
    private static final Duration WINDOW = windowFromEnvironment();

    private final Catalogue catalogue;
    private final IPublishEvents<OrderPlaced> events;

    public PlaceOrderHandler(Catalogue catalogue, IPublishEvents<OrderPlaced> events) {
        this.catalogue = catalogue;
        this.events = events;
    }

    @Override
    public void handle(PlaceOrder order) throws Exception {
        BigDecimal price = catalogue.priceOf(order.sku());
        BigDecimal total = price.multiply(BigDecimal.valueOf(order.quantity()));

        System.out.printf("  placed order %s: %d x %s for %s%n",
                order.id(), order.quantity(), order.sku(), CURRENCY.format(total));

        events.publish(new OrderPlaced(
                UUID.randomUUID().toString(),
                order.id(),
                order.sku(),
                order.quantity(),
                total,
                OffsetDateTime.now()));

        if (!WINDOW.isZero()) {
            System.out.println("  [the event is on the stream. RabbitMQ has NOT been acked yet.]");
            System.out.printf("  [you have %d seconds. kill -9 %d]%n",
                    WINDOW.toSeconds(), ProcessHandle.current().pid());
            Thread.sleep(WINDOW.toMillis());
        }
    }

    private static Duration windowFromEnvironment() {
        String seconds = System.getenv("DUAL_WRITE_WINDOW");
        try {
            return Duration.ofSeconds(seconds == null ? 0 : Long.parseLong(seconds));
        } catch (NumberFormatException e) {
            return Duration.ZERO;
        }
    }
}
