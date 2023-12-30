import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class DataTypeChannelProducer<T extends IAmAMessage> implements AutoCloseable {
    private final Function<T, String> messageSerializer;
    private final String routingKey;
    private static final String exchangeName = "practical-messaging";
    private final Connection connection;
    private final Channel channel;

    public DataTypeChannelProducer(Function<T, String> messageSerializer, String routingKey, String hostName) throws IOException, TimeoutException {
        this.messageSerializer = messageSerializer;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(hostName);
        factory.setAutomaticRecoveryEnabled(true);
        connection = factory.newConnection();
        channel = connection.createChannel();

        this.routingKey = routingKey;
        String queueName = routingKey;

        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT, false);
        channel.queueDeclare(queueName, false, false, false, null);
        channel.queueBind(queueName, exchangeName, routingKey);
    }

    public void send(T message) throws IOException {
        byte[] body = messageSerializer.apply(message).getBytes(StandardCharsets.UTF_8);
        channel.basicPublish(exchangeName, routingKey, null, body);
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
