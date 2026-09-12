package sender;

import model.PlaceOrder;
import simplemessaging.DataTypeChannelProducer;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * The producer. It puts things on the channel for you, including things the receiver will not
 * like. Every probe in PROBE.md starts with one of these.
 * <pre>
 *   java -jar Sender/target/Sender.jar                  one good order
 *   java -jar Sender/target/Sender.jar slow             an order whose lookup takes 30 seconds
 *   java -jar Sender/target/Sender.jar poison           an order for a SKU that is not in the catalogue
 *   java -jar Sender/target/Sender.jar unmappable       a body that is not a PlaceOrder at all
 *   java -jar Sender/target/Sender.jar burst 20         twenty good orders, as fast as we can publish them
 * </pre>
 */
public final class Sender {
    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true));

        String command = args.length > 0 ? args[0].toLowerCase() : "good";

        try (DataTypeChannelProducer<PlaceOrder> producer =
                     new DataTypeChannelProducer<>(PlaceOrder::serialize, PlaceOrder.class)) {

            switch (command) {
                case "good" -> publish(producer, PlaceOrder.forSku("WIDGET-1"));

                case "slow" -> publish(producer, PlaceOrder.forSku("GIZMO-SLOW"));

                // Well-formed. Maps perfectly. The handler will throw on it every single time.
                case "poison" -> publish(producer, PlaceOrder.forSku("NOPE-404"));

                case "unmappable" -> {
                    // Valid JSON, wrong shape. The mapper cannot turn this into a PlaceOrder, and
                    // no amount of retrying will change that.
                    String body = "{\"Id\":\"c0ffee\",\"ProductCode\":\"WIDGET-1\",\"Qty\":1}";
                    producer.sendRaw(body);
                    System.out.println("Sent an unmappable body: " + body);
                }

                case "burst" -> {
                    int count = args.length > 1 ? Integer.parseInt(args[1]) : 20;
                    for (int i = 0; i < count; i++) {
                        publish(producer, PlaceOrder.forSku("WIDGET-1", i + 1, "CUST-001"));
                    }
                    System.out.println("Sent " + count + " orders");
                }

                default -> {
                    System.err.println("Unknown command '" + command
                            + "'. Try: good, slow, poison, unmappable, burst");
                    System.exit(1);
                }
            }
        }
    }

    private static void publish(DataTypeChannelProducer<PlaceOrder> producer, PlaceOrder order)
            throws Exception {
        producer.send(order);
        System.out.printf("Sent order %s: %d x %s%n", order.id(), order.quantity(), order.sku());
    }
}
