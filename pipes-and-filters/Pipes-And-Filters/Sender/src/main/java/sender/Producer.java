package sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import greetings.Greeting;
import simplemessaging.DataTypeChannelProducer;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Producer {
    public static void main(String[] args) throws Exception {
        try (var executorService = Executors.newSingleThreadExecutor()) {
            System.out.println("Consumer running, entering loop until signalled");
            System.out.println(" Press [enter] to exit.");

            // has its own thread and will continue until signaled
            var future = executorService.submit(Producer::send);
            System.in.read();
            System.out.println("Exiting Producer");
            future.cancel(true);
        }
    }

    private static void send() {
        ObjectMapper objectMapper = new ObjectMapper();

        try (DataTypeChannelProducer<Greeting> channel = new DataTypeChannelProducer<>(greeting -> {
            try {
                return objectMapper.writeValueAsString(greeting);
            } catch (IOException e) {
                throw new RuntimeException("Error deserializing message", e);
            }
        }, "greeting", "localhost")) {
            System.out.println(" Press [enter] to exit.");
            int loop = 0;
            while (!Thread.interrupted()) {
                Greeting greeting = new Greeting();
                greeting.setSalutation("Hello World! #" + loop);
                channel.send(greeting);
                System.out.println("Sent message " + greeting.getSalutation());
                loop++;

                if (loop % 10 == 0) {
                    System.out.println("Pause for breath");
                    TimeUnit.SECONDS.sleep(3); // yield
                }
            }
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
