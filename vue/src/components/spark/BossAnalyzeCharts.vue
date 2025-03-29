<template>
  <div class="chart-container">
    <el-card v-loading="loading" class="chart-card">
      <template #header>
        <div class="chart-title">
          <span>{{ title }}</span>
          <el-tooltip content="刷新数据" placement="top">
            <el-button circle @click="refreshChart" :icon="Refresh" size="small"></el-button>
          </el-tooltip>
        </div>
      </template>
      <div :id="chartId" class="chart-content" :style="{ height }"></div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted, watch, computed, nextTick } from 'vue';
import * as echarts from 'echarts/core';
import { BarChart, PieChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { Refresh } from '@element-plus/icons-vue';
import 'echarts-wordcloud';
import type { BossAnalyzeData } from '@/types/boss';

// 注册必须的组件
echarts.use([
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  BarChart,
  PieChart,
  CanvasRenderer
]);

interface ChartProps {
  chartId: string;
  title: string;
  chartData: BossAnalyzeData;
  loading?: boolean;
  height?: string;
  chartType: 'salary' | 'city' | 'skills' | 'industry' | 'education';
  colorTheme?: string;
}

const props = withDefaults(defineProps<ChartProps>(), {
  loading: false,
  height: '255px',
  colorTheme: 'default'
});

const emit = defineEmits(['refresh']);

const chartInstance = ref<echarts.ECharts | null>(null);
const chartContainer = ref<HTMLElement | null>(null);

// 颜色主题配置
const colorThemes = {
  default: {
    salary: [
      ['#42b883', '#35495e'], // 渐变起止颜色
      '#42b883', // 单色
    ],
    city: [
      '#42b883', '#35495e', '#64C8A0', '#2c3e50',
      '#78D6B3', '#5c6b7f', '#8BE0C2', '#8ca3bc'
    ],
    industry: [
      '#42b883', '#35495e', '#64C8A0', '#2c3e50',
      '#78D6B3', '#5c6b7f', '#8BE0C2', '#8ca3bc'
    ],
    education: [
      ['#35495e', '#42b883'], // 渐变起止颜色
      '#35495e', // 单色
    ]
  },
  vivid: {
    salary: [
      ['#00c6fb', '#005bea'],
      '#1e90ff'
    ],
    city: [
      '#00c6fb', '#005bea', '#48b2ff', '#0043ce',
      '#75c5ff', '#0035b2', '#a0d7ff', '#002996'
    ],
    industry: [
      '#00c6fb', '#005bea', '#48b2ff', '#0043ce',
      '#75c5ff', '#0035b2', '#a0d7ff', '#002996'
    ],
    education: [
      ['#005bea', '#00c6fb'],
      '#005bea'
    ]
  },
  cool: {
    salary: [
      ['#667eea', '#764ba2'],
      '#667eea'
    ],
    city: [
      '#667eea', '#764ba2', '#85a0ff', '#8d65b6',
      '#a4c0ff', '#a480ca', '#c3d5ff', '#bc9cde'
    ],
    industry: [
      '#667eea', '#764ba2', '#85a0ff', '#8d65b6',
      '#a4c0ff', '#a480ca', '#c3d5ff', '#bc9cde'
    ],
    education: [
      ['#764ba2', '#667eea'],
      '#764ba2'
    ]
  },
  warm: {
    salary: [
      ['#ff9a9e', '#ff6a00'],
      '#ff7e5f'
    ],
    city: [
      '#ff9a9e', '#ff6a00', '#ffb8b8', '#ff7e33',
      '#ffd6d6', '#ff9466', '#ffe8e8', '#ffa98e'
    ],
    industry: [
      '#ff9a9e', '#ff6a00', '#ffb8b8', '#ff7e33',
      '#ffd6d6', '#ff9466', '#ffe8e8', '#ffa98e'
    ],
    education: [
      ['#ff6a00', '#ff9a9e'],
      '#ff6a00'
    ]
  }
};

// 获取当前主题的颜色
const getThemeColors = (type: string) => {
  const theme = props.colorTheme || 'default';
  const themeColors = colorThemes[theme as keyof typeof colorThemes] || colorThemes.default;
  return themeColors[type as keyof typeof themeColors] || colorThemes.default[type as keyof typeof colorThemes.default];
};

// 初始化图表
onMounted(async () => {
  await nextTick();
  initChart();
  window.addEventListener('resize', handleResize);
});

// 销毁图表，避免内存泄漏
onUnmounted(() => {
  if (chartInstance.value) {
    chartInstance.value.dispose();
    chartInstance.value = null;
  }
  window.removeEventListener('resize', handleResize);
});

// 监听窗口大小变化
const handleResize = () => {
  if (chartInstance.value) {
    chartInstance.value.resize();
  }
};

// 监听图表数据变化，更新图表
watch(() => props.chartData, () => {
  updateChart();
}, { deep: true });

// 监听主题变化，更新图表
watch(() => props.colorTheme, () => {
  updateChart();
});

// 监听加载状态，在加载完成后重新初始化图表
watch(() => props.loading, (newVal, oldVal) => {
  if (oldVal && !newVal) {
    // 从加载状态变为非加载状态时，重新初始化图表
    nextTick(() => {
      updateChart();
    });
  }
});

// 初始化图表
const initChart = () => {
  const chartDom = document.getElementById(props.chartId);
  if (!chartDom) {
    return;
  }

  chartContainer.value = chartDom;

  // 创建新图表实例或重用现有实例
  if (!chartInstance.value) {
    chartInstance.value = echarts.init(chartDom);
  }

  // 设置图表选项
  updateChart();
};

// 更新图表数据和选项
const updateChart = () => {
  if (!chartInstance.value) {
    return;
  }

  const options = chartOptions.value;
  chartInstance.value.setOption(options, true);
};

// 刷新图表数据
const refreshChart = () => {
  emit('refresh');
};

const chartOptions = computed(() => {
  switch (props.chartType) {
    case 'salary':
      return getSalaryChartOptions();
    case 'city':
      return getCityChartOptions();
    case 'skills':
      return getSkillsChartOptions();
    case 'industry':
      return getIndustryChartOptions();
    case 'education':
      return getEducationChartOptions();
    default:
      return {};
  }
});

const getSalaryChartOptions = () => {
  // 如果没有数据，返回空的配置
  if (!props.chartData || !props.chartData.salaryRanges || !props.chartData.salaryRanges.length) {
    return {
      title: {
        text: '薪资分布',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis'
      },
      grid: {
        containLabel: true, // 确保标签在容器内
        left: '3%',
        right: '4%',
        bottom: '3%'
      },
      xAxis: {
        type: 'category',
        data: []
      },
      yAxis: {
        type: 'value',
        name: '职位数量'
      },
      series: [
        {
          data: [],
          type: 'bar',
          showBackground: true,
          backgroundStyle: {
            color: 'rgba(180, 180, 180, 0.2)'
          }
        }
      ]
    };
  }

  // 获取当前主题的颜色
  const themeColors = getThemeColors('salary');

  // 处理数据
  const xData = props.chartData.salaryRanges.map((item) => item.range);
  const yData = props.chartData.salaryRanges.map((item) => item.count);

  return {
    title: {
      text: '薪资分布',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      formatter: '{b}: {c} 个职位'
    },
    grid: {
      containLabel: true, // 确保标签在容器内
      left: '3%',
      right: '4%',
      bottom: '3%'
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: {
        interval: 0,
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      name: '职位数量'
    },
    series: [
      {
        data: yData,
        type: 'bar',
        showBackground: true,
        backgroundStyle: {
          color: 'rgba(180, 180, 180, 0.2)'
        },
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: themeColors[0][0] },
              { offset: 1, color: themeColors[0][1] }
            ]
          }
        }
      }
    ]
  };
};

const getCityChartOptions = () => {
  // 如果没有数据，返回空的配置
  if (!props.chartData || !props.chartData.cityDistribution || !props.chartData.cityDistribution.length) {
    return {
      title: {
        text: '城市分布',
        left: 'center'
      },
      tooltip: {
        trigger: 'item'
      },
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          data: []
        }
      ]
    };
  }

  // 获取当前主题的颜色
  const themeColors = getThemeColors('city');

  // 处理数据 - 限制显示前10个城市
  const sortedData = [...props.chartData.cityDistribution]
    .sort((a, b) => b.count - a.count)
    .slice(0, 10)
    .map((item, index) => {
      return {
        name: item.city,
        value: item.count,
        itemStyle: {
          color: themeColors[index % themeColors.length]
        }
      };
    });

  return {
    title: {
      text: '城市分布 (TOP 10)',
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 个职位 ({d}%)'
    },
    legend: {
      orient: 'horizontal',
      bottom: 0,
      left: 'center',
      type: 'scroll',
      pageButtonPosition: 'end',
      width: '80%'
    },
    series: [
      {
        name: '城市分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: sortedData
      }
    ]
  };
};

const getIndustryChartOptions = () => {
  // 如果没有数据，返回空的配置
  if (!props.chartData || !props.chartData.industryDistribution || !props.chartData.industryDistribution.length) {
    return {
      title: {
        text: '行业分布',
        left: 'center'
      },
      tooltip: {
        trigger: 'item'
      },
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          data: []
        }
      ]
    };
  }

  // 获取当前主题的颜色
  const themeColors = getThemeColors('industry');

  // 处理数据 - 限制显示前10个行业
  const sortedData = [...props.chartData.industryDistribution]
    .sort((a, b) => b.count - a.count)
    .slice(0, 10)
    .map((item, index) => {
      return {
        name: item.industry,
        value: item.count,
        itemStyle: {
          color: themeColors[index % themeColors.length]
        }
      };
    });

  return {
    title: {
      text: '行业分布 (TOP 10)',
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 个职位 ({d}%)'
    },
    legend: {
      orient: 'horizontal',
      bottom: 0,
      left: 'center',
      type: 'scroll',
      pageButtonPosition: 'end',
      width: '80%'
    },
    series: [
      {
        name: '行业分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: sortedData
      }
    ]
  };
};

const getEducationChartOptions = () => {
  // 如果没有数据，返回空的配置
  if (!props.chartData || !props.chartData.educationDistribution || !props.chartData.educationDistribution.length) {
    return {
      title: {
        text: '学历要求分布',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis'
      },
      grid: {
        containLabel: true,
        left: '3%',
        right: '4%',
        bottom: '3%'
      },
      xAxis: {
        type: 'category',
        data: []
      },
      yAxis: {
        type: 'value',
        name: '职位数量'
      },
      series: [
        {
          data: [],
          type: 'bar',
          showBackground: true,
          backgroundStyle: {
            color: 'rgba(180, 180, 180, 0.2)'
          }
        }
      ]
    };
  }

  // 获取当前主题的颜色
  const themeColors = getThemeColors('education');

  // 处理数据 - 教育分布以柱状图的形式展示
  const sortedData = [...props.chartData.educationDistribution]
    .sort((a, b) => {
      // 学历顺序排序
      const eduOrder = ['初中及以下', '高中', '中专/中技', '大专', '本科', '学历不限'];
      return eduOrder.indexOf(a.education) - eduOrder.indexOf(b.education);
    });

  const xData = sortedData.map(item => item.education);
  const yData = sortedData.map(item => item.count);

  return {
    title: {
      text: '学历要求分布',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      formatter: '{b}: {c} 个职位'
    },
    grid: {
      containLabel: true,
      left: '3%',
      right: '4%',
      bottom: '3%'
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: {
        interval: 0,
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      name: '职位数量'
    },
    series: [
      {
        data: yData,
        type: 'bar',
        showBackground: true,
        backgroundStyle: {
          color: 'rgba(180, 180, 180, 0.2)'
        },
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: themeColors[0][0] },
              { offset: 1, color: themeColors[0][1] }
            ]
          }
        }
      }
    ]
  };
};

const getSkillsChartOptions = () => {
  // 如果没有数据，返回空的配置
  if (!props.chartData || !props.chartData.skillsRequired || !props.chartData.skillsRequired.length) {
    return {
      title: {
        text: '技能要求',
        left: 'center'
      },
      tooltip: {},
      series: [
        {
          type: 'wordCloud',
          shape: 'circle',
          data: []
        }
      ]
    };
  }

  // 获取当前主题的颜色
  const themeColors = getThemeColors('industry');

  // 处理数据 - 先按出现次数排序，然后取前30个关键词
  const sortedData = [...props.chartData.skillsRequired]
    .sort((a, b) => b.count - a.count)
    .slice(0, 30)
    .map((item, index) => {
      return {
        name: item.skill,
        value: item.count,
        textStyle: {
          color: themeColors[index % themeColors.length]
        }
      };
    });

  return {
    title: {
      text: '技能要求 (TOP 30)',
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: 出现 {c} 次'
    },
    series: [
      {
        type: 'wordCloud',
        shape: 'circle',
        left: 'center',
        top: 'center',
        width: '90%',
        height: '80%',
        right: null,
        bottom: null,
        sizeRange: [12, 30],
        rotationRange: [-45, 45],
        rotationStep: 15,
        gridSize: 8,
        drawOutOfBound: false,
        layoutAnimation: true,
        textStyle: {
          fontFamily: 'sans-serif',
          fontWeight: 'bold'
        },
        emphasis: {
          focus: 'self',
          textStyle: {
            shadowBlur: 10,
            shadowColor: '#333'
          }
        },
        data: sortedData
      }
    ]
  };
};
</script>

<style lang="scss" scoped>
.chart-container {
  width: 100%;
  height: 100%;
  transition: all 0.3s ease;
}

.chart-card {
  height: 100%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
    transform: translateY(-2px);
  }
}

.chart-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 16px;
}

.chart-content {
  width: 100%;
  min-height: 212px;
}

@media (max-width: 768px) {
  .chart-container {
    margin-bottom: 10px;
  }

  .chart-content {
    min-height: 170px;
  }
}

/* 暗黑模式适配 */
@media (prefers-color-scheme: dark) {
  .chart-card {
    background-color: rgba(30, 30, 30, 0.8);
    border-color: rgba(60, 60, 60, 0.8);

    .chart-title {
      color: #e0e0e0;
    }
  }
}
</style>
