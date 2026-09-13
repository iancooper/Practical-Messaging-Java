package simplemessaging;

/**
 * The contract your application code implements so the pump can dispatch to it.
 * <p>
 * A domain type in, and nothing else. No delivery, no channel, no headers, no ack. The handler
 * does not know the pump exists, which is exactly why a test can call it too.
 */
public interface IAmAHandler<T extends IAmAMessage> {
    void handle(T message) throws Exception;
}
