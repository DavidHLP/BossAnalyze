<template>
  <div ref="chartRef" style="width: 100%; height: 400px;"></div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
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

const initChart = () => {
  if (chartRef.value && hotCities.value.length > 0) {
    const myChart = echarts.init(chartRef.value);
    const option = {
      title: {
        text: '热门城市招聘数量'
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: hotCities.value.map(item => item.cityName),
        axisLabel: {
          interval: 0, // 显示所有标签
          rotate: 30 // 旋转标签防止重叠
        }
      },
      yAxis: {
        type: 'value',
        name: '职位数量'
      },
      series: [
        {
          name: '职位数量',
          type: 'bar',
          data: hotCities.value.map(item => item.jobCount),
          emphasis: {
            focus: 'series'
          }
        }
      ]
    };
    myChart.setOption(option);

    // 响应式调整图表大小
    window.addEventListener('resize', () => {
      myChart.resize();
    });
  }
};

onMounted(async () => {
  try {
    // 获取热门城市数据，这里限制获取前 20 条
    hotCities.value = await getHotCities(20);
    initChart();
  } catch (error) {
    console.error('获取热门城市数据失败:', error);
  }
});
</script>

<style scoped>
/* 可以添加一些样式 */
</style>
