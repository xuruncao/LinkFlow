<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <section class="brand-panel" aria-hidden="true">
      <div class="brand-glow glow-1"></div>
      <div class="brand-glow glow-2"></div>
      <div class="brand-grid"></div>

      <div class="brand-top">
        <div class="logo-mark">
          <el-icon><Link /></el-icon>
        </div>
        <span class="logo-text">LinkFlow</span>
      </div>

      <div class="brand-body">
        <p class="eyebrow">广告投放短链平台</p>
        <h1 class="headline">
          每一次点击，<br />
          都知道它<span class="highlight">从哪里来</span>
        </h1>
        <p class="subline">批量建链、高峰稳定跳转、按投放计划与渠道归因点击，帮你判断预算该投向哪里。</p>

        <!-- 装饰用的数据卡片 -->
        <div class="preview-card">
          <div class="preview-head">
            <span class="dot"></span>
            <span>618 大促 · 点击示例</span>
            <span class="live">DEMO</span>
          </div>
          <div class="preview-bars">
            <span v-for="(h, i) in previewBars" :key="i" :style="{ height: h + '%', animationDelay: i * 60 + 'ms' }"></span>
          </div>
          <div class="preview-channels">
            <div v-for="c in previewChannels" :key="c.name" class="channel-row">
              <span class="channel-name">{{ c.name }}</span>
              <div class="channel-track">
                <div class="channel-fill" :style="{ width: c.ratio + '%', background: c.color }"></div>
              </div>
              <span class="channel-ratio">{{ c.ratio }}%</span>
            </div>
          </div>
        </div>
      </div>

      <ul class="brand-features">
        <li><el-icon><Lightning /></el-icon>高峰点击稳定跳转</li>
        <li><el-icon><DataAnalysis /></el-icon>计划 + 渠道双粒度归因</li>
        <li><el-icon><FolderOpened /></el-icon>分组、有效期、回收站</li>
      </ul>
    </section>

    <!-- 右侧表单区 -->
    <main class="form-panel">
      <div class="form-card">
        <div class="mobile-logo">
          <div class="logo-mark"><el-icon><Link /></el-icon></div>
          <span>LinkFlow</span>
        </div>

        <h2 class="form-title">{{ isLogin ? '欢迎回来' : '创建账号' }}</h2>
        <p class="form-subtitle">{{ isLogin ? '登录后管理你的投放短链' : '几步完成注册，立即开始建链' }}</p>

        <div class="segmented" role="tablist">
          <button type="button" role="tab" :aria-selected="isLogin" :class="{ active: isLogin }"
            @click="!isLogin && changeLogin()">登录</button>
          <button type="button" role="tab" :aria-selected="!isLogin" :class="{ active: !isLogin }"
            @click="isLogin && changeLogin()">注册</button>
          <span class="segmented-thumb" :class="{ right: !isLogin }"></span>
        </div>

        <transition name="fade-slide" mode="out-in">
          <!-- 登录 -->
          <el-form v-if="isLogin" key="login" ref="loginFormRef1" :model="loginForm" :rules="loginFormRule"
            label-position="top" size="large" class="auth-form" @keyup.enter="login(loginFormRef1)">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" maxlength="11" clearable
                :prefix-icon="User" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password
                :prefix-icon="Lock" />
            </el-form-item>
            <div class="form-row">
              <el-checkbox v-model="checked">记住密码</el-checkbox>
            </div>
            <el-button class="submit-btn" type="primary" :loading="loading" @click="login(loginFormRef1)">
              登录
            </el-button>
          </el-form>

          <!-- 注册 -->
          <el-form v-else key="register" ref="loginFormRef2" :model="addForm" :rules="addFormRule"
            label-position="top" size="large" class="auth-form">
            <div class="two-col">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="addForm.username" placeholder="用户名" maxlength="11" clearable
                  :prefix-icon="User" />
              </el-form-item>
              <el-form-item label="姓名" prop="realName">
                <el-input v-model="addForm.realName" placeholder="真实姓名" clearable :prefix-icon="Postcard" />
              </el-form-item>
            </div>
            <el-form-item label="邮箱" prop="mail">
              <el-input v-model="addForm.mail" placeholder="name@company.com" clearable :prefix-icon="Message" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="addForm.phone" placeholder="11 位手机号" clearable :prefix-icon="Iphone" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="addForm.password" type="password" placeholder="8-15 位密码" show-password
                :prefix-icon="Lock" />
            </el-form-item>
            <el-button class="submit-btn" type="primary" :loading="loading" @click="addUser(loginFormRef2)">
              注册并登录
            </el-button>
          </el-form>
        </transition>

        <p class="switch-tip">
          {{ isLogin ? '还没有账号？' : '已有账号？' }}
          <a href="javascript:;" @click="changeLogin">{{ isLogin ? '立即注册' : '去登录' }}</a>
        </p>
      </div>
      <p class="copyright">© LinkFlow · 广告投放短链与点击归因</p>
    </main>
  </div>


</template>

<script setup>
import { setToken, setUsername } from '@/core/auth.js'
import { ref, reactive, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Iphone, Postcard } from '@element-plus/icons-vue'
const { proxy } = getCurrentInstance()
const API = proxy.$API
const loginFormRef1 = ref()
const loginFormRef2 = ref()
const router = useRouter()
const loginForm = reactive({
  username: import.meta.env.DEV ? 'admin' : '',
  password: import.meta.env.DEV ? 'admin123456' : '',
})
const addForm = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  mail: ''
})

const addFormRule = reactive({
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入正确的手机号',
      trigger: 'blur'
    },
    { min: 11, max: 11, message: '手机号必须是11位', trigger: 'blur' }
  ],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 15, message: '密码长度请在八位以上', trigger: 'blur' }
  ],
  mail: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    {
      pattern: /^([a-zA-Z]|[0-9])(\w|\-)+@[a-zA-Z0-9]+\.([a-zA-Z]{2,4})$/,
      message: '请输入正确的邮箱号',
      trigger: 'blur'
    }
  ],
  realName: [
    { required: true, message: '请输姓名', trigger: 'blur' },
  ]
})
const loginFormRule = reactive({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 15, message: '密码长度请在八位以上', trigger: 'blur' }
  ],
})
// 注册
const addUser = (formEl) => {
  if (!formEl) return
  formEl.validate(async (valid) => {
    if (valid) {
      // 检测用户名是否已经存在
      const res1 = await API.user.hasUsername({ username: addForm.username })
      if (res1.data.success !== false) {
        // 注册
        const res2 = await API.user.addUser(addForm)
        // console.log(res2)
        if (res2.data.success === false) {
          ElMessage.warning(res2.data.message)
        } else {
          const res3 = await API.user.login({ username: addForm.username, password: addForm.password })
          const token = res3?.data?.data?.token
          // 将username和token保存到cookies中和localStorage中
          if (token) {
            setToken(token)
            setUsername(addForm.username)
            localStorage.setItem('token', token)
            localStorage.setItem('username', addForm.username)
          }
          ElMessage.success('注册登录成功！')
          router.push('/home')
        }
      } else {
        ElMessage.warning('用户名已存在！')
      }
    } else {
      return false
    }
  })

}
// 登录
const login = async (formEl) => {
  if (!formEl || loading.value) return
  const valid = await formEl.validate().catch(() => false)
  if (!valid || loading.value) return
  loading.value = true
  try {
    const { data } = await API.user.login(loginForm)
    const token = data?.data?.token
    if (data?.code !== '0' || !token) {
      ElMessage.error(data?.message === '用户不存在' ? '请输入正确的账号密码！' : (data?.message || '登录失败，请稍后重试'))
      return
    }
    setToken(token)
    setUsername(loginForm.username)
    localStorage.setItem('token', token)
    localStorage.setItem('username', loginForm.username)
    ElMessage.success('登录成功！')
    await router.push('/home')
  } catch {
    // 请求失败由全局拦截器提示；保持表单可重试。
  } finally {
    loading.value = false
  }
}

const loading = ref(false)
// 是否记住密码
const checked = ref(true)
// 品牌区装饰数据（纯展示）
const previewBars = [38, 52, 44, 68, 59, 82, 74, 96, 88, 70, 62, 78]
const previewChannels = [
  { name: '微信', ratio: 42, color: 'linear-gradient(90deg,#5eead4,#14b8a6)' },
  { name: '抖音', ratio: 31, color: 'linear-gradient(90deg,#a5b4fc,#6366f1)' },
  { name: '短信', ratio: 18, color: 'linear-gradient(90deg,#fcd34d,#f59e0b)' }
]
// 展示登录还是展示注册
const isLogin = ref(true)
const changeLogin = () => {
  isLogin.value = !isLogin.value
}
</script>

<style lang="scss" scoped>
.login-page {
  display: flex;
  width: 100vw;
  min-height: 100vh;
  background: var(--sl-bg);
}

/* ---------------- 品牌区 ---------------- */
.brand-panel {
  position: relative;
  flex: 1.1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 64px;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(145deg, #1e1b4b 0%, #312e81 45%, #4338ca 100%);
}

.brand-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.55;
  pointer-events: none;
}

.glow-1 {
  width: 420px;
  height: 420px;
  top: -120px;
  right: -80px;
  background: #6366f1;
  animation: float 12s ease-in-out infinite;
}

.glow-2 {
  width: 360px;
  height: 360px;
  bottom: -140px;
  left: -60px;
  background: #14b8a6;
  opacity: 0.35;
  animation: float 14s ease-in-out infinite reverse;
}

.brand-grid {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(ellipse at 30% 40%, #000 30%, transparent 75%);
  pointer-events: none;
}

.brand-top,
.brand-body,
.brand-features {
  position: relative;
  z-index: 1;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-mark {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  font-size: 20px;
  color: #fff;
  background: linear-gradient(135deg, #818cf8, #14b8a6);
  box-shadow: 0 6px 18px rgba(20, 184, 166, 0.35);
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.3px;
}

.eyebrow {
  display: inline-block;
  padding: 4px 12px;
  margin-bottom: 20px;
  font-size: 12px;
  letter-spacing: 1px;
  color: #c7d2fe;
  border: 1px solid rgba(199, 210, 254, 0.3);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
}

.headline {
  font-size: 44px;
  line-height: 1.25;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.highlight {
  background: linear-gradient(90deg, #5eead4, #a5b4fc);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.subline {
  max-width: 460px;
  margin-top: 18px;
  font-size: 15px;
  line-height: 1.8;
  color: rgba(224, 231, 255, 0.78);
}

.preview-card {
  width: 420px;
  margin-top: 40px;
  padding: 18px 20px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(12px);
  box-shadow: 0 20px 50px rgba(15, 12, 60, 0.35);
}

.preview-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #e0e7ff;

  .dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #5eead4;
    box-shadow: 0 0 0 4px rgba(94, 234, 212, 0.2);
  }

  .live {
    margin-left: auto;
    padding: 1px 8px;
    font-size: 11px;
    font-weight: 600;
    border-radius: 6px;
    color: #5eead4;
    background: rgba(94, 234, 212, 0.12);
  }
}

.preview-bars {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 72px;
  margin: 16px 0 14px;

  span {
    flex: 1;
    border-radius: 4px 4px 2px 2px;
    background: linear-gradient(180deg, #a5b4fc, rgba(129, 140, 248, 0.35));
    transform-origin: bottom;
    animation: grow 0.8s cubic-bezier(0.2, 0.8, 0.2, 1) both;
  }
}

.channel-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #c7d2fe;

  & + & {
    margin-top: 8px;
  }
}

.channel-name {
  width: 52px;
}

.channel-track {
  flex: 1;
  height: 6px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

.channel-fill {
  height: 100%;
  border-radius: 6px;
}

.channel-ratio {
  width: 36px;
  text-align: right;
  font-family: var(--sl-mono);
}

.brand-features {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  font-size: 13px;
  color: rgba(224, 231, 255, 0.82);

  li {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .el-icon {
    color: #5eead4;
    font-size: 16px;
  }
}

/* ---------------- 表单区 ---------------- */
.form-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

.form-card {
  width: 100%;
  max-width: 420px;
  animation: rise 0.5s cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

.mobile-logo {
  display: none;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
  font-size: 18px;
  font-weight: 700;
}

.form-title {
  font-size: 28px;
  line-height: 1.3;
  font-weight: 700;
  color: var(--sl-text);
}

.form-subtitle {
  margin-top: 6px;
  color: var(--sl-text-3);
}

.segmented {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin: 28px 0 24px;
  padding: 4px;
  border-radius: 10px;
  background: #eceef5;

  button {
    position: relative;
    z-index: 1;
    height: 36px;
    border: 0;
    border-radius: 8px;
    background: transparent;
    font-weight: 500;
    color: var(--sl-text-2);
    transition: color 0.2s;

    &.active {
      color: var(--sl-text);
      font-weight: 600;
    }
  }
}

.segmented-thumb {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: 36px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.1);
  transition: transform 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);

  &.right {
    transform: translateX(100%);
  }
}

.auth-form {
  :deep(.el-form-item__label) {
    padding-bottom: 4px;
    font-weight: 500;
    color: var(--sl-text-2);
  }

  :deep(.el-form-item) {
    margin-bottom: 18px;
  }
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-row {
  display: flex;
  justify-content: space-between;
  margin: -4px 0 18px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  border-radius: 10px;
}

.switch-tip {
  margin-top: 22px;
  text-align: center;
  color: var(--sl-text-3);

  a {
    color: var(--sl-primary);
    font-weight: 500;
  }
}

.copyright {
  margin-top: 48px;
  font-size: 12px;
  color: var(--sl-text-3);
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.25s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@keyframes float {
  0%,
  100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(30px, 20px);
  }
}

@keyframes grow {
  from {
    transform: scaleY(0);
  }
  to {
    transform: scaleY(1);
  }
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .glow-1,
  .glow-2,
  .preview-bars span,
  .form-card {
    animation: none;
  }
}

@media (max-width: 960px) {
  .brand-panel {
    display: none;
  }

  .mobile-logo {
    display: flex;
  }
}
</style>
