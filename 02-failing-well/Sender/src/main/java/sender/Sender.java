package sender;

import model.PlaceOrder;
import simplemessaging.DataTypeChannelProducer;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * The producer, and the source of every failure you are asked to survive.
 * <pre>
 *   java -jar Sender/target/Sender.jar                  a good order
 *   java -jar Sender/target/Sender.jar flaky            TRANSIENT: the lookup fails twice, then works
 *   java -jar Sender/target/Sender.jar poison           PERMANENT: a SKU that is not in the catalogue, ever
 *   java -jar Sender/target/Sender.jar unmappable       INVALID:   a body that is not a PlaceOrder at all
 *   java -jar Sender/target/Sender.jar slow             an order whose lookup takes 30 seconds
 *   java -jar Sender/target/Sender.jar burst 20         twenty good orders
 * </pre>
 * Three of those are three different failures. They should not all end up in the same place.
 */
public final class Sender {
    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true));

        String command = args.length > 0 ? args[0].toLowerCase() : "good";

        try (DataTypeChannelProducer<PlaceOrder> producer =
                     new DataTypeChannelProducer<>(PlaceOrder::serialize, PlaceOrder.class)) {

            switch (command) {
                case "good" -> publish(producer, PlaceOrder.forSku("WIDGET-1"));

                // The order is fine. The catalogue is having a bad minute and will recover.
                case "flaky" -> publish(producer, PlaceOrder.forSku("FLAKY-1"));

                // Well-formed, maps perfectly, and the handler will throw on it every single time.
                case "poison" -> publish(producer, PlaceOrder.forSku("NOPE-404"));

                case "slow" -> publish(producer, PlaceOrder.forSku("GIZMO-SLOW"));

                case "unmappable" -> {
                    // Valid JSON, wrong shape. No number of retries will make this a PlaceOrder.
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
                            + "'. Try: good, flaky, poison, unmappable, slow, burst");
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
