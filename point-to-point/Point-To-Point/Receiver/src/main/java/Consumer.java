import java.util.Scanner;

public class Consumer {
    public static void main(String[] args) {
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
        new Scanner(System.in).nextLine();
    }
}
