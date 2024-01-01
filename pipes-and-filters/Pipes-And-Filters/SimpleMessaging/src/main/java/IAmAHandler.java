public interface IAmAHandler<T extends IAmAMessage> {
    void handle(T message);
}
