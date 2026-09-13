package priceconsumer;

import localcopy.SqlitePriceStore;
import model.PriceChanged;
import simpleeventing.EventStreamReader;
import simpleeventing.Stream;
import simpleeventing.StreamRecord;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Follows streams.PriceChanged and maintains the local copy of the catalogue's prices.
 * <pre>
 *   java -jar PriceConsumer/target/PriceConsumer.jar
 *   PRICE_WRITE_WINDOW=15 java -jar PriceConsumer/target/PriceConsumer.jar     # for Probe D
 * </pre>
 * <b>It is a process of its own, and that is deliberate.</b> A thread inside the Receiver would
 * have been less code; it would also have made Probe B "set a flag" instead of "kill -9 a real
 * process", and the whole of exercise 4 is about what happens to the people downstream of a thing
 * that stops.
 */
public final class PriceConsumer {
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance();

    /**
     * How long to pause <i>between</i> the two writes below, so that you can aim a kill at the gap.
     * In a real service the gap is microseconds wide. It is still a gap. This is the same instrument
     * as DUAL_WRITE_WINDOW in exercise 3, one layer down and in your own code.
     */
    private static final Duration WINDOW = windowFromEnvironment();

    private static volatile boolean stopping;

    public static void main(String[] args) throws Exception {
        // Flush every line. Probe B and Probe D both end with this process being killed, and a
        // buffered line you never see is a probe you cannot read.
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true));

        long pid = ProcessHandle.current().pid();
        System.out.println("PriceConsumer starting. PID " + pid);

        try (SqlitePriceStore store = SqlitePriceStore.open();
             EventStreamReader<PriceChanged> reader = new EventStreamReader<>(
                     PriceChanged::deserialize, PriceChanged.class, Stream.PRICE_CONSUMER_GROUP)) {

            System.out.printf("Following %s as group '%s'%n", reader.topic(), Stream.PRICE_CONSUMER_GROUP);

            OffsetDateTime newest = store.newestChangedAt();
            String age = newest == null ? "empty" : "newest change " + ageOf(newest) + " old";
            System.out.printf("Local copy is %s, holding %d prices -- %s.%n",
                    store.path(), store.count(), age);

            if (!WINDOW.isZero()) {
                System.out.printf("PRICE_WRITE_WINDOW is %ds -- there is a gap between the two writes.%n",
                        WINDOW.toSeconds());
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                stopping = true;
                reader.stop();
            }));

            while (!stopping) {
                StreamRecord<PriceChanged> record = reader.read();
                if (record == null) {
                    continue;
                }

                PriceChanged event = record.message();

                // ---------------------------------------------------------------------------
                //  TWO WRITES, TWO STORES, NO TRANSACTION. **PROBE D IS THE ORDER OF THESE LINES.**
                //
                //  The price goes into SQLite. The offset goes into Kafka. Nothing on this
                //  machine can make those two happen together, which is exactly what exercise 3
                //  showed you in the Receiver -- except that this time it is a loop you wrote,
                //  and it looks like one step.
                //
                //  As written: apply, then commit. Die in between and the record is read again
                //  on restart and applied twice, which is harmless *because PriceChanged is a
                //  snapshot*. Swap the two lines and die in between and the price is lost for
                //  ever, because Kafka will never offer it again.
                // ---------------------------------------------------------------------------
                OffsetDateTime appliedAt = store.apply(event);

                Duration staleness = Duration.between(event.changedAt(), appliedAt);
                System.out.printf("  %s = %s  (%s)  published-to-applied %d ms%n",
                        event.sku(), CURRENCY.format(event.price()), record.where(),
                        staleness.toMillis());

                pauseInTheWindow(pid);

                reader.commit(record);
            }
        }

        System.out.println("PriceConsumer stopped.");
    }

    private static void pauseInTheWindow(long pid) throws InterruptedException {
        if (WINDOW.isZero()) {
            return;
        }

        System.out.println("  [one of the two writes has happened and the other has not.]");
        System.out.printf("  [you have %d seconds. kill -9 %d]%n", WINDOW.toSeconds(), pid);
        Thread.sleep(WINDOW.toMillis());
    }

    private static Duration windowFromEnvironment() {
        String seconds = System.getenv("PRICE_WRITE_WINDOW");
        try {
            return Duration.ofSeconds(seconds == null ? 0 : Long.parseLong(seconds));
        } catch (NumberFormatException e) {
            return Duration.ZERO;
        }
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
