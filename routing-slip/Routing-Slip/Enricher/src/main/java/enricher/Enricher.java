package enricher;

import com.fasterxml.jackson.databind.ObjectMapper;
import greetings.GlobalStepList;
import greetings.Greeting;
import greetings.GreetingEnricher;
import simplemessaging.IAmAnOperation;
import simplemessaging.RoutingStep;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.function.Function;

public class Enricher {
    public static void main(String[] args) throws IOException {
        IAmAnOperation<Greeting> greetingEnricher = new GreetingEnricher();

        Function<String, Greeting> greetingDeserializer = messageBody -> {
            try {
                return new ObjectMapper().readValue(messageBody, Greeting.class);
            } catch (IOException e) {
                throw new RuntimeException("Error deserializing message", e);
            }
        };
        Function<Greeting, String> enrichedGreetingSerializer = enrichedGreeting -> {
            try {
                return new ObjectMapper().writeValueAsString(enrichedGreeting);
            } catch (IOException e) {
                throw new RuntimeException("Error deserializing message", e);
            }
        };

        var consumer = new RoutingStep<Greeting>(greetingDeserializer, enrichedGreetingSerializer, new GreetingEnricher(), GlobalStepList.Enricher, "localhost");

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
