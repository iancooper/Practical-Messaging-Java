import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

public class PollingConsumer<T extends IAmAMessage> implements Runnable {
    private final IAmAHandler<T> messageHandler;
    private final Function<String, T> messageDeserializer;
    private final String hostName;
    private final String routingKey;

    public PollingConsumer(IAmAHandler<T> messageHandler, Function<String, T> messageDeserializer, String routingKey, String hostName) {
        this.messageHandler = messageHandler;
        this.messageDeserializer = messageDeserializer;
        this.hostName = hostName;
        this.routingKey = routingKey;
    }

    public void run() {
        try (DataTypeChannelConsumer<T> channel = new DataTypeChannelConsumer<>(messageDeserializer, routingKey, hostName)) {
            while (!Thread.currentThread().isInterrupted()) {
                T message = channel.receive();
                if (message != null) {
                    messageHandler.handle(message);
                } else {
                    Thread.yield();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
