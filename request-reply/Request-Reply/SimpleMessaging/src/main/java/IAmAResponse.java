import java.util.UUID;

public interface IAmAResponse {
    UUID getCorrelationId();
    void setCorrelationId(UUID correlationId);

    String getResult();

    void setResult(String result);
}
