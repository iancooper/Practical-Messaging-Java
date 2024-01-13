package simplemessaging;

public interface IAmAnOperation<T extends IAmARoutingSlip> {
    T execute(T message);
}
