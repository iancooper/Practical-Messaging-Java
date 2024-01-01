import java.io.IOException;
import java.util.concurrent.*;
import java.util.function.Function;

public class Filter<TIn extends IAmAMessage, TOut extends IAmAMessage> {
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

    public Future<?> run(ExecutorService executor) {
        return executor.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    try (DataTypeChannelConsumer<TIn> inPipe = new DataTypeChannelConsumer<>(messageDeserializer, inRoutingKey, hostName)) {
                        TIn inMessage = inPipe.receive();
                        if (inMessage != null) {
                            TOut outMessage = operation.execute(inMessage);
                            try (DataTypeChannelProducer<TOut> outPipe = new DataTypeChannelProducer<>(messageSerializer, outRoutingKey, hostName)) {
                                outPipe.send(outMessage);
                            }
                        } else {
                            Thread.yield();
                        }
                    }
                }
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        });
    }
}
