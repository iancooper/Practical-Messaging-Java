package simplemessaging;

/**
 * The Translate stage: a message body on the wire becomes a domain object.
 * <p>
 * This is the seam. Everything on the broker side of it is the gateway's business; everything on
 * the other side is yours.
 * <p>
 * It throws when the body is not the type this channel carries. That throw is a <i>different kind
 * of failure</i> from one thrown by a handler, and the pump has to treat it differently -- which
 * is most of this exercise.
 */
public interface IAmAMessageMapper<T extends IAmAMessage> {
    /** @throws UnmappableMessageException the body is not a T and never will be. */
    T mapToRequest(String body);
}
