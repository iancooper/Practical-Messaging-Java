package greetings;

import simplemessaging.IAmAnOperation;

public class GreetingEnricher implements IAmAnOperation<Greeting, EnrichedGreeting> {
    @Override
    public EnrichedGreeting execute(Greeting message) {
        System.out.printf("Recieved greeing: %s%n", message.getSalutation());
        var enriched = new EnrichedGreeting();
        enriched.setSalutation(message.getSalutation());
        enriched.setRecipient("Clarissa Harlowe");
        System.out.printf("Enriched with: %s%n", enriched.getRecipient());
        return enriched;
    }
}
