package sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import greeting.BadGreeting;
import simplemessaging.DataTypeChannelProducer;

import java.io.IOException;

public class Producer {
    public static void main(String[] args) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        try (DataTypeChannelProducer<BadGreeting> channel = new DataTypeChannelProducer<>(greeting -> {
            try {
                return objectMapper.writeValueAsString(greeting);
            } catch (IOException e) {
                throw new RuntimeException("Error deserializing message", e);
            }
        },"badgreeting", "localhost")) {
            var greeting = new BadGreeting();
            greeting.setNumber(1234);
            channel.send(greeting);
            System.out.println("Sent message " + greeting.getNumber());
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Press [enter] to exit.");
        System.in.read();
    }
}
