package greetings;

import simplemessaging.IAmAHandler;

public class GreetingHandler implements IAmAHandler<Greeting, GreetingResponse> {
    @Override
    public GreetingResponse handle(Greeting message) {
        if (message != null) {
            System.out.printf("Received message with correlcation id %s amd salutation %s", message.getCorrelationId().toString(), message.getSalutation());

            var response = new GreetingResponse();
            response.setCorrelationId(message.getCorrelationId());
            response.setResult(String.format("Received Greeting: %s", message.getSalutation()));

            return response;
        }
        return null;
    }
}
