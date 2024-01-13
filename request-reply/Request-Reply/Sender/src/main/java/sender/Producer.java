package sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import greetings.Greeting;
import greetings.GreetingResponse;
import simplemessaging.RequestReplyChannelProducer;

import java.io.IOException;
import java.util.UUID;

public class Producer {
    public static void main(String[] args) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        try (RequestReplyChannelProducer<Greeting, GreetingResponse> channel =
                     new RequestReplyChannelProducer<>(
                             greeting -> {
                                try {
                                    return objectMapper.writeValueAsString(greeting);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                     return null;
                                }
                             },
                             messageBody -> {
                                 try {
                                     return objectMapper.readValue(messageBody, GreetingResponse.class);
                                 } catch (IOException e) {
                                     throw new RuntimeException("Error deserializing message", e);
                                 }
                             },
                             "greeting-request",
                             "localhost"
                     )
        ) {
            Greeting greeting = new Greeting();
            greeting.setSalutation("Hello World!");
            greeting.setCorrelationId(UUID.randomUUID());
            greeting.setReplyTo("greeting-replyto");

            GreetingResponse response = channel.call(greeting, 5000);

            System.out.println("Sent message greetings.Greeting " + greeting.getSalutation() +
                    " Correlation Id " + greeting.getCorrelationId());

            if (response != null) {
                System.out.println("Received Message " + response.getResult()+
                        " Correlation Id " + response.getCorrelationId() +
                        " at " + java.time.LocalDateTime.now());
            } else {
                System.out.println("Did not receive a response");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Press [enter] to exit.");
        System.in.read();
    }
}
