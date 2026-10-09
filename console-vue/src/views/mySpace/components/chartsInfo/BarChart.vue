<template>
  <!-- 柱状图 -->
  <div ref="barChartRef">
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { onMounted, nextTick, ref, watch } from 'vue'
const props = defineProps({
  chartData: {
    type: Object,
    // eslint-disable-next-line vue/require-valid-default-prop
    default: {
      xAxis: [1, 2, 3, 4, 5, 6, 7],
      value: [12, 12, 32, 32, 23, 23, 16]
    }
  }
})
watch(
  () => props.chartData,
  () => {
    initChart()
  }
)
const barChartRef = ref()
const initChart = () => {
  const barDom = barChartRef.value
  const barChart = echarts.getInstanceByDom(barDom) || echarts.init(barDom)
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15,23,42,0.9)',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 12 },
      axisPointer: {
        type: 'shadow',
        shadowStyle: { color: 'rgba(79,70,229,0.06)' }
      }
    },
    grid: {
      left: '5%',
      right: '5%',
      bottom: '5%',
      top: '13%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#94a3b8'
      },
      data: props.chartData.xAxis
    },
    yAxis: {
      axisLabel: {
        color: '#94a3b8'
      },
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f1f3f8', type: 'dashed' } },
      type: 'value'
    },
    series: [
      {
        data: props.chartData.value,
        type: 'bar',
        barMaxWidth: 16,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#818cf8' },
            { offset: 1, color: '#4f46e5' }
          ]),
          barBorderRadius: [4, 4, 0, 0]
        },
        showBackground: false
      }
    ]
  };
  barChart.setOption(option)
}
onMounted(() => {
  nextTick(() => {
    initChart()
  })
})
</script>

<style lang="scss" scoped>
.barChart {
  width: 100%;
  height: 100%;
}
</style>
