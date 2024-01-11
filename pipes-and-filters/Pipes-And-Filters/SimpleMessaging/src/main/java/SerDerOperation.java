public interface SerDerOperation<IN, OUT> {

    public OUT apply(IN in) throws SerDerException;
}
