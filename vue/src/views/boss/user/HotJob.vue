<template>
  <div class="chart-container">
    <div id="hotJobChart" class="chart"></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import * as echarts from 'echarts';
import { getHotJobs } from '@/api/boss/user/user';

const chartInstance = ref<echarts.ECharts | null>(null);
const loading = ref(true);

const initChart = () => {
  const chartDom = document.getElementById('hotJobChart');
  if (!chartDom) return;

  chartInstance.value = echarts.init(chartDom);
  chartInstance.value.showLoading();

  // 获取热门职位数据
  getHotJobs(20).then(data => {
    loading.value = false;
    if (chartInstance.value) {
      chartInstance.value.hideLoading();

      // 数据处理
      const positionNames = data.map(item => item.positionName);
      const positionCounts = data.map(item => item.positionCount);

      // 配置图表
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
          type: 'value',
          boundaryGap: [0, 0.01],
          axisLine: {
            lineStyle: {
              color: '#666'
            }
          },
          splitLine: {
            show: true,
            lineStyle: {
              color: 'rgba(255, 255, 255, 0.1)'
            }
          },
          axisLabel: {
            color: '#999'
          }
        },
        yAxis: {
          type: 'category',
          data: positionNames.reverse(),
          axisLabel: {
            interval: 0,
            color: '#999'
          },
          axisLine: {
            lineStyle: {
              color: '#666'
            }
          }
        },
        series: [
          {
            name: '职位数量',
            type: 'bar',
            data: positionCounts.reverse(),
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: '#3a7bd5' },
                { offset: 1, color: '#3a6073' }
              ])
            },
            barWidth: '60%',
            emphasis: {
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                  { offset: 0, color: '#6a11cb' },
                  { offset: 1, color: '#2575fc' }
                ])
              }
            }
          }
        ]
      };

      chartInstance.value.setOption(option);
    }
  }).catch(error => {
    console.error('获取热门职位数据失败:', error);
    loading.value = false;
    if (chartInstance.value) {
      chartInstance.value.hideLoading();
    }
  });
};

const handleResize = () => {
  if (chartInstance.value) {
    chartInstance.value.resize();
  }
};

onMounted(() => {
  initChart();
  window.addEventListener('resize', handleResize);
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
  display: flex;
  flex: 1;
}

.chart {
  width: 100%;
  height: 100%;
  flex: 1;
}
</style>
