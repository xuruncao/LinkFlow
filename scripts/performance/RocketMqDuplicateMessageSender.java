import java.nio.charset.StandardCharsets;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;

/**
 * Sends the same short-link statistics payload repeatedly to verify consumer idempotency.
 */
public final class RocketMqDuplicateMessageSender {

    private RocketMqDuplicateMessageSender() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                    "Usage: <namesrv> <topic> <business-key> <count> <full-short-url>");
        }
        String namesrv = args[0];
        String topic = args[1];
        String businessKey = args[2];
        int count = Integer.parseInt(args[3]);
        String fullShortUrl = args[4];
        String body = String.format(
                "{\"fullShortUrl\":\"%s\",\"remoteAddr\":\"127.0.0.1\","
                        + "\"os\":\"Windows\",\"browser\":\"Chrome\",\"device\":\"PC\","
                        + "\"network\":\"WIFI\",\"uv\":\"jmeter-idempotency-uv\","
                        + "\"uvFirstFlag\":false,\"uipFirstFlag\":false,\"keys\":\"%s\","
                        + "\"currentDate\":%d}",
                fullShortUrl, businessKey, System.currentTimeMillis());

        DefaultMQProducer producer = new DefaultMQProducer("short-link-idempotency-test-producer");
        producer.setNamesrvAddr(namesrv);
        producer.setSendMsgTimeout(5000);
        producer.start();
        int successful = 0;
        try {
            for (int i = 0; i < count; i++) {
                Message message = new Message(topic, "idempotency-test", businessKey,
                        body.getBytes(StandardCharsets.UTF_8));
                SendResult result = producer.send(message);
                if (result.getSendStatus() == SendStatus.SEND_OK) {
                    successful++;
                }
            }
        } finally {
            producer.shutdown();
        }
        System.out.printf("requested=%d successful=%d businessKey=%s%n", count, successful, businessKey);
        if (successful != count) {
            throw new IllegalStateException("Not all duplicate messages were sent successfully");
        }
    }
}
