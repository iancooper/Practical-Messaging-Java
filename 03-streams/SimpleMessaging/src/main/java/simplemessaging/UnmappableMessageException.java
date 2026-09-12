package simplemessaging;

/**
 * "I was handed this message and I cannot read it."
 * <p>
 * The gateway raises this so the pump does not have to know whether the body was JSON, XML or
 * protobuf. Retrying it is pointless: the bytes will not change.
 * <p>
 * It is unchecked because the pump catches it by type rather than being forced to declare it --
 * the decision about what to do with an unreadable body is policy, and policy belongs in the pump.
 */
public class UnmappableMessageException extends RuntimeException {
    public UnmappableMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
