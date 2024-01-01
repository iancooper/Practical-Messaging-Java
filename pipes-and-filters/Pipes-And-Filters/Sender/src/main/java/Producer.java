import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Producer {
    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();

        try (DataTypeChannelProducer<Greeting> channel = new DataTypeChannelProducer<>(greeting -> {
            try {
                return objectMapper.writeValueAsString(greeting);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }, "greeting", "localhost")) {
            System.out.println(" Press [enter] to exit.");
            int loop = 0;
            Scanner scanner = new Scanner(System.in);
            while (true) {
                // loop until we get a keyboard interrupt
                if (System.in.available() > 0) {
                    char key = scanner.nextLine().charAt(0);
                    if (key == '\n') {
                        break;
                    }
                }

                Greeting greeting = new Greeting();
                greeting.setSalutation("Hello World! #" + loop);
                channel.send(greeting);
                System.out.println("Sent message " + greeting.getSalutation());
                loop++;

                if (loop % 10 == 0) {
                    System.out.println("Pause for breath");
                    try {
                        TimeUnit.SECONDS.sleep(3); // yield
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
