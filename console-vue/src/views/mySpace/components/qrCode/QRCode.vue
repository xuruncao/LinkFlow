<template>
  <el-popover placement="bottom" trigger="click" :width="200">
    <div class="main-box">
      <div class="qr-title">扫码访问</div>
      <canvas ref="canvas"></canvas>
      <div class="qr-url">{{ url }}</div>
      <el-button type="primary" size="small" :icon="Download" @click="download">下载二维码</el-button>
    </div>
    <template #reference>
      <el-tooltip content="二维码" :show-after="300" placement="bottom">
        <button type="button" class="qr-code" aria-label="二维码">
          <el-icon><Grid /></el-icon>
        </button>
      </el-tooltip>
    </template>
  </el-popover>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Grid } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { Download } from '@element-plus/icons-vue'
const props = defineProps({
  url: String
})
const canvas = ref()
// 二维码
const createQrCode = () => {
  QRCode.toCanvas(canvas.value, props.url, { width: 160, margin: 1, color: { dark: '#0f172a', light: '#ffffff' } })
  // QRCode(qrcodeRef.value, {
  //   text: 'http://www.baidu.com',
  //   width: 200,
  //   height: 200,
  // })
}
const download = () => {
  let a = document.createElement('a')
  a.href = canvas.value.toDataURL('image/png');
  a.download = '二维码';
  a.click()
  ElMessage.success('正在下载')
}
onMounted(() => {
  createQrCode()
})
</script>

<style lang="scss" scoped>
.main-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.qr-title {
  align-self: flex-start;
  font-size: 13px;
  font-weight: 600;
  color: var(--sl-text);
}

.qr-url {
  max-width: 100%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-family: var(--sl-mono);
  font-size: 11px;
  color: var(--sl-text-3);
}

.main-box .el-button {
  width: 100%;
}

.qr-code {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: 8px;
  font-size: 16px;
  color: var(--sl-text-2);
  background: transparent;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    color: var(--sl-primary);
    background: var(--sl-primary-soft);
  }
}
</style>
