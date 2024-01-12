package receiver;

import simplemessaging.PointToPointChannel;

import java.io.IOException;

public class Consumer {
    public static void main(String[] args) throws IOException {
        try (PointToPointChannel channel = new PointToPointChannel("hello-p2p", "localhost")) {
            String message = channel.receive();
            if (message != null)
                System.out.println("Received message " + message);
            else
                System.out.println("Did not receive message");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Press [enter] to exit.");
        System.in.read();
    }
}
