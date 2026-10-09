<template>
  <!-- 排行条形列表：用于渠道 / 投放计划归因 -->
  <div class="rank-list">
    <div v-if="!dataLists || dataLists.length === 0" class="no-data">所选日期内没有归因数据</div>
    <div v-for="(item, index) in sortedList" :key="item[nameKey] + index" class="rank-row">
      <div class="rank-meta">
        <span class="rank-index" :class="{ top: index < 3 }">{{ index + 1 }}</span>
        <el-tooltip :content="item[nameKey]" :show-after="500">
          <span class="rank-name" :class="{ unknown: item[nameKey] === 'unknown' }">
            {{ item[nameKey] === 'unknown' ? '未标记' : item[nameKey] }}
          </span>
        </el-tooltip>
        <span class="rank-cnt">{{ item.cnt }} 次</span>
        <span class="rank-ratio">{{ ((item.ratio || 0) * 100).toFixed(1) }}%</span>
      </div>
      <div class="rank-track">
        <div class="rank-fill" :style="{ width: barWidth(item), background: color }"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  dataLists: Array,
  nameKey: {
    type: String,
    default: 'name'
  },
  color: {
    type: String,
    default: '#4f46e5'
  },
  limit: {
    type: Number,
    default: 6
  }
})
const sortedList = computed(() =>
  [...(props.dataLists || [])].sort((a, b) => (b.cnt || 0) - (a.cnt || 0)).slice(0, props.limit)
)
// 条形长度相对第一名，避免占比都很小时整列过短
const maxCnt = computed(() => sortedList.value[0]?.cnt || 1)
const barWidth = (item) => Math.max(((item.cnt || 0) / maxCnt.value) * 100, 2) + '%'
</script>

<style lang="scss" scoped>
.rank-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.no-data {
  padding: 36px 0;
  text-align: center;
  font-size: 13px;
  color: var(--sl-text-3);
}

.rank-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
}

.rank-index {
  display: grid;
  place-items: center;
  flex: none;
  width: 18px;
  height: 18px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 600;
  color: var(--sl-text-3);
  background: #f1f3f8;

  &.top {
    color: var(--sl-text);
    background: #e8eaf3;
  }
}

.rank-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-weight: 500;
  color: var(--sl-text);

  &.unknown {
    color: var(--sl-text-3);
    font-style: italic;
  }
}

.rank-cnt {
  color: var(--sl-text-2);
  font-variant-numeric: tabular-nums;
}

.rank-ratio {
  width: 48px;
  text-align: right;
  color: var(--sl-text-3);
  font-variant-numeric: tabular-nums;
}

.rank-track {
  height: 6px;
  border-radius: 6px;
  background: #f1f3f8;
  overflow: hidden;
}

.rank-fill {
  height: 100%;
  border-radius: 6px;
  transition: width 0.6s cubic-bezier(0.2, 0.8, 0.2, 1);
}
</style>
