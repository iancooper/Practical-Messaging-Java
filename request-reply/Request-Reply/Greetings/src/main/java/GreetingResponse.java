import java.util.UUID;

public class GreetingResponse implements IAmAResponse{
    private UUID correlationId;
    private String result;
    @Override
    public UUID getCorrelationId() {
        return correlationId;
    }

    @Override
    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public String getResult() {
        return result;
    }

    @Override
    public void setResult(String result) {
        this.result = result;
    }
}
