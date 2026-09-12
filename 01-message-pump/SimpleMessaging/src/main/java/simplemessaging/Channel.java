package simplemessaging;

/**
 * The names both ends of the channel have to agree on, in one place.
 * <p>
 * A Datatype Channel carries one type of message, so we derive the routing key from the type.
 * Producer and consumer both compute it, which is how they find each other without a shared
 * config file.
 * <p>
 * <b>Why these methods take a {@code Class<?>} rather than using a type parameter:</b> Java
 * erases generics, so a generic class cannot ask what {@code T} was at runtime. C# can write
 * {@code typeof(T).FullName}; here the type has to be passed in and carried around. It is the
 * same Datatype Channel either way -- but it is worth noticing that the language changed the
 * shape of the code without changing the pattern.
 */
public final class Channel {
    private Channel() {
    }

    public static final String HOST_NAME = "localhost";
    public static final String EXCHANGE_NAME = "practical-messaging-pump";

    public static String routingKeyFor(Class<?> type) {
        return "message-pump." + type.getName();
    }

    public static String queueNameFor(Class<?> type) {
        return routingKeyFor(type);
    }
}
