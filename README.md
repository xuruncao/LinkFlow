# LinkFlow · 广告投放短链平台

LinkFlow 提供批量建链、分组、有效期与回收站管理，并按投放渠道和投放计划聚合点击数据。统计消息通过 RocketMQ 异步持久化，跳转路径使用有界补偿队列和熔断隔离消息服务故障。

## 来源与维护

由 [xuruncao](https://github.com/xuruncao) 维护，基于 [nageoffer/shortlink](https://github.com/nageoffer/shortlink) 二次开发。上游提供短链系统、管理 API、网关和 Vue 控制台基础；本仓库主要改进统计链路、渠道归因、跳转故障隔离、本地开发环境与控制台界面。来源、许可和修改说明见 [LICENSE](LICENSE)、[NOTICE](NOTICE) 与 [IMPROVEMENTS.md](IMPROVEMENTS.md)。

## 功能与改进

- 批量创建与编辑短链、按分组管理、有效期控制、二维码与回收站。
- 捕获并归一化 `utm_source`、`utm_campaign`，支持单链和分组的渠道／计划榜单；缺失来源显示为 `unknown`。
- RocketMQ 消费端使用 Redis 幂等标记；生产端支持异步发送、失败补偿和熔断。
- Vue 3 控制台提供统一主题、登录／注册界面、工作台概览卡片、统计图表和账号设置。登录页图表标为 DEMO，仅作界面示例。
- Windows 本地开发使用 MySQL 3307、Redis、RocketMQ，`local` profile 不依赖 Docker 或 Nacos。

## 模块与技术栈

| 模块 | 作用 | 本地端口 |
|---|---|---|
| `gateway` | 路由、Token 校验与用户上下文 | 8000 |
| `aggregation` | 聚合运行 admin 与 project | 8003 |
| `admin` | 用户、分组与管理 API | 聚合进程内 |
| `project` | 建链、跳转、访问统计与 MQ | 聚合进程内 |
| `console-vue` | Web 控制台 | 5173 |

后端：JDK 17、Spring Boot 3、MySQL 8、ShardingSphere-JDBC 5.3.2、Redis／Redisson、RocketMQ 5.2.0。前端：Vue 3、Vite 4、Element Plus、ECharts。

## 快速开始

需要 JDK 17、Maven 3.9、Node.js 20、MySQL 8、Redis 和 RocketMQ 5.2.0。当前启动脚本面向 Windows；首次初始化与中间件启动步骤见 [LOCAL_DEVELOPMENT.md](LOCAL_DEVELOPMENT.md)。

```powershell
git clone git@github.com:xuruncao/LinkFlow.git
cd LinkFlow
# JAVA_HOME 指向你安装的 JDK 17；不要使用默认的 JDK 21 构建。
mvn -B package "-DskipTests" "-Dspotless.apply.skip=true"
cd console-vue
npm ci
cd ..
# 先准备本地 MySQL 数据目录并启动 Redis 和 RocketMQ。
.\scripts\start-local.ps1
```

打开 http://127.0.0.1:5173，使用本地开发账号 `admin / admin123456`。默认账号、数据库口令和开发用加密 key 都是本地示例，不用于生产部署；生产构建的登录表单不预填账号密码。

独立开发前端：

```sh
cd console-vue
npm run dev
npm run build
```

### 配置

- 前端 `/api` 代理到网关 8000；短链域名前缀见 `console-vue/src/store/index.js`，需与后端 `short-link.domain.default` 一致。
- 地域查询通过环境变量 `AMAP_KEY` 配置；未配置时不调用地图 API，访问明细地区显示为“未知”，基础统计仍持久化。
- 生产分片配置只提供 `shardingsphere-config-prod.yaml.example` 模板；实际生产配置、口令和加密 key 必须保存在 Git 忽略的私有文件或部署环境中。
- `.local/`、数据库目录、中间件发行包、日志、依赖、构建产物和内部工作记录不会提交。

## 验证与性能边界

历史本地压测：80 并发，预热一次、测量三次取中位数。同步落库与 RocketMQ 请求路径的 QPS 为 49.25 和 2,119.56，P99 为 3,158 ms 和 96 ms。

**这组对比关闭消费端，衡量的是请求路径解耦，不是端到端落库吞吐或生产容量。** 两组服务与压测工具同机；消费幂等和 1,000 条消息落库另做小批量检查。完整参数、汇总与限制见 [RESULTS.md](scripts/performance/RESULTS.md)、[复现步骤](scripts/performance/README.md) 和 [VALIDATION.md](docs/VALIDATION.md)。原始运行数据保留在本地，公开仓库提供汇总与复现脚本。

## 已知限制

- 当前是个人开发与验证项目，不包含租户计费、配额、广告平台回传或生产运维体系。
- 幂等标记有 30 分钟 TTL，不能视为跨任意时间窗口的 exactly-once 保证；异常发生在部分 SQL 写入之后时，重试一致性仍需进一步验证。
- 补偿队列上限 10 万条，超出丢弃最旧条目；Redis 和 broker 同时不可用时可能丢失统计。多实例故障场景尚未验证。
- 访问明细尚未完整透出渠道／计划；日期边界、浏览器兼容性与无障碍仍需完整回归。
- 用户密码继承了上游的明文存储方式，公开部署前应迁移到密码哈希、HTTPS 和独立的生产机密配置，详见 [SECURITY.md](SECURITY.md)。

## 许可

Apache License 2.0。请保留原始来源、[LICENSE](LICENSE) 和 [NOTICE](NOTICE)。
