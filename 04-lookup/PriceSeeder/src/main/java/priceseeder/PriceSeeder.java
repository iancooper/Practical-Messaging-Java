package priceseeder;

import model.PriceChanged;
import simpleeventing.EventStreamProducer;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Stands in for the catalogue service: it owns prices, and it tells the world when one changes.
 * <pre>
 *   java -jar PriceSeeder/target/PriceSeeder.jar seed                 a starting price for every SKU
 *   java -jar PriceSeeder/target/PriceSeeder.jar set WIDGET-1 11.99   change one, on demand
 * </pre>
 * It publishes and exits. It keeps no state, because <b>the stream is the state</b> -- which is the
 * whole argument for ECST, and the reason a new price consumer with an empty copy can catch up by
 * reading the log from the beginning.
 */
public final class PriceSeeder {
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance();
    private static final DateTimeFormatter AT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private record Starting(String sku, BigDecimal price) {
    }

    /**
     * The two SKUs that survived exercise 4. GIZMO-SLOW and FLAKY-1 are gone: they were failures of
     * an on-demand lookup, and there is no longer a lookup to fail. See Model/Catalogue.java.
     */
    private static final List<Starting> STARTING = List.of(
            new Starting("WIDGET-1", new BigDecimal("9.99")),
            new Starting("GIZMO-2", new BigDecimal("24.50")));

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true));

        String command = args.length > 0 ? args[0].toLowerCase() : "seed";

        try (EventStreamProducer<PriceChanged> producer = new EventStreamProducer<>(
                PriceChanged::serialize,
                // Keyed by SKU, so two changes to one price stay in order. Key it by the event's
                // own id instead and they land on different partitions, and yesterday's price can
                // be applied on top of today's -- which is a bug you will not see until the day it
                // costs money.
                PriceChanged::sku,
                PriceChanged.class)) {

            switch (command) {
                case "seed" -> {
                    for (Starting starting : STARTING) {
                        publish(producer, PriceChanged.of(starting.sku(), starting.price()));
                    }
                    System.out.println("Seeded " + STARTING.size() + " prices.");
                }

                case "set" -> {
                    if (args.length < 3) {
                        System.err.println("Usage: set <SKU> <PRICE>   e.g. set WIDGET-1 11.99");
                        System.exit(1);
                    }
                    // new BigDecimal(String), never valueOf(double): the string "11.99" is exactly
                    // eleven pounds ninety-nine and the double 11.99 is not.
                    publish(producer, PriceChanged.of(args[1], new BigDecimal(args[2])));
                }

                default -> {
                    System.err.println("Unknown command '" + command + "'. Try: seed, set");
                    System.exit(1);
                }
            }
        }
    }

    private static void publish(EventStreamProducer<PriceChanged> producer, PriceChanged event)
            throws Exception {
        producer.send(event);
        System.out.printf("Published %s = %s at %s%n",
                event.sku(), CURRENCY.format(event.price()), AT.format(event.changedAt()));
    }
}
