import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class Producer {
    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();

        try (DataTypeChannelProducer<BadGreeting> channel = new DataTypeChannelProducer<>(greeting -> {
            try {
                return objectMapper.writeValueAsString(greeting);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        },"greeting", "localhost")) {
            var greeting = new BadGreeting();
            greeting.setNumber(1234);
            channel.send(greeting);
            System.out.println("Sent message " + greeting.getNumber());
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Press [enter] to exit.");
        new java.util.Scanner(System.in).nextLine();
    }
}
