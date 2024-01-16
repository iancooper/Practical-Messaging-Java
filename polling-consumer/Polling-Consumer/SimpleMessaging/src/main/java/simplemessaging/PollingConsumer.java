package simplemessaging;

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

        /*
         * TODO:
         * while the thread is not interrupted
         *     create a data type channel consumer
         *         try go get a message
         *         dispatch that message to a handler
         *         yield
         *     dispose of the channel
         */
    }
}
