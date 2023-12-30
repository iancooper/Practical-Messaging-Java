public class GreetingHandler implements IAmAHandler<Greeting> {
    @Override
    public void handle(Greeting message) {
        if (message != null) {
            System.out.println(message.getSalutation());
        }
    }
}
