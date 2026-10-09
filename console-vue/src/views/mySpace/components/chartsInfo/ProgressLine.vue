<template>
  <div class="main-box">
    <div v-if="!dataLists || dataLists.length === 0" class="no-data">所选日期内没有访问数据</div>
    <div v-else>
      <div class="flex-box" v-for="(item, index) in dataLists" :key="index">
        <div class="flex-item">
          <div class="item-name">
            <img :src="getUrl(item?.browser, item?.os)" width="18" alt="" />
            <span>{{ item?.browser || item?.os }}</span>
          </div>
          <div class="item-num">
            <span>{{ item?.cnt }} 次</span>
            <em>{{ (item?.ratio * 100).toFixed(1) }}%</em>
          </div>
        </div>
        <el-progress
          color="#4f46e5"
          :show-text="false"
          :stroke-width="6"
          :percentage="Number((item?.ratio * 100).toFixed(2))"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import edge from '@/assets/png/edge.png'
import Andriod from '@/assets/png/Andriod.png'
import Chorme from '@/assets/png/Chorme.png'
import firefox from '@/assets/png/firefox.png'
import iOS from '@/assets/png/iOS.png'
import macOS from '@/assets/png/macOS.png'
import other from '@/assets/png/other.png'
import Safair from '@/assets/png/Safair.png'
import WeChat from '@/assets/png/WeChat.png'
import Windows from '@/assets/png/Windows.png'
import linux from '@/assets/png/linux.png'
import opera from '@/assets/png/opera.png'
import IE from '@/assets/png/IE.png'

defineProps({
  dataLists: {
    type: Array
    // eslint-disable-next-line vue/require-valid-default-prop
    // default: [
    //   {
    //     name: 'windows',
    //     img: null,
    //     times: 2,
    //     percentage: 50
    //   },
    //   {
    //     name: 'linux',
    //     img: null,
    //     times: 2,
    //     percentage: 50
    //   },
    // ]
  }
})
const getUrl = (img1, img2) => {
  if (img1) {
    img1 = img1.toLowerCase()
  }
  if (img2) {
    img2 = img2.toLowerCase()
  }
  if (img1?.includes('edge') || img2?.includes('edge')) {
    return edge
  } else if (img1?.includes('chrome') || img2?.includes('chrome')) {
    return Chorme
  } else if (img1?.includes('android') || img2?.includes('android')) {
    return Andriod
  } else if (img1?.includes('fire') || img2?.includes('fire')) {
    return firefox
  } else if (img1?.includes('ios') || img2?.includes('ios')) {
    return iOS
  } else if (img1?.includes('mac') || img2?.includes('mac')) {
    return macOS
  } else if (img1?.includes('safari') || img2?.includes('safari')) {
    return Safair
  } else if (img1?.includes('windows') || img2?.includes('windows')) {
    return Windows
  } else if (img1?.includes('opera') || img2?.includes('opera')) {
    return opera
  }
   else if (img1?.includes('internet') || img2?.includes('internet')) {
    return IE
  }
  else if (
    img1?.includes('wechat') ||
    img1?.includes('微信') ||
    img2?.includes('wechat') ||
    img2?.includes('微信')
  ) {
    return WeChat
  } else if (img1?.includes('linux') || img2?.includes('linux')) {
    return linux
  } else {
    return other
  }
}
</script>

<style lang="scss" scoped>
.no-data {
  padding: 36px 0;
  text-align: center;
  font-size: 13px;
  color: var(--sl-text-3);
}

.flex-box {
  & + & {
    margin-top: 12px;
  }
}

.flex-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
}

.item-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: var(--sl-text);
}

.item-num {
  display: flex;
  align-items: baseline;
  gap: 8px;
  color: var(--sl-text-2);
  font-variant-numeric: tabular-nums;

  em {
    width: 48px;
    text-align: right;
    font-style: normal;
    color: var(--sl-text-3);
  }
}

:deep(.el-progress-bar__outer) {
  background-color: #f1f3f8;
}
</style>
