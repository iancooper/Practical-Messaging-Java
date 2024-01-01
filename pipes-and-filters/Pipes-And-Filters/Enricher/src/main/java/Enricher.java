import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.function.Function;

public class Enricher {
    public static void main(String[] args) {
        IAmAnOperation<Greeting, EnrichedGreeting> greetingEnricher = new GreetingEnricher();

        Function<String, Greeting> greetingDeserializer = messageBody -> {
            try {
                return new ObjectMapper().readValue(messageBody, Greeting.class);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        };
        Function<EnrichedGreeting, String> enrichedGreetingSerializer = enrichedGreeting -> {
            try {
                return new ObjectMapper().writeValueAsString(enrichedGreeting);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        };

        var consumer = new Filter<>(greetingEnricher, greetingDeserializer, enrichedGreetingSerializer, "greeting", "enrichedgreeting", "localhost");

        var executorService = Executors.newSingleThreadExecutor();

        try {
            System.out.println("Enricher running, entering loop until signaled");
            System.out.println(" Press [enter] to exit.");
            // has its own thread and will continue until signaled
            var task = consumer.run(executorService);

            while (true) {
                // loop until we get a keyboard interrupt
                if (System.in.available() > 0) {
                    // Note: This will deadlock with System.out.println on the task thread unless we have called println first
                    char key = (char) System.in.read();
                    if (key == '\n') {
                        // signal exit
                        task.cancel(true);
                        break;
                    }

                    Thread.yield();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }
}
