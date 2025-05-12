<template>
  <div class="chart-container">
    <div id="job-scatter-plot-three" ref="chartRef" class="chart"></div>
  </div>
</template>

<script lang="ts">
import { defineComponent, onMounted, reactive, ref, watch } from 'vue';
import * as echarts from 'echarts';
import 'echarts-gl';
import { getThreeDimensionalAnalysisChart } from '@/api/boss/user/user';
import type { JobData } from '@/api/boss/user/user.d';

export default defineComponent({
  name: 'JobScatterPlotThree',
  props: {
    cityName: {
      type: String,
      required: true
    },
    positionName: {
      type: String,
      required: true
    }
  },
  setup(props) {
    const chartRef = ref<HTMLElement | null>(null);
    const chart = ref<echarts.ECharts | null>(null);
    const loading = ref(false);
    const chartData = reactive<JobData[]>([]);

    const initChart = () => {
      if (!chartRef.value) return;

      chart.value = echarts.init(chartRef.value);

      window.addEventListener('resize', () => {
        chart.value?.resize();
      });
    };

    const loadData = async () => {
      loading.value = true;
      try {
        const data = await getThreeDimensionalAnalysisChart(props.cityName, props.positionName);
        console.log(data);
        Object.assign(chartData, data);
        updateChart();
      } catch (error) {
        console.error('获取三维分析图表数据失败', error);
      } finally {
        loading.value = false;
      }
    };

    const updateChart = () => {
      if (!chart.value) return;

      const option = {
        tooltip: {},
        grid3D: {
          width: '50%'
        },
        xAxis3D: {
          name: '薪资水平',
          type: 'value'
        },
        yAxis3D: {
          name: '学历要求',
          type: 'value'
        },
        zAxis3D: {
          name: '经验要求',
          type: 'value'
        },
        grid: [
          { left: '50%', width: '20%', bottom: '57%' },
          { left: '75%', width: '20%', bottom: '57%' },
          { left: '50%', width: '20%', top: '57%' },
          { left: '75%', width: '20%', top: '57%' }
        ],
        xAxis: [
          {
            type: 'value',
            gridIndex: 0,
            name: '薪资水平',
            axisLabel: { rotate: 50, interval: 0 }
          },
          {
            type: 'category',
            gridIndex: 1,
            name: '职位',
            boundaryGap: false,
            axisLabel: { rotate: 50, interval: 0 }
          },
          {
            type: 'value',
            gridIndex: 2,
            name: '薪资水平',
            axisLabel: { rotate: 50, interval: 0 }
          },
          {
            type: 'value',
            gridIndex: 3,
            name: '学历要求',
            axisLabel: { rotate: 50, interval: 0 }
          }
        ],
        yAxis: [
          { type: 'value', gridIndex: 0, name: '学历要求' },
          { type: 'value', gridIndex: 1, name: '薪资水平' },
          { type: 'value', gridIndex: 2, name: '经验要求' },
          { type: 'value', gridIndex: 3, name: '经验要求' }
        ],
        dataset: {
          dimensions: [
            'salaryValue',
            'degreeValue',
            'experienceValue',
            'positionName',
            'companyName'
          ],
          source: chartData
        },
        series: [
          {
            type: 'scatter3D',
            symbolSize: 5,
            encode: {
              x: 'salaryValue',
              y: 'degreeValue',
              z: 'experienceValue',
              tooltip: [0, 1, 2, 3, 4]
            }
          },
          {
            type: 'scatter',
            symbolSize: 3,
            xAxisIndex: 0,
            yAxisIndex: 0,
            encode: {
              x: 'salaryValue',
              y: 'degreeValue',
              tooltip: [0, 1, 2, 3, 4]
            }
          },
          {
            type: 'scatter',
            symbolSize: 3,
            xAxisIndex: 1,
            yAxisIndex: 1,
            encode: {
              x: 'positionName',
              y: 'salaryValue',
              tooltip: [0, 1, 2, 3, 4]
            }
          },
          {
            type: 'scatter',
            symbolSize: 3,
            xAxisIndex: 2,
            yAxisIndex: 2,
            encode: {
              x: 'salaryValue',
              y: 'experienceValue',
              tooltip: [0, 1, 2, 3, 4]
            }
          },
          {
            type: 'scatter',
            symbolSize: 3,
            xAxisIndex: 3,
            yAxisIndex: 3,
            encode: {
              x: 'degreeValue',
              y: 'experienceValue',
              tooltip: [0, 1, 2, 3, 4]
            }
          }
        ]
      };

      chart.value.setOption(option);
    };

    onMounted(() => {
      initChart();
      loadData();
    });

    watch([() => props.cityName, () => props.positionName], () => {
      loadData();
    });

    return {
      chartRef,
      loading
    };
  }
});
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 100%;
}

.chart {
  width: 100%;
  height: 600px;
}
</style>
