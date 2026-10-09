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

package com.linkflow.shortlink.project.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短链接统计消息发送与补偿配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "short-link.stats.producer")
public class StatsProducerProperties {

    /**
     * 是否使用异步发送。false 时走同步发送（仅用于同代码双跑对照），同步失败同样不阻断跳转
     */
    private boolean asyncEnabled = true;

    /**
     * 异步发送线程池核心线程数
     */
    private int asyncCorePoolSize = 4;

    /**
     * 异步发送线程池最大线程数
     */
    private int asyncMaxPoolSize = 8;

    /**
     * 异步发送等待队列容量，打满后转入补偿队列
     */
    private int asyncQueueCapacity = 2000;

    /**
     * 补偿配置
     */
    private Compensation compensation = new Compensation();

    /**
     * 熔断配置
     * <p>
     * broker 持续不可用时，若继续逐条重试，每一轮重试都会消耗一次网络往返与一次回调，
     * 反而把故障放大成重试风暴。因此连续失败到阈值后直接熔断，期间消息只入补偿队列、
     * 不发起任何发送尝试，冷却结束后再放行探测请求，探测成功才恢复。
     */
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    /**
     * 统计消息失败补偿配置
     */
    @Data
    public static class Compensation {

        /**
         * 是否启用失败补偿
         */
        private boolean enabled = true;

        /**
         * 补偿队列长度上限，超出后丢弃最旧的消息，保证队列不会无限增长
         */
        private long maxSize = 100000L;

        /**
         * 单次 drain 最多重投的消息条数
         */
        private int drainBatchSize = 200;

        /**
         * 单条消息两次重投之间至少间隔的毫秒数，避免同一条消息被连续重投
         */
        private long replayCooldownMillis = 30000L;

        /**
         * drain 间隔，单位毫秒
         */
        private long drainDelayMillis = 5000L;

        /**
         * drain 尝试获取锁的最长等待时间，单位毫秒
         */
        private long drainLockWaitMillis = 100L;
    }

    /**
     * 统计消息发送熔断配置
     */
    @Data
    public static class CircuitBreaker {

        /**
         * 是否启用熔断
         */
        private boolean enabled = true;

        /**
         * 连续失败多少次后熔断
         */
        private int failureThreshold = 20;

        /**
         * 熔断后多少毫秒放行一次探测发送
         */
        private long openMillis = 10000L;
    }
}
