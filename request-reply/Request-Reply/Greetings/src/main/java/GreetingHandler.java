public class GreetingHandler implements IAmAHandler<Greeting> {
    @Override
    public GreetingResponse handle(Greeting message) {
        if (message != null) {
            System.out.printf("Received message with correlcation id %s amd salutation %s", message.getCorrelationId().toString(), message.getSalutation());

            return new
        }
    }
}
