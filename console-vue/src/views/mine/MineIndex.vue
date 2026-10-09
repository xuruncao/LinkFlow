<template>
  <div class="account-page">
    <div class="account-inner">
      <div class="page-head">
        <h1 class="page-title">账号设置</h1>
        <p class="page-sub">管理你的个人资料与登录信息</p>
      </div>

      <!-- 资料卡 -->
      <section class="profile-card">
        <div class="profile-cover"></div>
        <div class="profile-body">
          <div class="profile-avatar">{{ (userInfo?.data?.data?.username || '?').charAt(0).toUpperCase() }}</div>
          <div class="profile-ident">
            <div class="profile-name">{{ userInfo?.data?.data?.realName || userInfo?.data?.data?.username }}</div>
            <div class="profile-handle">@{{ userInfo?.data?.data?.username }}</div>
          </div>
          <el-button type="primary" :icon="EditPen" @click="dialogVisible = !dialogVisible">编辑资料</el-button>
        </div>
      </section>

      <!-- 信息列表 -->
      <section class="info-card">
        <div class="info-card-title">基本信息</div>
        <div class="info-row">
          <div class="info-label"><el-icon><User /></el-icon>用户名</div>
          <div class="info-value">{{ userInfo?.data?.data?.username || '—' }}</div>
        </div>
        <div class="info-row">
          <div class="info-label"><el-icon><Postcard /></el-icon>姓名</div>
          <div class="info-value">{{ userInfo?.data?.data?.realName || '—' }}</div>
        </div>
        <div class="info-row">
          <div class="info-label"><el-icon><Iphone /></el-icon>手机号</div>
          <div class="info-value">{{ userInfo?.data?.data?.phone || '—' }}</div>
        </div>
        <div class="info-row">
          <div class="info-label"><el-icon><Message /></el-icon>邮箱</div>
          <div class="info-value">{{ userInfo?.data?.data?.mail || '—' }}</div>
        </div>
      </section>
    </div>
  </div>

  <!-- 修改信息 -->
  <el-dialog v-model="dialogVisible" title="编辑个人资料" width="520px">
    <el-form ref="loginFormRef" :model="userInfoForm" label-position="top" :rules="formRule" class="edit-form">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="userInfoForm.username" maxlength="11" disabled :prefix-icon="User" />
      </el-form-item>
      <div class="two-col">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="userInfoForm.realName" placeholder="请输入姓名" clearable :prefix-icon="Postcard" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userInfoForm.phone" placeholder="请输入手机号" clearable :prefix-icon="Iphone" />
        </el-form-item>
      </div>
      <el-form-item label="邮箱" prop="mail">
        <el-input v-model="userInfoForm.mail" placeholder="请输入邮箱" clearable :prefix-icon="Message" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="userInfoForm.password" type="password" show-password
          placeholder="留空则不修改；如需修改请输入新密码" clearable :prefix-icon="Lock" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="changeUserInfo(loginFormRef)">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { getCurrentInstance, ref, reactive } from 'vue'
import { getUsername } from '@/core/auth'
import { cloneDeep } from 'lodash'
import { ElMessage } from 'element-plus'
import { User, Postcard, Iphone, Message, Lock, EditPen } from '@element-plus/icons-vue'
const loginFormRef = ref()
const { proxy } = getCurrentInstance()
// eslint-disable-next-line no-unused-vars
const API = proxy.$API
const userInfo = ref()
const userInfoForm = ref() // 修改信息
const getUserInfo = async () => {
  const username = getUsername()
  userInfo.value = await API.user.queryUserInfo(username)
  userInfoForm.value = cloneDeep(userInfo.value.data?.data)
  // console.log(userInfoForm.value)
}
getUserInfo()
// 修改信息
const dialogVisible = ref(false)
const formRule = reactive({
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    {
      pattern: /^1[3|5|7|8|9]\d{9}$/,
      message: '请输入正确的手机号',
      trigger: 'blur'
    },
    { min: 11, max: 11, message: '手机号必须是11位', trigger: 'blur' }
  ],
  username: [{ required: true, message: '请输入您的用户名', trigger: 'blur' }],
  mail: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    {
      pattern: /^([a-zA-Z]|[0-9])(\w|\-)+@[a-zA-Z0-9]+\.([a-zA-Z]{2,4})$/,
      message: '请输入正确的邮箱号',
      trigger: 'blur'
    }
  ],
  password: [
    { required: false, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 15, message: '密码长度请在八位以上', trigger: 'blur' }
  ],
  realNamee: [{ required: true, message: '请输姓名', trigger: 'blur' }]
})
const changeUserInfo = (formEl) => {
  if (!formEl) return
  formEl.validate(async (valid) => {
    if (valid) {
      await API.user.editUser(userInfoForm.value).then((res) => {
        if (res?.data?.code !== '0') {
          ElMessage.error(res.data.message)
        } else {
          getUserInfo()
          dialogVisible.value = false
          ElMessage.success('修改成功!')
        }
      })
    } else {
      return false
    }
  })
}
</script>

<style lang="scss" scoped>
.account-page {
  height: 100%;
  overflow-y: auto;
}

.account-inner {
  max-width: 760px;
  margin: 0 auto;
  padding: 32px 24px 48px;
}

.page-head {
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  line-height: 1.3;
  font-weight: 700;
  color: var(--sl-text);
}

.page-sub {
  margin-top: 4px;
  font-size: 13px;
  color: var(--sl-text-3);
}

.profile-card,
.info-card {
  border: 1px solid var(--sl-border);
  border-radius: 16px;
  background: var(--sl-surface);
  box-shadow: var(--sl-shadow);
  overflow: hidden;
}

.profile-cover {
  height: 96px;
  background: linear-gradient(120deg, #4f46e5 0%, #6366f1 45%, #14b8a6 100%);
}

.profile-body {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  padding: 0 24px 20px;
}

.profile-avatar {
  display: grid;
  place-items: center;
  flex: none;
  width: 80px;
  height: 80px;
  margin-top: -36px;
  border: 4px solid #fff;
  border-radius: 22px;
  font-size: 30px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.3);
}

.profile-ident {
  flex: 1;
  min-width: 0;
  padding-bottom: 4px;
}

.profile-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--sl-text);
}

.profile-handle {
  font-size: 13px;
  color: var(--sl-text-3);
}

.info-card {
  margin-top: 16px;
  padding: 8px 24px;
}

.info-card-title {
  padding: 14px 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--sl-text);
}

.info-row {
  display: flex;
  align-items: center;
  min-height: 52px;
  border-top: 1px solid var(--sl-border-soft);
}

.info-label {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 160px;
  color: var(--sl-text-3);

  .el-icon {
    font-size: 16px;
  }
}

.info-value {
  flex: 1;
  font-weight: 500;
  color: var(--sl-text);
}

.edit-form {
  :deep(.el-form-item__label) {
    padding-bottom: 4px;
    font-weight: 500;
  }
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
</style>
