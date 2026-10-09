# LinkFlow · 控制台（console-vue）

LinkFlow 的 Web 控制台：广告投放短链与点击归因的前端。

- 技术栈：Vue 3 + Vite 4 + Vue Router + Vuex + Element Plus + ECharts（`echarts@4.8`）
- 后端接口：默认通过 `/api` 代理到 `http://127.0.0.1:8000`（网关），见 `vite.config.js`

## 开发

```sh
npm install
npm run dev      # http://127.0.0.1:5173
npm run build    # 产物在 dist/
npm run lint
npm run format
```

本地跑通需要先按根目录 [`README.md`](../README.md) 与 [`LOCAL_DEVELOPMENT.md`](../LOCAL_DEVELOPMENT.md) 起后端（MySQL 3307 / Redis 6379 / RocketMQ 9876 / aggregation 8003 / gateway 8000）。

## 短链域名从哪来

前端「创建短链」「批量创建」用的域名前缀取自 `src/store/index.js` 的 `domain`：

- 本地默认 `127.0.0.1:8003`（与 `aggregation` 的 `server.port` 对齐）；
- 部署到自己的域名后改这一处即可；
- 后端 `short-link.domain.default`（`aggregation/src/main/resources/application.yaml`）需要与之保持一致，否则创建/编辑弹窗里显示的短链域名会和实际跳转域名不一致。

## 目录结构

```
src/
├── api/                 # 接口封装（按模块拆）
├── assets/              # 图片、样式
├── components/          # 通用组件
├── core/                # token / 用户名等会话存储
├── router/              # 路由
├── store/               # Vuex（短链域名等）
├── utils/               # 请求封装等
└── views/
    ├── login/           # 登录 / 注册
    ├── home/            # 首页（导航）
    └── mySpace/         # 分组、短链管理、批量创建、二维码、回收站、统计图表
```

## 说明

本控制台随主仓库一并基于开源项目 [`nageoffer/shortlink`](https://github.com/nageoffer/shortlink)（Apache-2.0）二次开发；来源与改造说明见根目录 [`README.md`](../README.md) 与 [`NOTICE`](../NOTICE)。
