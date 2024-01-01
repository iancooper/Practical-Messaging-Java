import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class Producer {
    public static void main(String[] args) {
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
                             "greeting",
                             "localhost"
                     )
        ) {
            Greeting greeting = new Greeting();
            greeting.setSalutation("Hello World!");

            GreetingResponse response = channel.call(greeting, 5000);

            System.out.println("Sent message Greeting " + greeting.getSalutation() +
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
        new java.util.Scanner(System.in).nextLine();
    }
}
