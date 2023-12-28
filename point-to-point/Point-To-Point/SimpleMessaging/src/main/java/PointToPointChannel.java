import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class PointToPointChannel implements AutoCloseable {
    private final String routingKey;
    private final String queueName;
    private static final String exchangeName = "practical-messaging";
    private final Connection connection;
    private final Channel channel;

    public PointToPointChannel(String queueName, String hostName) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(hostName);
        factory.setAutomaticRecoveryEnabled(true);
        connection = factory.newConnection();
        channel = connection.createChannel();

        // Because we are point to point, we are just going to use queueName for the routing key
        routingKey = queueName;
        this.queueName = queueName;

        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT, false);
        channel.queueDeclare(this.queueName, false, false, false, null);
        channel.queueBind(this.queueName, exchangeName, routingKey);
    }

    public void send(String message) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);
        channel.basicPublish(exchangeName, routingKey, null, body);
    }

    public String receive() throws IOException {
        GetResponse result = channel.basicGet(queueName, true);
        if (result != null) {
            return new String(result.getBody(), StandardCharsets.UTF_8);
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
