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

package com.linkflow.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linkflow.shortlink.project.dao.entity.LinkChannelStatsDO;
import com.linkflow.shortlink.project.dto.req.ShortLinkGroupStatsReqDTO;
import com.linkflow.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.HashMap;
import java.util.List;

/**
 * 投放渠道统计访问持久层
 * 同一张维度表按 channel 与 campaign 两种粒度聚合，对外暴露两个统计列表
 */
public interface LinkChannelStatsMapper extends BaseMapper<LinkChannelStatsDO> {

    /**
     * 记录渠道访问监控数据
     */
    @Insert("INSERT INTO " +
            "t_link_channel_stats (full_short_url, date, cnt, channel, campaign, create_time, update_time, del_flag) " +
            "VALUES( #{linkChannelStats.fullShortUrl}, #{linkChannelStats.date}, #{linkChannelStats.cnt}, #{linkChannelStats.channel}, #{linkChannelStats.campaign}, NOW(), NOW(), 0) " +
            "ON DUPLICATE KEY UPDATE cnt = cnt +  #{linkChannelStats.cnt};")
    void shortLinkChannelState(@Param("linkChannelStats") LinkChannelStatsDO linkChannelStatsDO);

    /**
     * 根据短链接获取指定日期内渠道监控数据
     */
    @Select("SELECT " +
            "    tlcs.channel, " +
            "    SUM(tlcs.cnt) AS count " +
            "FROM " +
            "    t_link tl INNER JOIN " +
            "    t_link_channel_stats tlcs ON tl.full_short_url = tlcs.full_short_url " +
            "WHERE " +
            "    tlcs.full_short_url = #{param.fullShortUrl} " +
            "    AND tl.gid = #{param.gid} " +
            "    AND tl.del_flag = '0' " +
            "    AND tl.enable_status = #{param.enableStatus} " +
            "    AND tlcs.date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    tlcs.full_short_url, tl.gid, tlcs.channel;")
    List<HashMap<String, Object>> listChannelStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

    /**
     * 根据分组获取指定日期内渠道监控数据
     */
    @Select("SELECT " +
            "    tlcs.channel, " +
            "    SUM(tlcs.cnt) AS count " +
            "FROM " +
            "    t_link tl INNER JOIN " +
            "    t_link_channel_stats tlcs ON tl.full_short_url = tlcs.full_short_url " +
            "WHERE " +
            "    tl.gid = #{param.gid} " +
            "    AND tl.del_flag = '0' " +
            "    AND tl.enable_status = '0' " +
            "    AND tlcs.date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    tl.gid, tlcs.channel;")
    List<HashMap<String, Object>> listChannelStatsByGroup(@Param("param") ShortLinkGroupStatsReqDTO requestParam);

    /**
     * 根据短链接获取指定日期内投放计划监控数据
     */
    @Select("SELECT " +
            "    tlcs.campaign, " +
            "    SUM(tlcs.cnt) AS count " +
            "FROM " +
            "    t_link tl INNER JOIN " +
            "    t_link_channel_stats tlcs ON tl.full_short_url = tlcs.full_short_url " +
            "WHERE " +
            "    tlcs.full_short_url = #{param.fullShortUrl} " +
            "    AND tl.gid = #{param.gid} " +
            "    AND tl.del_flag = '0' " +
            "    AND tl.enable_status = #{param.enableStatus} " +
            "    AND tlcs.date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    tlcs.full_short_url, tl.gid, tlcs.campaign;")
    List<HashMap<String, Object>> listCampaignStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

    /**
     * 根据分组获取指定日期内投放计划监控数据
     */
    @Select("SELECT " +
            "    tlcs.campaign, " +
            "    SUM(tlcs.cnt) AS count " +
            "FROM " +
            "    t_link tl INNER JOIN " +
            "    t_link_channel_stats tlcs ON tl.full_short_url = tlcs.full_short_url " +
            "WHERE " +
            "    tl.gid = #{param.gid} " +
            "    AND tl.del_flag = '0' " +
            "    AND tl.enable_status = '0' " +
            "    AND tlcs.date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    tl.gid, tlcs.campaign;")
    List<HashMap<String, Object>> listCampaignStatsByGroup(@Param("param") ShortLinkGroupStatsReqDTO requestParam);
}
