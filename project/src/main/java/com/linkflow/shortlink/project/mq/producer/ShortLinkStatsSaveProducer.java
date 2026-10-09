/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Modifications (LinkFlow, 2026): package renamed, branding cleanup.
 */

package com.linkflow.shortlink.project.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.linkflow.shortlink.project.config.StatsProducerProperties;
import com.linkflow.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.linkflow.shortlink.project.common.constant.RedisKeyConstant.STATS_COMPENSATION_QUEUE_KEY;

/**
 * 短链接监控状态保存消息队列生产者
 * <p>
 * 跳转是主业务，统计是副业务：本类任何一步失败都不允许影响跳转，因此
 * <ul>
 *     <li>发送改为异步，跳转线程只投递、不等 RocketMQ 应答，不再被 broker 抖动阻塞
 *     （同步发送最坏阻塞 send-message-timeout × 重试次数）；</li>
 *     <li>发送失败进入 Redis 补偿队列，broker 恢复后由 {@code ShortLinkStatsCompensationConsumer} 原样重投。
 *     消息原文中的 keys 即消费端幂等键，重投多次也只会真正落库一次，因此重投是安全的；</li>
 *     <li>异步线程池与队列打满时自定义拒绝策略写入补偿队列，而不是让调用线程自己执行发送
 *     （RocketMQ 客户端异步线程池默认拒绝策略是 CallerRuns，打满时会让跳转线程阻塞，等于异步失效）；</li>
 *     <li>broker 持续不可用时熔断：只入补偿队列、不再发起发送尝试。否则每一次重试都要消耗一次网络往返与回调，
 *     故障会被放大成重试风暴，比丢弃统计更糟。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkStatsSaveProducer {

    private final RocketMQTemplate rocketMQTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final StatsProducerProperties statsProducerProperties;

    @Value("${short-link.stats.mq.topic:short-link-stats}")
    private String statsTopic;

    /**
     * 异步发送线程池：只负责把发送任务投递给 RocketMQ 客户端，真正的网络发送由客户端异步线程完成
     */
    private ExecutorService statsAsyncSendExecutor;

    /**
     * 补偿入队失败（通常是 Redis 同时不可用）次数
     */
    private final AtomicLong compensationFailureCount = new AtomicLong();

    /**
     * 发送失败次数
     */
    private final AtomicLong sendFailureCount = new AtomicLong();

    /**
     * 发送成功次数
     */
    private final AtomicLong sendSuccessCount = new AtomicLong();

    /**
     * 熔断期间未尝试发送、直接进入补偿队列的条数
     */
    private final AtomicLong circuitBreakCount = new AtomicLong();

    /**
     * 连续失败次数
     */
    private final AtomicInteger consecutiveFailureCount = new AtomicInteger();

    /**
     * 熔断是否打开
     */
    private volatile boolean circuitOpen = false;

    /**
     * 熔断打开的时间点
     */
    private volatile long circuitOpenTime = 0L;

    /**
     * 上一次探测发送的时间点，避免探测请求过于频繁
     */
    private volatile long lastProbeTime = 0L;

    /**
     * 日志限流打点，避免 broker 长时间不可用时刷爆日志
     */
    private final AtomicLong lastWarnLogTime = new AtomicLong();

    /**
     * 限流日志最小间隔，单位毫秒
     */
    private static final long WARN_LOG_INTERVAL_MILLIS = 10000L;

    /**
     * 异常信息写入补偿条目的最大长度
     */
    private static final int MAX_ERROR_MESSAGE_LENGTH = 256;

    @PostConstruct
    public void init() {
        int corePoolSize = Math.max(1, statsProducerProperties.getAsyncCorePoolSize());
        int maxPoolSize = Math.max(corePoolSize, statsProducerProperties.getAsyncMaxPoolSize());
        statsAsyncSendExecutor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(Math.max(1, statsProducerProperties.getAsyncQueueCapacity())),
                new StatsAsyncSendThreadFactory(),
                (runnable, executor) -> {
                    // 池与队列都打满：这里只拒绝，绝不占用跳转线程执行发送
                    // 拒绝后由 sendAsync 的 catch 分支把消息转入补偿队列
                    logWarnLimited("统计消息异步线程池已满，转入补偿队列", null);
                    throw new RejectedExecutionException("统计消息异步线程池已满");
                }
        );
        log.info("统计消息异步发送线程池已初始化，core={}，max={}，queue={}",
                corePoolSize, maxPoolSize, statsProducerProperties.getAsyncQueueCapacity());
    }

    /**
     * 发送短链接统计消息
     * <p>
     * 本方法自身不抛出异常：无论发送成功与否，调用方（跳转链路）都必须能继续执行跳转
     */
    public void send(ShortLinkStatsRecordDTO statsRecord) {
        if (statsRecord.getKeys() == null || statsRecord.getKeys().isBlank()) {
            statsRecord.setKeys(UUID.randomUUID().toString());
        }
        String message = JSON.toJSONString(statsRecord);
        if (!statsProducerProperties.isAsyncEnabled()) {
            sendSync(message);
            return;
        }
        sendAsync(message);
    }

    /**
     * 重投补偿队列中的消息
     * <p>
     * 与 {@link #send} 的区别：本方法同步抛出异常，让补偿流程能感知"本次重投没能提交"，从而把条目放回队列退避重试；
     * 提交成功之后的发送结果仍由异步回调处理，回调失败会重新进入补偿队列
     *
     * @param message     消息原文，与首次发送完全一致
     * @param replayTimes 该消息已重投次数，回调再次入队时在其基础上累加
     */
    public void resend(String message, int replayTimes) {
        if (StrUtil.isBlank(message)) {
            return;
        }
        doSend(message, new StatsSendCallback(message, replayTimes));
    }

    /**
     * 熔断是否已打开
     * <p>
     * broker 持续不可用时不再逐条重试，只把消息沉淀到补偿队列，避免重试风暴把故障放大
     */
    private boolean circuitOpen() {
        return circuitOpen;
    }

    /**
     * 获取一次探测许可
     * <p>
     * 只有熔断已打开、距熔断打开已过一个冷却周期、且距上次探测也已过一个冷却周期时才放行一次真实发送尝试。
     * 这两个条件缺一不可：探测失败会重新计时并再次写入 lastProbeTime，因此不会出现"每个 drain 周期都探测一次"
     * 的高频重试；而冷却结束后始终能再有探测机会，不会把校验周期无限拉长
     */
    private boolean tryAcquireProbe() {
        if (!circuitOpen) {
            return false;
        }
        long now = System.currentTimeMillis();
        long openMillis = statsProducerProperties.getCircuitBreaker().getOpenMillis();
        if (now - circuitOpenTime < openMillis || now - lastProbeTime < openMillis) {
            return false;
        }
        lastProbeTime = now;
        log.info("统计消息发送熔断冷却结束，放行一次探测发送，探测成功即恢复");
        return true;
    }

    /**
     * 记录一次发送失败，达到阈值则打开熔断
     */
    private void recordFailure() {
        long failureCount = sendFailureCount.incrementAndGet();
        logWarnLimited("统计消息发送失败，转入补偿队列，累计失败 " + failureCount + " 条", null);
        if (!statsProducerProperties.getCircuitBreaker().isEnabled()) {
            return;
        }
        if (consecutiveFailureCount.incrementAndGet() >= statsProducerProperties.getCircuitBreaker().getFailureThreshold()
                && !circuitOpen) {
            circuitOpen = true;
            circuitOpenTime = System.currentTimeMillis();
            lastProbeTime = circuitOpenTime;
            log.warn("统计消息发送连续失败 {} 次，熔断已打开 {} ms，期间统计消息只入补偿队列不再重试",
                    consecutiveFailureCount.get(), statsProducerProperties.getCircuitBreaker().getOpenMillis());
        }
    }

    /**
     * 记录一次发送成功，连续失败清零并关闭熔断
     */
    private void recordSuccess() {
        sendSuccessCount.incrementAndGet();
        consecutiveFailureCount.set(0);
        if (circuitOpen) {
            circuitOpen = false;
            lastProbeTime = 0L;
            log.info("统计消息发送已恢复，熔断关闭，继续从补偿队列重投历史消息");
        }
    }

    /**
     * 异步发送：投递即返回，不等待 broker 应答
     */
    private void sendAsync(String message) {
        if (circuitOpen() && !tryAcquireProbe()) {
            circuitBreakCount.incrementAndGet();
            pushCompensation(message, "circuit open", 1);
            return;
        }
        try {
            statsAsyncSendExecutor.execute(() -> doSend(message, new StatsSendCallback(message, 0)));
        } catch (Throwable ex) {
            // 线程池拒绝、线程池已关闭等情况：转入补偿队列，保证统计不静默丢失
            logWarnLimited("统计消息投递异步线程池失败，转入补偿队列", ex);
            pushCompensation(message, describeException(ex), 1);
        }
    }

    /**
     * 同步发送：仅用于同代码双跑对照，失败同样只记日志与补偿，不阻断跳转
     */
    private void sendSync(String message) {
        try {
            rocketMQTemplate.syncSend(statsTopic, message);
            recordSuccess();
        } catch (Throwable ex) {
            recordFailure();
            pushCompensation(message, describeException(ex), 1);
        }
    }

    /**
     * 实际发送消息
     *
     * @param message  消息原文
     * @param callback 发送结果回调
     */
    private void doSend(String message, SendCallback callback) {
        rocketMQTemplate.asyncSend(statsTopic, message, callback);
    }

    /**
     * 发送结果回调
     * <p>
     * 回调运行在异步发送线程上，与跳转线程无关；即使这里再出问题，也只记录不抛出
     */
    @Data
    private class StatsSendCallback implements SendCallback {

        private final String message;

        private final int replayTimes;

        @Override
        public void onSuccess(SendResult sendResult) {
            recordSuccess();
        }

        @Override
        public void onException(Throwable ex) {
            recordFailure();
            pushCompensation(message, describeException(ex), replayTimes + 1);
        }
    }

    /**
     * 消息进入补偿队列
     * <p>
     * 队列长度超过上限时丢弃最旧的消息：补偿队列是尽力而为的兜底，不允许无限增长吃满 Redis
     */
    private void pushCompensation(String message, String errorMessage, int replayTimes) {
        if (!statsProducerProperties.getCompensation().isEnabled()) {
            return;
        }
        try {
            CompensationEntry entry = new CompensationEntry(
                    message, errorMessage, replayTimes, statsProducerProperties.getCompensation().getReplayCooldownMillis());
            String queueKey = STATS_COMPENSATION_QUEUE_KEY;
            stringRedisTemplate.opsForList().rightPush(queueKey, JSON.toJSONString(entry));
            stringRedisTemplate.opsForList().trim(queueKey, -statsProducerProperties.getCompensation().getMaxSize(), -1);
        } catch (Throwable ex) {
            long failureCount = compensationFailureCount.incrementAndGet();
            logWarnLimited("统计消息写入补偿队列失败，本条统计降级丢弃，累计 " + failureCount + " 条", ex);
        }
    }

    /**
     * 补偿队列条目
     * <p>
     * 持久化消息原文是为了重投时 keys 保持不变，从而复用消费端已有的幂等能力
     */
    @Data
    public static class CompensationEntry {

        /**
         * 消息原文
         */
        private String message;

        /**
         * 失败原因
         */
        private String errorMessage;

        /**
         * 已重投次数
         */
        private int replayTimes;

        /**
         * 首次进入补偿队列的时间戳
         */
        private long firstCompensationTime;

        /**
         * 最早可重投时间戳，避免同一条消息被连续重投
         */
        private long nextReplayTime;

        public CompensationEntry() {
        }

        public CompensationEntry(String message, String errorMessage, int replayTimes, long replayCooldownMillis) {
            long now = System.currentTimeMillis();
            this.message = message;
            this.errorMessage = errorMessage;
            this.replayTimes = replayTimes;
            this.firstCompensationTime = now;
            // 必须带上冷却时间：否则重投失败的回调会立刻把消息放回队列，被下一个 drain 周期再次重投，
            // 在 broker 持续不可用时形成高频重试（每次重试都要一次网络往返 + 一次回调）
            this.nextReplayTime = now + replayCooldownMillis;
        }
    }

    /**
     * 异常信息摘要
     */
    private String describeException(Throwable ex) {
        String message = ex.getClass().getSimpleName() + ": " + StrUtil.blankToDefault(ex.getMessage(), "");
        return StrUtil.sub(message, 0, MAX_ERROR_MESSAGE_LENGTH);
    }

    /**
     * 限流告警日志：broker 长时间不可用时避免每个请求都打一条堆栈
     */
    private void logWarnLimited(String message, Throwable ex) {
        long now = System.currentTimeMillis();
        long last = lastWarnLogTime.get();
        if (now - last < WARN_LOG_INTERVAL_MILLIS || !lastWarnLogTime.compareAndSet(last, now)) {
            return;
        }
        if (ex == null) {
            log.warn(message);
        } else {
            log.warn("{}，失败原因：{}", message, describeException(ex));
        }
    }

    /**
     * 统计消息异步发送线程工厂
     */
    private static class StatsAsyncSendThreadFactory implements ThreadFactory {

        private final AtomicLong threadIndex = new AtomicLong();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("stats-async-send-" + threadIndex.incrementAndGet());
            thread.setDaemon(Boolean.TRUE);
            return thread;
        }
    }
}
