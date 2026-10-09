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

package com.linkflow.shortlink.project.mq.consumer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.linkflow.shortlink.project.config.StatsProducerProperties;
import com.linkflow.shortlink.project.config.StatsProducerProperties.Compensation;
import com.linkflow.shortlink.project.mq.producer.ShortLinkStatsSaveProducer;
import com.linkflow.shortlink.project.mq.producer.ShortLinkStatsSaveProducer.CompensationEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static com.linkflow.shortlink.project.common.constant.RedisKeyConstant.LOCK_STATS_COMPENSATION_DRAIN_KEY;
import static com.linkflow.shortlink.project.common.constant.RedisKeyConstant.STATS_COMPENSATION_QUEUE_KEY;

/**
 * 短链接统计消息补偿消费者
 * <p>
 * 定时把发送失败、暂存在 Redis 中的统计消息原样重投给消息队列。
 * 重投使用的是同一条消息原文（keys 不变），消费端已有的 SETNX 幂等能力会拦住重复消费，
 * 因此重投不会导致统计被重复累加，这也是本补偿方案安全的前提。
 * <p>
 * 两个约束保证补偿不会在 broker 长时间不可用时变成重试风暴：
 * <ul>
 *     <li>单条消息两次重投之间至少间隔 {@code replay-cooldown-millis}，且本轮取出的消息在本轮内不再重复处理；</li>
 *     <li>broker 连续失败到阈值后由生产者熔断，重投直接失败，消息原地放回队列等冷却结束，不再发起网络请求。</li>
 * </ul>
 * 多实例部署时通过分布式锁保证同一时刻只有一个实例在重投，避免补偿被放大。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkStatsCompensationConsumer {

    private final ShortLinkStatsSaveProducer shortLinkStatsSaveProducer;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final StatsProducerProperties statsProducerProperties;

    /**
     * 定时重投补偿队列中的统计消息
     * <p>
     * 本方法不抛出异常：补偿是后台尽力而为的动作，任何异常都不应该影响调度线程
     */
    @Scheduled(fixedDelayString = "${short-link.stats.producer.compensation.drain-delay-millis:5000}")
    public void drain() {
        Compensation compensation = statsProducerProperties.getCompensation();
        if (!compensation.isEnabled()) {
            return;
        }
        RLock lock = redissonClient.getLock(LOCK_STATS_COMPENSATION_DRAIN_KEY);
        boolean locked = false;
        try {
            locked = lock.tryLock(compensation.getDrainLockWaitMillis(), TimeUnit.MILLISECONDS);
        } catch (Throwable ex) {
            log.warn("统计消息补偿队列获取重投锁失败，本轮跳过", ex);
            return;
        }
        if (!locked) {
            // 其他实例正在重投，本轮跳过
            return;
        }
        try {
            int replayCount = 0;
            int scannedCount = 0;
            int maxScannedCount = Math.max(1, compensation.getDrainBatchSize());
            long now = System.currentTimeMillis();
            while (scannedCount < maxScannedCount) {
                // 本轮取出的消息一律放到队尾（重投或延后），因此不会在本轮被再次取出
                String entryJson = stringRedisTemplate.opsForList().leftPop(STATS_COMPENSATION_QUEUE_KEY);
                if (StrUtil.isBlank(entryJson)) {
                    break;
                }
                scannedCount++;
                if (handleEntry(entryJson, compensation, now)) {
                    replayCount++;
                }
            }
            if (replayCount > 0) {
                log.info("统计消息补偿队列重投完成，本轮重投 {} 条，扫描 {} 条，剩余 {} 条",
                        replayCount, scannedCount, stringRedisTemplate.opsForList().size(STATS_COMPENSATION_QUEUE_KEY));
            }
            if (replayCount == 0 && scannedCount > 0) {
                // 全部处于冷却期（通常是 broker 熔断中）：消息都还在队列里，不构成重试风暴
                log.debug("统计消息补偿队列本轮无消息可重投，扫描 {} 条，均处于冷却期，剩余 {} 条",
                        scannedCount, stringRedisTemplate.opsForList().size(STATS_COMPENSATION_QUEUE_KEY));
            }
        } catch (Throwable ex) {
            // 通常是 Redis 不可用：本轮剩余消息留在队列里，下个周期再试
            log.warn("统计消息补偿队列重投异常，本轮剩余消息留待下个周期重试", ex);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 处理单条补偿消息
     *
     * @return 本次是否真正发起过重投
     */
    private boolean handleEntry(String entryJson, Compensation compensation, long now) {
        CompensationEntry entry;
        try {
            entry = JSON.parseObject(entryJson, CompensationEntry.class);
        } catch (Throwable ex) {
            log.warn("统计消息补偿条目解析失败，丢弃该条目：{}", StrUtil.sub(entryJson, 0, 256), ex);
            return false;
        }
        if (entry == null || StrUtil.isBlank(entry.getMessage())) {
            log.warn("统计消息补偿条目内容为空，丢弃该条目");
            return false;
        }
        if (entry.getNextReplayTime() > now) {
            // 仍在冷却期内：原样放回队尾，等下一轮或更晚再试
            stringRedisTemplate.opsForList().rightPush(STATS_COMPENSATION_QUEUE_KEY, entryJson);
            return false;
        }
        try {
            shortLinkStatsSaveProducer.resend(entry.getMessage(), entry.getReplayTimes());
            // 重投已提交：提交结果由发送回调决定，回调失败会重新进入补偿队列
            return true;
        } catch (Throwable ex) {
            // broker 不可用或熔断：消息放回队尾并设置冷却，避免每个周期都全量重投
            entry.setReplayTimes(entry.getReplayTimes() + 1);
            entry.setErrorMessage(StrUtil.sub(ex.getClass().getSimpleName(), 0, 256));
            entry.setNextReplayTime(System.currentTimeMillis() + compensation.getReplayCooldownMillis());
            stringRedisTemplate.opsForList().rightPush(STATS_COMPENSATION_QUEUE_KEY, JSON.toJSONString(entry));
            return false;
        }
    }
}
