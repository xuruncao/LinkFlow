<template>
  <div class="card">
    <div class="title-box">
      <div class="title-text">
        <span>{{ title }}</span>
        <small v-if="subtitle">{{ subtitle }}</small>
      </div>
      <slot name="titleButton"></slot>
    </div>
    <div class="content-box">
      <slot name="content"></slot>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
defineProps({
  title: {
    type: String,
    default: '默认标题'
  },
  subtitle: {
    type: String,
    default: ''
  }
})
const emits = defineEmits(['onMounted'])
onMounted(() => {
  emits('onMounted')
})
</script>

<style lang="scss" scoped>
.card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 16px 20px 18px;
  border: 1px solid var(--sl-border);
  border-radius: var(--sl-radius);
  background: var(--sl-surface);
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: var(--sl-shadow);
  }
}

.title-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 28px;
  margin-bottom: 12px;
}

.title-text {
  display: flex;
  align-items: baseline;
  gap: 8px;

  span {
    font-size: 15px;
    font-weight: 600;
    color: var(--sl-text);
  }

  small {
    font-size: 12px;
    color: var(--sl-text-3);
  }
}

.content-box {
  flex: 1;
  min-height: 0;
}
</style>
