package receiver;

import greetings.Greeting;
import greetings.GreetingHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import simplemessaging.IAmAHandler;
import simplemessaging.PollingConsumer;

import java.io.IOException;
import java.util.concurrent.*;
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

        var consumer = new PollingConsumer<Greeting>(greetingHandler, messageDeserializer, "greeting", "localhost");


        try (var executorService = Executors.newSingleThreadExecutor()) {
            System.out.println("Consumer running, entering loop until signalled");
            System.out.println(" Press [enter] to exit.");

            var future = executorService.submit(consumer);
            System.in.read();
            System.out.println("Exiting consumer");
            future.cancel(true);
        }
    }
}
