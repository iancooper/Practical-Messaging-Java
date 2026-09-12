package receiver;

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

/**
 * A command in on a queue, a fact out on a stream. This process is both a RabbitMQ consumer and a
 * Kafka producer, which is an extremely common shape and the reason exercise 3 exists.
 * <pre>
 *   java -jar Receiver/target/Receiver.jar
 *   DUAL_WRITE_WINDOW=15 java -jar Receiver/target/Receiver.jar     # for Probe A
 * </pre>
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

        try (KafkaEventPublisher<OrderPlaced> publisher = new KafkaEventPublisher<>(
                new EventStreamProducer<>(
                        OrderPlaced::serialize,
                        OrderPlaced::orderId,
                        OrderPlaced.class))) {

            MessagePump<PlaceOrder> pump = new MessagePump<>(
                    PlaceOrder.class,
                    new PlaceOrderMapper(),
                    new PlaceOrderHandler(new Catalogue(), publisher));

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
}
