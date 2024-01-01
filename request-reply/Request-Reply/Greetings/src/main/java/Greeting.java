import java.util.UUID;

public class Greeting implements IAmAMessage {
    private String salutation = "Hello World";
    private UUID correlationId;
    private String replyTo;

    public String getSalutation() {
        return salutation;
    }

    public void setSalutation(String salutation) {
        this.salutation = salutation;
    }

    @Override
    public UUID getCorrelationId() {
        return correlationId;
    }

    @Override
    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public String getReplyTo() {
        return replyTo;
    }

    @Override
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }
}
