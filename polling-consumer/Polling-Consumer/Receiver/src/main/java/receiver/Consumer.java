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

        //TODO: Create a single threaded executor
        //Submit the consumer to the executor
        //Poll for a keyboard interrupt
        //On receiving the interrupt cancel the Future returned from the executor
    }
}
