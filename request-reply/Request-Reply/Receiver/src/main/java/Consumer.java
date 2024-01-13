import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import greetings.Greeting;
import greetings.GreetingHandler;
import greetings.GreetingResponse;
import simplemessaging.IAmAHandler;
import simplemessaging.PollingConsumer;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class Consumer {
    public static void main(String[] args) throws IOException {
        IAmAHandler<Greeting, GreetingResponse> greetingHandler = new GreetingHandler();

        Function<String, Greeting> messageDeserializer = messageBody -> {
            try {
                return new ObjectMapper().readValue(messageBody, Greeting.class);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        };

        Function<GreetingResponse, String> messageSerializer = response -> {
          try{
              return new ObjectMapper().writeValueAsString(response);
          } catch (Exception e) {
              e.printStackTrace();
              return null;
          }
        };

        var consumer = new PollingConsumer<Greeting, GreetingResponse>(greetingHandler, messageDeserializer, messageSerializer,"greeting-request", "localhost");

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
