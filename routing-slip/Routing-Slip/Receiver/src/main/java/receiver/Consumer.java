package receiver;

import com.fasterxml.jackson.databind.ObjectMapper;
import greetings.GlobalStepList;
import greetings.Greeting;
import greetings.GreetingHandler;
import simplemessaging.IAmAHandler;
import simplemessaging.PollingConsumer;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class Consumer {
    public static void main(String[] args) throws IOException {
        IAmAHandler<Greeting> greetingHandler = new GreetingHandler();

        Function<String, Greeting> messageDeserializer = messageBody -> {
            try {
                return new ObjectMapper().readValue(messageBody, Greeting.class);
            } catch (Exception e) {
                throw new RuntimeException("Error deserializing message", e);
            }
        };

        var consumer = new PollingConsumer<Greeting>(greetingHandler, messageDeserializer, GlobalStepList.Receiver, "localhost");

        try (var executorService = Executors.newSingleThreadExecutor()) {
            System.out.println("Consumer running, entering loop until signalled");
            System.out.println(" Press [enter] to exit.");

            // has its own thread and will continue until signaled
            var future = executorService.submit(consumer);
            System.in.read();
            System.out.println("Exiting Consumer");
            future.cancel(true);
        }
    }
}
