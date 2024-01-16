package simplemessaging;

import java.util.function.Function;

public class Filter<TIn extends IAmAMessage, TOut extends IAmAMessage> implements Runnable {
    private final IAmAnOperation<TIn, TOut> operation;
    private final Function<String, TIn> messageDeserializer;
    private final Function<TOut, String> messageSerializer;
    private final String hostName;
    private final String inRoutingKey;
    private final String outRoutingKey;

    public Filter(IAmAnOperation<TIn, TOut> operation, Function<String, TIn> messageDeserializer, Function<TOut, String> messageSerializer, String inRoutingKey, String outRoutingKey, String hostName) {
        this.operation = operation;
        this.messageDeserializer = messageDeserializer;
        this.messageSerializer = messageSerializer;
        this.hostName = hostName;
        this.inRoutingKey = inRoutingKey;
        this.outRoutingKey = outRoutingKey;
    }

    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                /*TODO
                 *
                 * Create an in pipe from a DataTypeChannelConsumer
                 * while true
                 *     read from the inpipe
                 *     if we get a message
                 *         use the operation to transform the message
                 *         create a DataTypeChannelProducer for the out pipe
                 *             Send the message on the outpipe
                 *         dispose of the producer
                 *     else
                 *        yield
                 * displose of the consumer
                 */
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
