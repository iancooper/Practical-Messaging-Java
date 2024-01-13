package simplemessaging;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class RequestReplyChannelResponder<TResponse extends IAmAResponse> implements AutoCloseable {
    private final Function<TResponse, String> messageSerializer;
    private final Connection connection;
    private final Channel channel;

    public RequestReplyChannelResponder(Function<TResponse, String> messageSerializer, String hostName) throws IOException, TimeoutException {
        this.messageSerializer = messageSerializer;

        // just use defaults: usr: guest pwd: guest port:5672 virtual host: /
        // it would make sense to pool the connections so that this and the consumer use the same one
        // with multiplexed channels, but we don't implement that here.
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(hostName);
        factory.setAutomaticRecoveryEnabled(true);
        this.connection = factory.newConnection();
        this.channel = connection.createChannel();
    }

    public void respond(String replyQueueName, TResponse response) throws IOException {
        try {
            System.out.println(String.format("Responding on queue %s to message with correlation id %s",
                    replyQueueName, response.getCorrelationId().toString()));

            // Create basic properties for the reply
            var properties = new AMQP.BasicProperties.Builder()
                    .correlationId(response.getCorrelationId().toString())
                            .build();

            // Serialize the response to bytes
            String body = messageSerializer.apply(response);

            // Publish the response
            channel.basicPublish("", replyQueueName, properties, body.getBytes(StandardCharsets.UTF_8));

            System.out.println(String.format("Responded on queue %s at %s", replyQueueName, System.currentTimeMillis()));

        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw e;
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
