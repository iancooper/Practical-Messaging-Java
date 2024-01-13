package greetings;

import simplemessaging.IAmAnOperation;

public class GreetingEnricher implements IAmAnOperation<Greeting> {
    @Override
    public Greeting execute(Greeting message) {
        System.out.printf("Recieved greeing: %s%n", message.getSalutation());
        message.setRecipient("Clarissa Harlowe");
        System.out.printf("Enriched with: %s%n", message.getRecipient());
        return message;
    }
}
