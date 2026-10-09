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
 * Modifications (LinkFlow, 2026): package rename, MQ statistics and optional location lookup.
 */

package com.linkflow.shortlink.project.mq.consumer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.linkflow.shortlink.project.common.convention.exception.ServiceException;
import com.linkflow.shortlink.project.dao.entity.LinkAccessLogsDO;
import com.linkflow.shortlink.project.dao.entity.LinkAccessStatsDO;
import com.linkflow.shortlink.project.dao.entity.LinkBrowserStatsDO;
import com.linkflow.shortlink.project.dao.entity.LinkChannelStatsDO;
import com.linkflow.shortlink.project.dao.entity.LinkDeviceStatsDO;
import com.linkflow.shortlink.project.dao.entity.LinkLocaleStatsDO;
import com.linkflow.shortlink.project.dao.entity.LinkNetworkStatsDO;
import com.linkflow.shortlink.project.dao.entity.LinkOsStatsDO;
import com.linkflow.shortlink.project.dao.entity.LinkStatsTodayDO;
import com.linkflow.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.linkflow.shortlink.project.dao.mapper.LinkAccessLogsMapper;
import com.linkflow.shortlink.project.dao.mapper.LinkAccessStatsMapper;
import com.linkflow.shortlink.project.dao.mapper.LinkBrowserStatsMapper;
import com.linkflow.shortlink.project.dao.mapper.LinkChannelStatsMapper;
import com.linkflow.shortlink.project.dao.mapper.LinkDeviceStatsMapper;
import com.linkflow.shortlink.project.dao.mapper.LinkLocaleStatsMapper;
import com.linkflow.shortlink.project.dao.mapper.LinkNetworkStatsMapper;
import com.linkflow.shortlink.project.dao.mapper.LinkOsStatsMapper;
import com.linkflow.shortlink.project.dao.mapper.LinkStatsTodayMapper;
import com.linkflow.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.linkflow.shortlink.project.dao.mapper.ShortLinkMapper;
import com.linkflow.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.linkflow.shortlink.project.mq.idempotent.MessageQueueIdempotentHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.linkflow.shortlink.project.common.constant.RedisKeyConstant.LOCK_GID_UPDATE_KEY;
import static com.linkflow.shortlink.project.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;
import static com.linkflow.shortlink.project.common.constant.ShortLinkConstant.UNKNOWN_CHANNEL;

/**
 * 短链接监控状态保存消息队列消费者
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "short-link.stats.consumer.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "${short-link.stats.mq.topic:short-link-stats}",
        consumerGroup = "${short-link.stats.mq.consumer-group:short-link-stats-consumer}",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING,
        consumeThreadNumber = 8,
        consumeThreadMax = 8,
        maxReconsumeTimes = 5,
        consumeTimeout = 5,
        awaitTerminationMillisWhenShutdown = 10000
)
public class ShortLinkStatsSaveConsumer implements RocketMQListener<String> {

    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final RedissonClient redissonClient;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkChannelStatsMapper linkChannelStatsMapper;
    private final LinkStatsTodayMapper linkStatsTodayMapper;
    private final MessageQueueIdempotentHandler messageQueueIdempotentHandler;

    @Value("${short-link.stats.locale.amap-key}")
    private String statsLocaleAmapKey;

    @Value("${short-link.stats.locale.mock-enabled:false}")
    private boolean statsLocaleMockEnabled;

    @Override
    public void onMessage(String message) {
        ShortLinkStatsRecordDTO statsRecord = JSON.parseObject(message, ShortLinkStatsRecordDTO.class);
        String messageId = statsRecord.getKeys();
        if (messageId == null || messageId.isBlank()) {
            throw new ServiceException("短链接统计消息缺少唯一标识");
        }
        if (messageQueueIdempotentHandler.isMessageBeingConsumed(messageId)) {
            // 判断当前的这个消息流程是否执行完成
            if (messageQueueIdempotentHandler.isAccomplish(messageId)) {
                return;
            }
            throw new ServiceException("消息未完成流程，需要消息队列重试");
        }
        try {
            actualSaveShortLinkStats(statsRecord);
            messageQueueIdempotentHandler.setAccomplish(messageId);
        } catch (Throwable ex) {
            messageQueueIdempotentHandler.delMessageProcessed(messageId);
            log.error("记录短链接监控消费异常", ex);
            throw ex;
        }
    }

    public void actualSaveShortLinkStats(ShortLinkStatsRecordDTO statsRecord) {
        String fullShortUrl = statsRecord.getFullShortUrl();
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl));
        RLock rLock = readWriteLock.readLock();
        rLock.lock();
        try {
            LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
            String gid = shortLinkGotoDO.getGid();
            Date currentDate = statsRecord.getCurrentDate();
            // 渠道与计划标识缺失时归一为 unknown：维度表唯一键包含这两列，
            // 而 MySQL 唯一索引把 NULL 视为互不相同，直接写 NULL 会累积重复行而不是累加
            String channel = StrUtil.isBlank(statsRecord.getChannel()) ? UNKNOWN_CHANNEL : statsRecord.getChannel();
            String campaign = StrUtil.isBlank(statsRecord.getCampaign()) ? UNKNOWN_CHANNEL : statsRecord.getCampaign();
            int hour = DateUtil.hour(currentDate, true);
            Week week = DateUtil.dayOfWeekEnum(currentDate);
            int weekValue = week.getIso8601Value();
            LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                    .pv(1)
                    .uv(statsRecord.getUvFirstFlag() ? 1 : 0)
                    .uip(statsRecord.getUipFirstFlag() ? 1 : 0)
                    .hour(hour)
                    .weekday(weekValue)
                    .fullShortUrl(fullShortUrl)
                    .date(currentDate)
                    .build();
            linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
            Map<String, Object> localeParamMap = new HashMap<>();
            localeParamMap.put("key", statsLocaleAmapKey);
            localeParamMap.put("ip", statsRecord.getRemoteAddr());
            String localeResultStr = statsLocaleMockEnabled
                    ? "{\"infocode\":\"10000\",\"province\":\"上海市\",\"city\":\"上海市\",\"adcode\":\"310000\"}"
                    : (StrUtil.isBlank(statsLocaleAmapKey)
                    ? "{\"infocode\":\"10001\"}"
                    : HttpUtil.get(AMAP_REMOTE_URL, localeParamMap));
            JSONObject localeResultObj = JSON.parseObject(localeResultStr);
            String infoCode = localeResultObj.getString("infocode");
            String actualProvince = "未知";
            String actualCity = "未知";
            if (StrUtil.isNotBlank(infoCode) && StrUtil.equals(infoCode, "10000")) {
                String province = localeResultObj.getString("province");
                boolean unknownFlag = StrUtil.equals(province, "[]");
                LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                        .province(actualProvince = unknownFlag ? actualProvince : province)
                        .city(actualCity = unknownFlag ? actualCity : localeResultObj.getString("city"))
                        .adcode(unknownFlag ? "未知" : localeResultObj.getString("adcode"))
                        .cnt(1)
                        .fullShortUrl(fullShortUrl)
                        .country("中国")
                        .date(currentDate)
                        .build();
                linkLocaleStatsMapper.shortLinkLocaleState(linkLocaleStatsDO);
            }
            LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                    .os(statsRecord.getOs())
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .date(currentDate)
                    .build();
            linkOsStatsMapper.shortLinkOsState(linkOsStatsDO);
            LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                    .browser(statsRecord.getBrowser())
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .date(currentDate)
                    .build();
            linkBrowserStatsMapper.shortLinkBrowserState(linkBrowserStatsDO);
            LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                    .device(statsRecord.getDevice())
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .date(currentDate)
                    .build();
            linkDeviceStatsMapper.shortLinkDeviceState(linkDeviceStatsDO);
            LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                    .network(statsRecord.getNetwork())
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .date(currentDate)
                    .build();
            linkNetworkStatsMapper.shortLinkNetworkState(linkNetworkStatsDO);
            LinkChannelStatsDO linkChannelStatsDO = LinkChannelStatsDO.builder()
                    .channel(channel)
                    .campaign(campaign)
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .date(currentDate)
                    .build();
            linkChannelStatsMapper.shortLinkChannelState(linkChannelStatsDO);
            LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                    .user(statsRecord.getUv())
                    .ip(statsRecord.getRemoteAddr())
                    .browser(statsRecord.getBrowser())
                    .os(statsRecord.getOs())
                    .network(statsRecord.getNetwork())
                    .device(statsRecord.getDevice())
                    .channel(channel)
                    .campaign(campaign)
                    .locale(StrUtil.join("-", "中国", actualProvince, actualCity))
                    .fullShortUrl(fullShortUrl)
                    .build();
            linkAccessLogsMapper.insert(linkAccessLogsDO);
            shortLinkMapper.incrementStats(gid, fullShortUrl, 1, statsRecord.getUvFirstFlag() ? 1 : 0, statsRecord.getUipFirstFlag() ? 1 : 0);
            LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
                    .todayPv(1)
                    .todayUv(statsRecord.getUvFirstFlag() ? 1 : 0)
                    .todayUip(statsRecord.getUipFirstFlag() ? 1 : 0)
                    .fullShortUrl(fullShortUrl)
                    .date(currentDate)
                    .build();
            linkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);
        } finally {
            rLock.unlock();
        }
    }
}
