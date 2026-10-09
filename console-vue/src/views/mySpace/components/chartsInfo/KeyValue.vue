<template>
  <div class="main">
    <div v-if="dataLists.length === 0" class="no-data">所选日期内没有访问数据</div>
    <div class="flex-box">
      <div class="flex-item" v-for="(item, index) in dataLists" :key="index">
        <span class="rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
        <span class="key-text">{{ item.ip }}</span>
        <span class="cnt">{{ item.cnt }} 次</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  dataLists: {
    type: Array,
    // eslint-disable-next-line vue/require-valid-default-prop
    default: []
  }
})
</script>

<style lang="scss" scoped>
.no-data {
  padding: 36px 0;
  text-align: center;
  font-size: 13px;
  color: var(--sl-text-3);
}

.flex-box {
  display: flex;
  flex-direction: column;
}

.flex-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 36px;
  padding: 0 4px;
  border-bottom: 1px dashed var(--sl-border-soft);

  &:last-child {
    border-bottom: 0;
  }
}

.rank {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 600;
  color: var(--sl-text-3);
  background: #f1f3f8;

  &.top {
    color: #fff;
    background: var(--sl-primary);
  }
}

.key-text {
  flex: 1;
  font-family: var(--sl-mono);
  font-size: 13px;
  color: var(--sl-text);
}

.cnt {
  font-size: 13px;
  color: var(--sl-text-2);
  font-variant-numeric: tabular-nums;
}
</style>
