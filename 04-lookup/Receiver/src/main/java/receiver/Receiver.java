package receiver;

import localcopy.SqlitePriceStore;
import model.Catalogue;
import model.OrderPlaced;
import model.PlaceOrder;
import model.PlaceOrderHandler;
import model.PlaceOrderMapper;
import simpleeventing.EventStreamProducer;
import simpleeventing.KafkaEventPublisher;
import simplemessaging.MessagePump;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * A command in on a queue, a fact out on a stream -- and now the price comes from a local copy of
 * somebody else's data rather than from a map.
 * <pre>
 *   java -jar Receiver/target/Receiver.jar
 *   DUAL_WRITE_WINDOW=15 java -jar Receiver/target/Receiver.jar     # exercise 3's Probe A, still here
 * </pre>
 * <b>This is the only file that knows all three things at once</b>: that prices live in SQLite,
 * that the domain wants an {@code IPriceStore}, and that the two fit together. Composition is the
 * application's job. Model declares the interface and names none of the rest of it.
 */
public final class Receiver {
    /** Set when the pump has returned or thrown, so the shutdown hook knows which ending this is. */
    private static volatile boolean finished;

    public static void main(String[] args) throws Exception {
        // Flush every line. Several probes end with this process being killed, and a buffered line
        // you never see is a probe you cannot read.
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true));

        long pid = ProcessHandle.current().pid();
        System.out.println("Receiver starting. PID " + pid);
        System.out.println("Ctrl-C to stop between messages; 'kill -9 " + pid + "' to stop mid-message.");
        System.out.println();

        try (SqlitePriceStore prices = SqlitePriceStore.open();
             KafkaEventPublisher<OrderPlaced> publisher = new KafkaEventPublisher<>(
                     new EventStreamProducer<>(
                             OrderPlaced::serialize,
                             OrderPlaced::orderId,
                             OrderPlaced.class))) {

            // Say how old the copy is, once, at startup. It is the only line in the system that
            // knows -- and watch what Probe C makes of that. Knowing at startup is not the same as
            // noticing, and a receiver that prices ten thousand orders from a four-day-old copy
            // will say this once.
            OffsetDateTime newest = prices.newestChangedAt();
            String age = newest == null ? "empty" : "newest change " + ageOf(newest) + " old";
            System.out.printf("Local copy is %s, holding %d prices -- %s.%n",
                    prices.path(), prices.count(), age);

            MessagePump<PlaceOrder> pump = new MessagePump<>(
                    PlaceOrder.class,
                    new PlaceOrderMapper(),
                    // PlaceOrderHandler has not changed since exercise 3, and nothing in this line
                    // asks it to. The Catalogue is handed a store instead of holding a map, and the
                    // handler's call site is the same call site.
                    new PlaceOrderHandler(new Catalogue(prices), publisher));

            // Ctrl-C asks the pump to stop, and then we wait for it: tearing the process down while
            // a message is in flight is the rude ending, and that is what kill -9 is for.
            //
            // A shutdown hook runs on *every* exit, including the pump throwing its way out of the
            // process, so it has to know which ending this is or it reports the polite one either way.
            Thread main = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (finished) {
                    return;
                }
                System.out.println("\nStopping after the current message...");
                pump.stop();
                try {
                    main.join(5000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }));

            try {
                pump.run();
            } finally {
                finished = true;
            }
        }

        System.out.println("Receiver stopped.");
    }

    /**
     * How old, in words, without saying "1 minutes". A copy's age is the one number that tells a
     * current local copy from a stale one, so it is worth printing in a shape a human reads.
     */
    private static String ageOf(OffsetDateTime when) {
        Duration d = Duration.between(when, OffsetDateTime.now());
        if (d.toMinutes() < 1) return d.toSeconds() + "s";
        if (d.toHours() < 1) return d.toMinutes() + "m";
        if (d.toDays() < 1) return d.toHours() + "h";
        return d.toDays() + "d";
    }
}
