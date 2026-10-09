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

package com.linkflow.shortlink.project.common.constant;

/**
 * Redis Key 常量类
 */
public class RedisKeyConstant {

    /**
     * 短链接跳转前缀 Key
     */
    public static final String GOTO_SHORT_LINK_KEY = "short-link:goto:%s";

    /**
     * 短链接空值跳转前缀 Key
     */
    public static final String GOTO_IS_NULL_SHORT_LINK_KEY = "short-link:is-null:goto_%s";

    /**
     * 短链接跳转锁前缀 Key
     */
    public static final String LOCK_GOTO_SHORT_LINK_KEY = "short-link:lock:goto:%s";

    /**
     * 短链接修改分组 ID 锁前缀 Key
     */
    public static final String LOCK_GID_UPDATE_KEY = "short-link:lock:update-gid:%s";

    /**
     * 短链接延迟队列消费统计 Key
     */
    public static final String DELAY_QUEUE_STATS_KEY = "short-link:delay-queue:stats";

    /**
     * 短链接统计判断是否新用户缓存标识
     */
    public static final String SHORT_LINK_STATS_UV_KEY = "short-link:stats:uv:";

    /**
     * 短链接统计判断是否新 IP 缓存标识
     */
    public static final String SHORT_LINK_STATS_UIP_KEY = "short-link:stats:uip:";

    /**
     * 创建短链接锁标识
     */
    public static final String SHORT_LINK_CREATE_LOCK_KEY = "short-link:lock:create";

    /**
     * 短链接统计消息补偿队列 Key
     * 异步发送失败或异步线程池打满时，消息原文进入该队列等待重新投递
     */
    public static final String STATS_COMPENSATION_QUEUE_KEY = "short-link:stats:compensation";

    /**
     * 短链接统计消息补偿队列重投锁 Key
     * 多实例部署时保证同一时刻只有一个实例在重投，避免补偿被放大
     */
    public static final String LOCK_STATS_COMPENSATION_DRAIN_KEY = "short-link:lock:stats-compensation-drain";
}
