package simplemessaging;

import java.util.UUID;

public interface IAmAMessage {
    UUID getCorrelationId();
    void setCorrelationId(UUID correlationId);
    String getReplyTo();
    void setReplyTo(String replyTo);

}
