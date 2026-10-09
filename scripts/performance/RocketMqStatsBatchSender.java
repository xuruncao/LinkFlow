import java.nio.charset.StandardCharsets;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;

/** Sends distinct short-link statistics records for consumer drain verification. */
public final class RocketMqStatsBatchSender {

    private RocketMqStatsBatchSender() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                    "Usage: <namesrv> <topic> <batch-prefix> <count> <full-short-url>");
        }
        String namesrv = args[0];
        String topic = args[1];
        String batchPrefix = args[2];
        int count = Integer.parseInt(args[3]);
        String fullShortUrl = args[4];

        DefaultMQProducer producer = new DefaultMQProducer("short-link-stats-drain-test-producer");
        producer.setNamesrvAddr(namesrv);
        producer.setSendMsgTimeout(5000);
        producer.start();
        int successful = 0;
        long startedAt = System.currentTimeMillis();
        try {
            for (int i = 1; i <= count; i++) {
                String businessKey = batchPrefix + "-" + i;
                String body = String.format(
                        "{\"fullShortUrl\":\"%s\",\"remoteAddr\":\"127.0.0.1\","
                                + "\"os\":\"Windows\",\"browser\":\"Chrome\",\"device\":\"PC\","
                                + "\"network\":\"WIFI\",\"uv\":\"%s\","
                                + "\"uvFirstFlag\":false,\"uipFirstFlag\":false,\"keys\":\"%s\","
                                + "\"currentDate\":%d}",
                        fullShortUrl, batchPrefix + "-uv", businessKey, System.currentTimeMillis());
                Message message = new Message(topic, "stats-drain-test", businessKey,
                        body.getBytes(StandardCharsets.UTF_8));
                message.setKeys(businessKey);
                SendResult result = producer.send(message);
                if (result.getSendStatus() == SendStatus.SEND_OK) {
                    successful++;
                }
            }
        } finally {
            producer.shutdown();
        }
        long elapsed = System.currentTimeMillis() - startedAt;
        System.out.printf("requested=%d successful=%d sendElapsedMs=%d batchPrefix=%s%n",
                count, successful, elapsed, batchPrefix);
        if (successful != count) {
            throw new IllegalStateException("Not all statistics messages were sent successfully");
        }
    }
}
