package simplemessaging;

import java.util.function.Function;

public class PollingConsumer<T extends IAmAMessage, TResponse extends IAmAResponse> implements Runnable {
    private final IAmAHandler<T, TResponse> messageHandler;
    private final Function<String, T> messageDeserializer;
    private final Function<TResponse, String> messageSerializer;
    private final String hostName;
    private final String routingKey;

    public PollingConsumer(IAmAHandler<T, TResponse> messageHandler, Function<String, T> messageDeserializer, Function<TResponse, String> messageSerializer, String routingKey, String hostName) {
        this.messageHandler = messageHandler;
        this.messageDeserializer = messageDeserializer;
        this.messageSerializer = messageSerializer;
        this.hostName = hostName;
        this.routingKey = routingKey;
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try (var channel = new RequestReplyChannelConsumer<T>(messageDeserializer, routingKey, hostName)) {
                T message = channel.receive();
                /*
                 * TODO: receive a request on the channel
                 * if the request is not null then
                 *     handle the message
                 *     create a RequestReplyChannelResponder
                 *     respond to the request, with the response from the handler
                 */
                } else {
                    Thread.yield();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}