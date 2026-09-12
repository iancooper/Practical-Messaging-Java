package receiver;

import model.Catalogue;
import model.PlaceOrder;
import model.PlaceOrderHandler;
import simplemessaging.MessagePump;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * The consumer. Runs a Message Pump until you stop it.
 * <pre>
 *   java -jar Receiver/target/Receiver.jar
 * </pre>
 * Ctrl-C stops it <i>between</i> messages, which is the polite ending.
 * <p>
 * Several probes want the rude ending instead -- a process that dies with a message in its
 * hands. Use the PID printed below, from another terminal:
 * <pre>
 *   kill -9 &lt;pid&gt;
 * </pre>
 */
public final class Receiver {
    /** Set when the pump has returned or thrown, so the shutdown hook knows which ending this is. */
    private static volatile boolean finished;

    public static void main(String[] args) throws Exception {
        // Flush every line. Several probes end with this process being killed, and a buffered
        // line you never see is a probe you cannot read.
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true));

        long pid = ProcessHandle.current().pid();
        System.out.println("Receiver starting. PID " + pid);
        System.out.println("Ctrl-C to stop between messages; 'kill -9 " + pid + "' to stop mid-message.");
        System.out.println();

        MessagePump<PlaceOrder> pump =
                new MessagePump<>(PlaceOrder.class, new PlaceOrderHandler(new Catalogue()));

        // Ctrl-C asks the pump to stop, and then we wait for it: tearing the process down while a
        // message is in flight is the rude ending, and that is what kill -9 is for.
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

        System.out.println("Receiver stopped.");
    }
}
