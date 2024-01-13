package enricher;

import com.fasterxml.jackson.databind.ObjectMapper;
import greetings.EnrichedGreeting;
import greetings.Greeting;
import greetings.GreetingEnricher;
import simplemessaging.Filter;
import simplemessaging.IAmAnOperation;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.function.Function;

public class Enricher {
    public static void main(String[] args) throws IOException {
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

        var enricher = new Filter<>(greetingEnricher, greetingDeserializer, enrichedGreetingSerializer, "greeting", "enrichedgreeting", "localhost");

        try (var executorService = Executors.newSingleThreadExecutor()) {
            System.out.println("Enricher running, entering loop until signaled");
            System.out.println(" Press [enter] to exit.");
            // has its own thread and will continue until signaled
            var future = executorService.submit(enricher);
            System.in.read();
            System.out.println("Exiting Enricher");
            future.cancel(true);
        }
    }
}
