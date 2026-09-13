package sender;

import model.OrderPlaced;
import model.PlaceOrder;
import simpleeventing.EventStreamProducer;
import simplemessaging.DataTypeChannelProducer;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Puts things on channels, including things the other end will not like.
 * <pre>
 *   java -jar Sender/target/Sender.jar                  a good order, on the queue
 *   java -jar Sender/target/Sender.jar burst 20         twenty good orders
 *   java -jar Sender/target/Sender.jar poison           a SKU no price was ever published for (queue-side failure)
 *   java -jar Sender/target/Sender.jar unmappable       a body that is not a PlaceOrder (queue-side failure)
 *   java -jar Sender/target/Sender.jar bad-event        A RECORD THE STREAM CONSUMER CANNOT READ -- straight onto Kafka
 * </pre>
 * 'flaky' and 'slow' are gone. They sent GIZMO-SLOW and FLAKY-1, which were failures of a lookup
 * that was called on demand -- and there is no call any more. Removing the thing that could fail is
 * not the same as fixing it, and Probe B is the bill.
 */
public final class Sender {
    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true));

        String command = args.length > 0 ? args[0].toLowerCase() : "good";

        if (command.equals("bad-event")) {
            // Appended to the stream directly, because we need a poison *record* rather than a
            // poison message. There is no such thing as putting it on an invalid record topic for us.
            try (EventStreamProducer<OrderPlaced> stream = new EventStreamProducer<>(
                    OrderPlaced::serialize, OrderPlaced::orderId, OrderPlaced.class)) {
                String record = "{\"Id\":\"c0ffee\",\"OrderRef\":\"not-a-field\",\"Sku\":\"WIDGET-1\"}";
                System.out.println("Appending a record the consumer cannot map:");
                System.out.println("  " + record);
                stream.sendRaw("poison", record);
            }
            return;
        }

        try (DataTypeChannelProducer<PlaceOrder> producer =
                     new DataTypeChannelProducer<>(PlaceOrder::serialize, PlaceOrder.class)) {

            switch (command) {
                case "good" -> publish(producer, PlaceOrder.forSku("WIDGET-1"));

                // Well-formed, maps perfectly, and the handler will throw on it every single time.
                case "poison" -> publish(producer, PlaceOrder.forSku("NOPE-404"));

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
                            + "'. Try: good, poison, unmappable, burst, bad-event");
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
