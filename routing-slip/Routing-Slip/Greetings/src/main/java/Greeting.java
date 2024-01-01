public class Greeting extends RoutingSlip implements IAmAMessage {
    private String salutation = "Hello World";
    private String recipient;

    public String getSalutation() {
        return salutation;
    }

    public void setSalutation(String salutation) {
        this.salutation = salutation;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
}
