package simplemessaging;

public interface IAmAHandler<T extends IAmAMessage, TResponse extends IAmAResponse> {
    TResponse handle(T message);
}
