package simplemessaging;

import java.util.concurrent.*;
import java.util.function.Function;

public class RoutingStep<T extends IAmARoutingSlip> implements Runnable {
    private final String thisRoutingKey;
    private final IAmAnOperation<T> operation;
    private final Function<String, T> messageDeserializer;
    private final Function<T, String> messageSerializer;
    private final String hostName;

    public RoutingStep(
            Function<String, T> messageDeserializer,
            Function<T, String> messageSerializer,
            IAmAnOperation<T> operation,
            String thisRoutingKey,
            String hostName) {
        this.thisRoutingKey = thisRoutingKey;
        this.operation = operation;
        this.messageDeserializer = messageDeserializer;
        this.messageSerializer = messageSerializer;
        this.hostName = hostName;
    }

    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try (DataTypeChannelConsumer<T> inPipe = new DataTypeChannelConsumer<>(messageDeserializer, thisRoutingKey, hostName)) {
                        /* TODO
                         * receive a message from the in pipe
                         * if we get non-null message
                         *     execute the operation on it to get the out message
                         *     complete the step on te in message
                         *     increment the step counter
                         *     if there is a step for the next step counter
                         *         retrieve the routing key from the next step
                         *         set the next step on the outgoing message
                         *         create an outpipe DataTypeChannelProducer
                         *             send the message
                         *         dispose of the producer
                         */
                    } else {
                        Thread.yield();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
