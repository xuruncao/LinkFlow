<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="header-left">
        <button type="button" class="brand" @click="toMySpace">
          <span class="logo-mark"><el-icon><Link /></el-icon></span>
          <span class="brand-name">LinkFlow</span>
          <span class="brand-tag">投放短链</span>
        </button>

        <nav class="main-nav" aria-label="主导航">
          <router-link to="/home/space" class="nav-item" active-class="active">
            <el-icon><Grid /></el-icon>投放链接
          </router-link>
          <router-link to="/home/account" class="nav-item" active-class="active">
            <el-icon><User /></el-icon>账号设置
          </router-link>
        </nav>
      </div>

      <div class="header-right">
        <el-dropdown trigger="click" placement="bottom-end">
          <button type="button" class="user-chip">
            <span class="avatar" :style="{ background: avatarColor }">{{ avatarText }}</span>
            <span class="user-name">{{ username }}</span>
            <el-icon class="caret"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item :icon="User" @click="toMine">个人信息</el-dropdown-item>
              <el-dropdown-item divided :icon="SwitchButton" @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="app-main">
      <RouterView v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </RouterView>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance, onMounted } from 'vue'
import { User, SwitchButton } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { removeKey, removeUsername, getToken, getUsername } from '@/core/auth.js'
import { ElMessage } from 'element-plus'
const { proxy } = getCurrentInstance()
const API = proxy.$API
// 当当前路径和菜单不匹配时，菜单不会被选中
const router = useRouter()
const toMine = () => {
  router.push('/home' + '/account')
}
// 登出
const logout = async () => {
  const token = getToken()
  const username = getUsername()
  // 请求登出的接口
  await API.user.logout({ token, username })
  // 删除cookies中的token和username
  removeUsername()
  removeKey()
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
  ElMessage.success('成功退出！')
}
// 点击左上方的图片跳转到我的空间
const toMySpace = () => {
  router.push('/home' + '/space')
}
const username = ref('')
onMounted(async () => {
  const actualUsername = getUsername()
  const res = await API.user.queryUserInfo(actualUsername)
  // firstName.value = res?.data?.data?.realName?.split('')[0]
  username.value = truncateText(actualUsername, 8)
})
// 头像：取用户名首字符，并按用户名稳定地挑一个渐变色
const avatarPalette = [
  'linear-gradient(135deg,#6366f1,#8b5cf6)',
  'linear-gradient(135deg,#14b8a6,#0ea5e9)',
  'linear-gradient(135deg,#f59e0b,#ef4444)',
  'linear-gradient(135deg,#ec4899,#8b5cf6)',
  'linear-gradient(135deg,#22c55e,#14b8a6)'
]
const avatarText = computed(() => (username.value || '?').charAt(0).toUpperCase())
const avatarColor = computed(() => {
  const name = username.value || ''
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = (hash + name.charCodeAt(i)) % avatarPalette.length
  return avatarPalette[hash]
})

// 辅助函数，用于截断文本
const truncateText = (text, maxLength) => {
  return text.length > maxLength ? text.slice(0, maxLength) + '...' : text
}
</script>

<style lang="scss" scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--sl-bg);
}

.app-header {
  position: relative;
  z-index: 10;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--sl-header-h);
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid var(--sl-border);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 36px;
  height: 100%;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0;
  border: 0;
  background: none;
}

.logo-mark {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  font-size: 18px;
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #14b8a6);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.28);
}

.brand-name {
  font-size: 17px;
  font-weight: 700;
  color: var(--sl-text);
  letter-spacing: 0.2px;
}

.brand-tag {
  padding: 1px 8px;
  font-size: 11px;
  font-weight: 500;
  color: var(--sl-primary);
  border-radius: 999px;
  background: var(--sl-primary-soft);
}

.main-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 100%;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border-radius: 8px;
  font-weight: 500;
  color: var(--sl-text-2);
  transition: all 0.2s;

  &:hover {
    color: var(--sl-text);
    background: #f1f3f9;
  }

  &.active {
    color: var(--sl-primary);
    background: var(--sl-primary-soft);
  }
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 10px 0 4px;
  border: 1px solid var(--sl-border);
  border-radius: 999px;
  background: #fff;
  transition: all 0.2s;

  &:hover {
    border-color: var(--el-color-primary-light-7);
    box-shadow: var(--sl-shadow);
  }
}

.avatar {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
}

.user-name {
  max-width: 120px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-weight: 500;
  color: var(--sl-text);
}

.caret {
  color: var(--sl-text-3);
  font-size: 12px;
}

.app-main {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.page-enter-active,
.page-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.page-leave-to {
  opacity: 0;
}

:deep(.el-tooltip__trigger:focus-visible) {
  outline: unset;
}
</style>
