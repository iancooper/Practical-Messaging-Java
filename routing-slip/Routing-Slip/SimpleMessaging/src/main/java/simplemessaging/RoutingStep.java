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
                    T inMessage = inPipe.receive();
                    if (inMessage != null) {
                        T outMessage = operation.execute(inMessage);
                        outMessage.getSteps().get(inMessage.getCurrentStep()).setCompleted(true);

                        int nextStepNo = inMessage.getCurrentStep() + 1;
                        if (inMessage.getSteps().containsKey(nextStepNo)) {
                            var nextStep = inMessage.getSteps().get(nextStepNo);
                            String outRoutingStep = nextStep.getRoutingKey();

                            outMessage.setCurrentStep(nextStepNo);
                            try (DataTypeChannelProducer<T> outPipe = new DataTypeChannelProducer<>(messageSerializer, outRoutingStep, hostName)) {
                                outPipe.send(outMessage);
                            }
                        }
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
