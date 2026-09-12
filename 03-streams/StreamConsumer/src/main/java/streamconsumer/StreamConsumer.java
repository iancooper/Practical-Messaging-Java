package streamconsumer;

import model.OrderPlaced;
import simpleeventing.EventStreamConsumer;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Follows the OrderPlaced stream and counts what it sees.
 * <pre>
 *   java -jar StreamConsumer/target/StreamConsumer.jar
 * </pre>
 * The count is the point. An order placed once should appear once.
 */
public final class StreamConsumer {
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance();

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true));

        System.out.println("StreamConsumer starting. PID " + ProcessHandle.current().pid());

        // How many times have we seen an event for each order? Anything above one is a duplicate,
        // and duplicates are what exercise 3 is about.
        Map<String, Integer> seen = new HashMap<>();

        EventStreamConsumer<OrderPlaced> consumer = new EventStreamConsumer<>(
                OrderPlaced::deserialize,
                event -> {
                    int count = seen.merge(event.orderId(), 1, Integer::sum);
                    String flag = count > 1 ? "  <-- DUPLICATE, seen " + count + " times" : "";

                    System.out.printf("  order %s: %d x %s for %s%s%n",
                            event.orderId(), event.quantity(), event.sku(),
                            CURRENCY.format(event.total()), flag);
                },
                OrderPlaced.class);

        Runtime.getRuntime().addShutdownHook(new Thread(consumer::stop));

        consumer.run();

        System.out.println();
        int events = seen.values().stream().mapToInt(Integer::intValue).sum();
        System.out.printf("Distinct orders seen: %d. Events read: %d.%n", seen.size(), events);
        seen.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .forEach(e -> System.out.printf("  %s arrived %d times%n", e.getKey(), e.getValue()));
    }
}
