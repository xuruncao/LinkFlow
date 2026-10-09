# LinkFlow 改造说明

本项目基于 [nageoffer/shortlink](https://github.com/nageoffer/shortlink) 二次开发。基础用户与分组、短链创建／跳转／回收站、分片数据模型、网关和控制台来自上游；以下说明本仓库的增量与限制。

## 1. 统计链路与请求路径解耦

将原实现的 Redis Stream 统计链路改为 RocketMQ；消费端使用 Redis `SETNX` 与 30 分钟 TTL 标记处理中／已完成状态，失败时回删标记允许重试。原有实现的业务重试和死信观测能力有限，独立消息组件提高了积压与重投的可观测性，代价是新增中间件和运维成本。

同代码 `sync / rocketmq` 开关用于历史对照，测量参数与结果见 [RESULTS.md](scripts/performance/RESULTS.md)。消息幂等有时间窗口，且多表写入后的部分失败一致性仍需测试；当前结果不能推导出全局不丢不重或生产最大吞吐。

## 2. 渠道与投放计划归因

跳转时优先读取 `utm_source`，缺失时可回退 Referer 域名；同时处理 `utm_campaign` 与 `click_id`。标识经过长度限制和字符清洗，缺失值统一为 `unknown`，避免唯一索引中的 NULL 导致无法累加。

新增 `t_link_channel_stats` 预聚合表和明细字段，单链／分组 API 返回 `channelStats`、`campaignStats`。控制台用榜单展示渠道和计划分布。来源参数与 Referer 都可能缺失或被客户端伪造，因此它们是描述性归因信号，不构成可信转化证明；也不回传广告平台。

实现入口：[ShortLinkServiceImpl.java](project/src/main/java/com/linkflow/shortlink/project/service/impl/ShortLinkServiceImpl.java)、[LinkChannelStatsMapper.java](project/src/main/java/com/linkflow/shortlink/project/dao/mapper/LinkChannelStatsMapper.java) 与 [link.sql](resources/database/link.sql)。

## 3. 跳转故障隔离

RocketMQ 改造的早期同步提交版本在 broker 不可用时可能返回失败 JSON 而不跳转。当前通过 `submitStatsQuietly`、有界线程池、异步回调、Redis 补偿队列和熔断降低统计故障对跳转的影响。

补偿重投保留原消息业务键，尝试复用消费幂等；队列每 5 秒 drain，重投冷却 30 秒。队列上限 10 万条，连续失败 20 次打开熔断，10 秒后探测。队列溢出与多个依赖同时失效仍有丢数边界。

实现入口：[ShortLinkStatsSaveProducer.java](project/src/main/java/com/linkflow/shortlink/project/mq/producer/ShortLinkStatsSaveProducer.java)、[ShortLinkStatsCompensationConsumer.java](project/src/main/java/com/linkflow/shortlink/project/mq/consumer/ShortLinkStatsCompensationConsumer.java)。历史验证见 [VALIDATION.md](docs/VALIDATION.md)，未重新测量当前界面版本的完整压测。

## 4. 本地开发与控制台

`local` profile 关闭 Nacos 服务发现，网关直连聚合服务；Windows 脚本使用隔离 MySQL 3307 和本地日志目录。公开仓库保留源代码、SQL 结构与复现脚本，不包含数据库、消息存储、发行包、依赖和原始日志。

前端统一为 LinkFlow 主题、登录／注册、顶部导航、工作台概览、统计图表和账号设置，并修复登录失败提示、按钮重试与路由守卫。登录页图表为标注 DEMO 的展示数据，工作台读取后端数据；本地开发账号只在开发模式预填。

Java 命名空间为 `com.linkflow.shortlink`，Maven 和 npm 元数据由当前维护者维护；上游许可和来源声明保留。地图 API key 改由 `AMAP_KEY` 提供，未配置时地域查询降级为未知。

## 发布范围

公开版本从审查后的源码快照开始记录提交。旧开发历史、交接笔记和简历材料保留在维护者本地；[NOTICE](NOTICE) 明确上游来源。维护者不宣称项目基础系统为从零原创，也不将开发机测量解释为生产容量。
