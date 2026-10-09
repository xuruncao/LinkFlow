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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 短链接统计消息补偿调度配置
 * <p>
 * 跳转是主业务，统计是副业务。统计消息的发送线程池必须有明确边界：
 * 池与队列打满时不能把发送压回跳转线程（RocketMQ 客户端异步线程池默认拒绝策略是 CallerRuns，
 * 打满时会让调用线程自己执行发送，等于异步失效），而是直接转入补偿队列等待重投。
 * 补偿重投则由本配置提供的单线程调度器驱动。
 */
@Configuration
@EnableScheduling
public class StatsCompensationConfiguration {

    /**
     * 补偿队列重投使用的调度线程池
     * <p>
     * 单线程即可：重投是后台补偿动作，不需要并发抢占跳转与消费端的资源。
     * 该 Bean 是应用中唯一暴露的 {@link org.springframework.scheduling.TaskScheduler}，
     * 因此 {@code @Scheduled} 会直接使用它（Spring 6.0 的 {@code @Scheduled} 不支持指定调度器名）
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("stats-compensation-scheduler-");
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.setAwaitTerminationSeconds(10);
        return taskScheduler;
    }
}
