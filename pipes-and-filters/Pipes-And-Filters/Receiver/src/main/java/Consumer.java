import com.fasterxml.jackson.databind.ObjectMapper;
import greetings.EnrichedGreeting;
import greetings.GreetingHandler;
import simplemessaging.IAmAHandler;
import simplemessaging.PollingConsumer;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class Consumer {
    public static void main(String[] args) throws IOException {
        IAmAHandler<EnrichedGreeting> greetingHandler = new GreetingHandler();

        Function<String, EnrichedGreeting> messageDeserializer = messageBody -> {
            try {
                return new ObjectMapper().readValue(messageBody, EnrichedGreeting.class);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        };

        var consumer = new PollingConsumer<EnrichedGreeting>(greetingHandler, messageDeserializer, "enrichedgreeting", "localhost");

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
