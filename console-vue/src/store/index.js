import { createStore } from 'vuex'

// 前端展示用的短链域名前缀。
// 本地默认与 aggregation 的 server.port 对齐；部署到自己的域名后改这里，
// 并让后端 short-link.domain.default（aggregation/src/main/resources/application.yaml）保持一致。
const store = createStore({
  state() {
    return {
      domain: '127.0.0.1:8003'
    }
  }
})

export default store
