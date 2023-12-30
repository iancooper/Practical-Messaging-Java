import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class Consumer {
    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();

        try (DataTypeChannelConsumer<Greeting> channel = new DataTypeChannelConsumer<>(messageBody -> {
            try {
                return objectMapper.readValue(messageBody, Greeting.class);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }, "greeting", "localhost")) {
            Greeting greeting = channel.receive();
            if (greeting != null)
                System.out.println("Received message " + greeting.getSalutation());
            else
                System.out.println("Did not receive message");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Press [enter] to exit.");
        new java.util.Scanner(System.in).nextLine();
    }
}
