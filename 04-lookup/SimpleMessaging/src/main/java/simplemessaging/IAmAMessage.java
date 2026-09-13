package simplemessaging;

/**
 * Anything we are willing to put on a channel.
 * <p>
 * The id is the message's identity, not the entity's -- we need it to key a stream, and later
 * to answer "have I seen this before?".
 */
public interface IAmAMessage {
    String id();
}
