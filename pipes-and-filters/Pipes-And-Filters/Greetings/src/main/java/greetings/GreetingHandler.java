package greetings;

import simplemessaging.IAmAHandler;

public class GreetingHandler implements IAmAHandler<EnrichedGreeting> {
    @Override
    public void handle(EnrichedGreeting message) {
        if (message != null) {
            System.out.printf("%s %s%n", message.getSalutation(), message.getRecipient());
        }
    }
}
