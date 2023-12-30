import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;


public class DataTypeChannelConsumer<T extends IAmAMessage> implements AutoCloseable {
    private final Function<String, T> messageDeserializer;
    private final String queueName;
    private static final String exchangeName = "practical-messaging";
    private static final String invalidExchangeName = "practical-messaging-invalid";
    private final Connection connection;
    private final Channel channel;

    public DataTypeChannelConsumer(Function<String, T> messageDeserializer, String routingKey, String hostName) throws IOException, TimeoutException {
        this.messageDeserializer = messageDeserializer;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(hostName);
        factory.setAutomaticRecoveryEnabled(true);
        connection = factory.newConnection();
        channel = connection.createChannel();

        queueName = routingKey;
        var invalidRoutingKey = "invalid" + routingKey;
        var invalidMessageQueueName = invalidRoutingKey;

        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT, false);

        var arguments = new HashMap<String, Object>();
        arguments.put("x-dead-letter-exchange", invalidExchangeName);
        arguments.put("x-dead-letter-routing-key", invalidRoutingKey);

        channel.queueDeclare(queueName, false, false, false, arguments);
        channel.queueBind(queueName, exchangeName, routingKey);
    }

    public T receive() throws IOException {
        GetResponse result = channel.basicGet(queueName, true);
        if (result != null) {
            return messageDeserializer.apply(new String(result.getBody(), StandardCharsets.UTF_8));
        } else {
            return null;
        }
    }

    @Override
    public void close() {
        releaseResources();
    }

    private void releaseResources() {
        try {
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
