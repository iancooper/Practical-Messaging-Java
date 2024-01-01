import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

public class PollingConsumer<T extends IAmAMessage, TResponse extends IAmAResponse> {
    private final IAmAHandler<T, TResponse> messageHandler;
    private final Function<TResponse, String> messageSerializer;
    private final Function<String, T> messageDeserializer;
    private final String hostName;
    private final String routingKey;

    public PollingConsumer(
            IAmAHandler<T, TResponse> messageHandler,
            Function<String, T> messageDeserializer,
            Function<TResponse, String> messageSerializer,
            String routingKey,
            String hostName) {
        this.messageHandler = messageHandler;
        this.messageDeserializer = messageDeserializer;
        this.messageSerializer = messageSerializer;
        this.hostName = hostName;
        this.routingKey = routingKey;
    }

    public Future<?> run(ExecutorService executor) {
         return executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (RequestReplyChannelConsumer<T> channel = new RequestReplyChannelConsumer<>(messageDeserializer,routingKey, hostName)) {
                        T request = channel.receive();
                        if (request != null) {
                            TResponse response = messageHandler.handle(request);

                            try (RequestReplyChannelResponder<TResponse> responder =  new RequestReplyChannelResponder<>(messageSerializer, hostName)) {
                                responder.respond(request.getReplyTo(), response);
                            }
                        } else {
                            System.out.println("Did not receive message");
                        }

                        Thread.yield();
                    }  catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
