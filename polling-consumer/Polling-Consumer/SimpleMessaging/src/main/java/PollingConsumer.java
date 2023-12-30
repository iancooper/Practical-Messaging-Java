import java.util.concurrent.*;
import java.util.function.Function;

import static java.lang.Thread.sleep;

public class PollingConsumer<T extends IAmAMessage> {
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

    public Future<?> run(ExecutorService executor) {
         return executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (DataTypeChannelConsumer<T> channel = new DataTypeChannelConsumer<>(messageDeserializer,routingKey, hostName)) {
                    T message = channel.receive();
                    if (message != null) {
                        messageHandler.handle(message);
                    }
                    Thread.yield();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
