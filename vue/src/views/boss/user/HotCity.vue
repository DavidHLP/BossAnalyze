<template>
  <div ref="chartRef" class="chart-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import * as echarts from 'echarts/core';
import { BarChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  DatasetComponent,
  TransformComponent
} from 'echarts/components';
import { LabelLayout, UniversalTransition } from 'echarts/features';
import { CanvasRenderer } from 'echarts/renderers';
import { getHotCities } from '@/api/boss/user/user';
import type { HotCity } from '@/api/boss/user/user.d';

// 注册 ECharts 组件
echarts.use([
  TitleComponent,
  TooltipComponent,
  GridComponent,
  DatasetComponent,
  TransformComponent,
  BarChart,
  LabelLayout,
  UniversalTransition,
  CanvasRenderer
]);

const chartRef = ref<HTMLElement | null>(null);
const hotCities = ref<HotCity[]>([]);
const chartInstance = ref<echarts.ECharts | null>(null);

const initChart = () => {
  if (chartRef.value && hotCities.value.length > 0) {
    chartInstance.value = echarts.init(chartRef.value);
    const option = {
      backgroundColor: 'transparent',
      title: {
        show: false
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        },
        formatter: '{b}: {c}个职位'
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        top: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: hotCities.value.map(item => item.cityName),
        axisLabel: {
          interval: 0,
          rotate: 30,
          color: '#999'
        },
        axisLine: {
          lineStyle: {
            color: '#666'
          }
        }
      },
      yAxis: {
        type: 'value',
        name: '职位数量',
        nameTextStyle: {
          color: '#999'
        },
        axisLabel: {
          color: '#999'
        },
        axisLine: {
          lineStyle: {
            color: '#666'
          }
        },
        splitLine: {
          lineStyle: {
            color: 'rgba(255, 255, 255, 0.1)'
          }
        }
      },
      series: [
        {
          name: '职位数量',
          type: 'bar',
          data: hotCities.value.map(item => item.jobCount),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 1, 0, 0, [
              { offset: 0, color: '#3a6073' },
              { offset: 1, color: '#3a7bd5' }
            ])
          },
          barWidth: '60%',
          emphasis: {
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 1, 0, 0, [
                { offset: 0, color: '#2575fc' },
                { offset: 1, color: '#6a11cb' }
              ])
            }
          }
        }
      ]
    };
    chartInstance.value.setOption(option);
  }
};

const handleResize = () => {
  if (chartInstance.value) {
    chartInstance.value.resize();
  }
};

onMounted(async () => {
  try {
    // 获取热门城市数据，这里限制获取前 20 条
    hotCities.value = await getHotCities(20);
    initChart();
    window.addEventListener('resize', handleResize);
  } catch (error) {
    console.error('获取热门城市数据失败:', error);
  }
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (chartInstance.value) {
    chartInstance.value.dispose();
  }
});
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 100%;
  flex: 1;
}
</style>
