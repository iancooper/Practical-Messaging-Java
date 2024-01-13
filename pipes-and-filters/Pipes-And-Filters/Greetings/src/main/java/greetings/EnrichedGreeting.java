package greetings;

import simplemessaging.IAmAMessage;

public class EnrichedGreeting extends Greeting implements IAmAMessage {
    private String recipient;

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
}
