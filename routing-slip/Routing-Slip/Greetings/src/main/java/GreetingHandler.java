public class GreetingHandler implements IAmAHandler<Greeting> {
    @Override
    public void handle(Greeting message) {
        if (message != null) {
            System.out.printf("%s %s%n", message.getSalutation(), message.getRecipient());
        }
    }
}
