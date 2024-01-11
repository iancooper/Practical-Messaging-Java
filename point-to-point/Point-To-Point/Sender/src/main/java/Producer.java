import java.io.IOException;
import java.util.Scanner;

public class Producer {
    public static void main(String[] args) throws IOException {
        try (PointToPointChannel channel = new PointToPointChannel("hello-p2p", "localhost")) {
            String message = "Hello World!";
            channel.send(message);
            System.out.println("Sent message " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Press [enter] to exit.");
        System.in.read();
    }
}
