package simplemessaging;

public interface IAmAnOperation<TIn extends IAmAMessage, TOut extends IAmAMessage> {
    TOut execute(TIn message);
}
