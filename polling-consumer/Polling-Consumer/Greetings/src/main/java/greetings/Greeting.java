package greetings;

import simplemessaging.IAmAMessage;

public class Greeting implements IAmAMessage{
    private String salutation = "Hello World";

    public String getSalutation() {
        return salutation;
    }

    public void setSalutation(String salutation) {
        this.salutation = salutation;
    }
}
