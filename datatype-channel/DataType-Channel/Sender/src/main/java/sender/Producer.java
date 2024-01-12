package sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import simplemessaging.DataTypeChannelProducer;
import greeting.Greeting;

import java.io.IOException;

public class Producer {
    public static void main(String[] args) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        try (DataTypeChannelProducer<Greeting> channel = new DataTypeChannelProducer<>(greeting -> {
            try {
                return objectMapper.writeValueAsString(greeting);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        },"greeting", "localhost")) {
            Greeting greeting = new Greeting();
            greeting.setSalutation("Hello World!");
            channel.send(greeting);
            System.out.println("Sent message " + greeting.getSalutation());
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Press [enter] to exit.");
        System.in.read();
    }
}
