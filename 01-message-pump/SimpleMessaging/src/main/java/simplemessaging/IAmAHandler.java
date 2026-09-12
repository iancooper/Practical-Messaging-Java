package simplemessaging;

import com.rabbitmq.client.GetResponse;

/**
 * The contract your application code implements so the pump can dispatch to it.
 */
public interface IAmAHandler<T extends IAmAMessage> {
    void handle(GetResponse delivery) throws Exception;
}
