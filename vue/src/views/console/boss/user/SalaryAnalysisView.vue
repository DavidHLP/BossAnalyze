<template>
  <el-card class="salary-analysis-card">
    <el-page-header class="salary-header">
      <template #title>
        <h1 class="salary-title">薪资分析视图</h1>
      </template>
    </el-page-header>

    <el-alert
      :title="loading ? '加载数据中...' : `已加载 ${salaryJobList.length} 条数据`"
      :type="loading ? 'info' : 'success'"
      :closable="false"
      class="data-status-alert"
    />

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="6" animated class="skeleton-content" />
    </div>
    <div v-else-if="error" class="error-container">
      <el-alert :title="error" type="error" :closable="false" class="error-alert" />
      <el-button type="primary" @click="handleRetry" class="retry-button">重试</el-button>
    </div>
    <div v-else class="chart-container">
      <div ref="chartRef" class="chart-content"></div>
    </div>
  </el-card>
</template>

<script lang="ts" setup>
import { ref, onMounted, onBeforeUnmount} from 'vue';
import * as echarts from 'echarts';
import type { ECharts, EChartsOption } from 'echarts';
import { getSalaryHotJob } from '@/api/boss/user/user';
import type { SalaryJob } from '@/api/boss/user/user.d';
import {
  ElCard,
  ElPageHeader,
  ElSkeleton,
  ElAlert,
  ElButton
} from 'element-plus';

// 图表容器引用
const chartRef = ref<HTMLElement | null>(null);
let myChart: ECharts | null = null;

// 1. 准备来自JSON的数据
const salaryJobList = ref<SalaryJob[]>([]);
const loading = ref<boolean>(false);
const error = ref<string>('');

/**
 * 获取热门职位薪资数据
 */
const fetchSalaryData = async (limit = 20) => {
  loading.value = true;
  error.value = '';

  try {
    const result = await getSalaryHotJob(limit);
    salaryJobList.value = result;
  } catch (err) {
    console.error('获取薪资数据失败:', err);
    error.value = '获取薪资数据失败，请稍后重试';
  } finally {
    loading.value = false;
  }
};

/**
 * 处理重试按钮点击
 */
const handleRetry = () => {
  fetchSalaryData();
};

// 定义排序优先级和对应的 key
// 优先级: 平均薪资 > 职位数量 > 最高工资 > 最低工资
const sortPriority: { name: string; key: keyof SalaryJob }[] = [
  { name: '平均薪资', key: 'avgSalary' },
  { name: '职位数量', key: 'jobCount' },
  { name: '最高工资', key: 'maxSalary' },
  { name: '最低工资', key: 'minSalary' }
];
// 如果都没有选中，默认按平均薪资排序
const defaultSortKey: keyof SalaryJob = 'avgSalary';

// --- ECharts 基础配置 (静态部分) ---
const baseOption = {
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'cross',
      crossStyle: { color: 'rgba(59, 130, 246, 0.5)' },
      label: {
        backgroundColor: '#3B82F6'
      }
    },
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderWidth: 1,
    borderColor: '#E5E7EB',
    textStyle: {
      color: '#344767',
      fontSize: 14
    },
    padding: [12, 16],
    extraCssText: 'box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1); border-radius: 8px;',
    formatter: function(params: echarts.DefaultLabelFormatterCallbackParams[]) {
      // 这里不能直接使用原始数据索引，需要根据当前点击的系列和索引找到对应数据
      const positionName = params[0].name as string; // 当前鼠标位置对应的职位名称
      const item = salaryJobList.value.find(job => job.positionName === positionName);

      if (!item) {
        return '暂无数据';
      }

      let result = `<div style="font-weight: bold; font-size: 16px; margin-bottom: 8px; color: #344767;">${item.positionName}</div>`;
      result += `<div style="display: flex; justify-content: space-between; margin-bottom: 4px;">`;
      result += `<span style="color: #3B82F6; font-weight: bold;">${item.avgSalary}k</span>`;
      result += `<span style="color: #67748E;">平均薪资</span>`;
      result += '</div>';
      result += `<div style="display: flex; justify-content: space-between; margin-bottom: 4px;">`;
      result += `<span style="color: #67748E;">薪资范围</span>`;
      result += `<span style="color: #344767;">${item.minSalary}k - ${item.maxSalary}k</span>`;
      result += '</div>';
      result += `<div style="display: flex; justify-content: space-between; margin-bottom: 4px;">`;
      result += `<span style="color: #67748E;">职位数量</span>`;
      result += `<span style="color: #344767;">${item.jobCount}个</span>`;
      result += '</div>';
      result += `<div style="border-top: 1px solid #E5E7EB; margin-top: 8px; padding-top: 8px;">`;
      result += `<div style="margin-bottom: 4px;"><span style="color: #67748E;">推荐城市: </span><span style="color: #344767; font-weight: bold;">${item.recommendedCity}</span></div>`;
      result += `<div style="display: flex; justify-content: space-between; margin-bottom: 4px;">`;
      result += `<span style="color: #67748E;">城市薪资</span>`;
      result += `<span style="color: #3B82F6;">${item.recommendedCitySalary}k</span>`;
      result += '</div>';
      result += `<div style="display: flex; justify-content: space-between;">`;
      result += `<span style="color: #67748E;">城市职位数</span>`;
      result += `<span style="color: #344767;">${item.recommendedCityJobCount}个</span>`;
      result += '</div>';
      result += '</div>';
      return result;
    }
  },
  toolbox: {
    feature: {
      dataView: { show: true, readOnly: false },
      magicType: { show: true, type: ['line', 'bar'] },
      restore: { show: true },
      saveAsImage: { show: true }
    },
    textStyle: {
      color: '#67748E'
    },
    iconStyle: {
      borderColor: '#3B82F6'
    },
    emphasis: {
      iconStyle: {
        borderColor: '#2563EB'
      }
    }
  },
  legend: {
    type: 'scroll',
    top: '5%',
    textStyle: {
      color: '#344767'
    },
    selectedMode: 'multiple',
    inactiveColor: '#9CA3AF',
    itemStyle: {
      borderWidth: 0
    },
    icon: 'circle',
    itemWidth: 10,
    itemHeight: 10
  },
  grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
  xAxis: [
    {
      type: 'category',
      axisPointer: { type: 'shadow' },
      axisLabel: {
        interval: 0,
        rotate: 45,
        color: '#67748E',
        fontSize: 12
      },
      axisLine: {
        lineStyle: {
          color: '#E5E7EB'
        }
      },
      splitLine: {
        show: false
      }
    }
  ],
  yAxis: [
    {
      type: 'value',
      name: '薪资 (k)',
      axisLabel: {
        formatter: '{value} k',
        color: '#67748E'
      },
      nameTextStyle: {
        color: '#344767',
        padding: [0, 0, 0, 10]
      },
      splitLine: {
        lineStyle: {
          type: 'dashed',
          color: '#E5E7EB'
        }
      },
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      }
    },
    {
      type: 'value',
      name: '职位数量',
      axisLabel: {
        formatter: '{value}',
        color: '#67748E'
      },
      nameTextStyle: {
        color: '#344767',
        padding: [0, 10, 0, 0]
      },
      splitLine: {
        show: false
      },
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      }
    }
  ],
  // 系列结构定义，data 将在 updateChart 函数中设置
  series: [
    {
      name: '平均薪资',
      type: 'bar',
      yAxisIndex: 0,
      tooltip: { valueFormatter: (value: number | string) => value + ' k' },
      itemStyle: {
        color: '#3B82F6',
        borderRadius: [4, 4, 0, 0]
      },
      emphasis: {
        itemStyle: {
          color: '#2563EB'
        }
      }
    },
    {
      name: '最低工资',
      type: 'line',
      yAxisIndex: 0,
      smooth: true,
      tooltip: { valueFormatter: (value: number | string) => value + ' k' },
      lineStyle: {
        width: 3,
        color: '#38BDF8'
      },
      symbol: 'circle',
      symbolSize: 8,
      itemStyle: {
        color: '#38BDF8',
        borderWidth: 2,
        borderColor: '#FFFFFF'
      }
    },
    {
      name: '最高工资',
      type: 'line',
      yAxisIndex: 0,
      smooth: true,
      tooltip: { valueFormatter: (value: number | string) => value + ' k' },
      lineStyle: {
        width: 3,
        color: '#60A5FA'
      },
      symbol: 'circle',
      symbolSize: 8,
      itemStyle: {
        color: '#60A5FA',
        borderWidth: 2,
        borderColor: '#FFFFFF'
      }
    },
    {
      name: '职位数量',
      type: 'bar',
      yAxisIndex: 1,
      tooltip: { valueFormatter: (value: number | string) => value + ' 职位' },
      itemStyle: {
        color: '#93C5FD',
        borderRadius: [4, 4, 0, 0]
      },
      emphasis: {
        itemStyle: {
          color: '#60A5FA'
        }
      }
    }
  ],
  // 添加基础动画配置
  animationDuration: 800,
  animationEasing: 'cubicInOut',
  animationThreshold: 50
};

/**
 * 初始化图表
 */
const initChart = () => {
  if (chartRef.value) {
    myChart = echarts.init(chartRef.value);

    // 首次加载时，使用默认排序
    const sortedData = prepareSortedData(defaultSortKey);

    // 设置初始配置和数据（确保还原按钮有正确的初始状态可还原）
    myChart.setOption({
      tooltip: baseOption.tooltip,
      toolbox: baseOption.toolbox,
      legend: baseOption.legend,
      grid: baseOption.grid,
      xAxis: [{
        type: 'category',
        data: sortedData.positionNames,
        axisPointer: { type: 'shadow' },
        axisLabel: {
          interval: 0,
          rotate: 45,
          color: '#67748E',
          fontSize: 12
        },
        axisLine: {
          lineStyle: {
            color: '#E5E7EB'
          }
        },
        splitLine: {
          show: false
        }
      }],
      yAxis: baseOption.yAxis,
      series: [
        {
          name: '平均薪资',
          type: 'bar',
          yAxisIndex: 0,
          data: sortedData.avgSalaries,
          tooltip: { valueFormatter: (value: number | string) => value + ' k' },
          itemStyle: {
            color: '#3B82F6',
            borderRadius: [4, 4, 0, 0]
          },
          emphasis: {
            itemStyle: {
              color: '#2563EB'
            }
          }
        },
        {
          name: '最低工资',
          type: 'line',
          yAxisIndex: 0,
          data: sortedData.minSalaries,
          smooth: true,
          tooltip: { valueFormatter: (value: number | string) => value + ' k' },
          lineStyle: {
            width: 3,
            color: '#38BDF8'
          },
          symbol: 'circle',
          symbolSize: 8,
          itemStyle: {
            color: '#38BDF8',
            borderWidth: 2,
            borderColor: '#FFFFFF'
          }
        },
        {
          name: '最高工资',
          type: 'line',
          yAxisIndex: 0,
          data: sortedData.maxSalaries,
          smooth: true,
          tooltip: { valueFormatter: (value: number | string) => value + ' k' },
          lineStyle: {
            width: 3,
            color: '#60A5FA'
          },
          symbol: 'circle',
          symbolSize: 8,
          itemStyle: {
            color: '#60A5FA',
            borderWidth: 2,
            borderColor: '#FFFFFF'
          }
        },
        {
          name: '职位数量',
          type: 'bar',
          yAxisIndex: 1,
          data: sortedData.jobCounts,
          tooltip: { valueFormatter: (value: number | string) => value + ' 职位' },
          itemStyle: {
            color: '#93C5FD',
            borderRadius: [4, 4, 0, 0]
          },
          emphasis: {
            itemStyle: {
              color: '#60A5FA'
            }
          }
        }
      ],
      animationDuration: 800,
      animationEasing: 'cubicInOut',
      animationThreshold: 50
    } as EChartsOption);

    // 监听图例选择变化事件
    myChart.on('legendselectchanged', function(params) {
      // 类型断言
      const selected = (params as {selected: Record<string, boolean>}).selected;
      let currentSortKey: keyof SalaryJob = defaultSortKey; // 默认排序键

      // 检查是否所有图例都未被选中
      const allUnselected = Object.values(selected).every(value => value === false);

      if (!allUnselected) {
        // 查找优先级最高的、当前被选中的图例对应的 key
        let foundSelected = false;
        for (const priority of sortPriority) {
          if (selected[priority.name]) { // 如果该图例项被选中
            currentSortKey = priority.key;
            foundSelected = true;
            break; // 找到最高优先级的就停止查找
          }
        }

        // 如果没有找到任何选中的项，但图例选择状态不是全部未选中
        // 这种情况可能发生在用户手动切换图例时
        if (!foundSelected) {
          // 保持默认排序键
          console.log('使用默认排序键:', defaultSortKey);
        }
      } else {
        console.log('所有图例都未选中，使用默认排序键:', defaultSortKey);
      }

      // 使用新的排序键和选中状态更新图表
      updateChart(currentSortKey, selected);
    });
  }
};

/**
 * 准备排序后的数据
 * @param sortKey 用于排序的键
 * @returns 排序后的数据数组，但不会修改原始数据
 */
const prepareSortedData = (sortKey: keyof SalaryJob) => {
  // 对数据进行排序（创建副本以避免修改原始数据）
  const sortedList = [...salaryJobList.value].sort((a, b) => {
    // 降序排序
    const valA = a[sortKey] as number;
    const valB = b[sortKey] as number;
    return valB - valA;
  });

  // 从排序后的数据生成数据数组
  return {
    positionNames: sortedList.map(item => item.positionName),
    avgSalaries: sortedList.map(item => item.avgSalary),
    minSalaries: sortedList.map(item => item.minSalary),
    maxSalaries: sortedList.map(item => item.maxSalary),
    jobCounts: sortedList.map(item => item.jobCount)
  };
};

/**
 * 根据指定的 key 对数据进行排序，并更新图表
 * @param sortKey 用于排序的 SalaryJob 对象的键
 * @param currentSelected 当前图例选中状态 (可选)
 */
const updateChart = (
  sortKey: keyof SalaryJob,
  currentSelected?: Record<string, boolean>
) => {
  if (!myChart) return;

  // 重新排序，确保所有数据同步
  const sortedData = prepareSortedData(sortKey);

  // 更新 option，保证 xAxis 和 series 数据一一对应
  myChart.setOption({
    xAxis: [
      {
        type: 'category',
        data: sortedData.positionNames,
        axisPointer: { type: 'shadow' },
        axisLabel: {
          interval: 0,
          rotate: 45,
          color: '#67748E',
          fontSize: 12
        },
        axisLine: {
          lineStyle: {
            color: '#E5E7EB'
          }
        },
        splitLine: {
          show: false
        }
      }
    ],
    series: [
      {
        name: '平均薪资',
        type: 'bar',
        yAxisIndex: 0,
        data: sortedData.avgSalaries,
        tooltip: { valueFormatter: (value: number | string) => value + ' k' },
        itemStyle: {
          color: '#3B82F6',
          borderRadius: [4, 4, 0, 0]
        },
        emphasis: {
          itemStyle: {
            color: '#2563EB'
          }
        }
      },
      {
        name: '最低工资',
        type: 'line',
        yAxisIndex: 0,
        data: sortedData.minSalaries,
        smooth: true,
        tooltip: { valueFormatter: (value: number | string) => value + ' k' },
        lineStyle: {
          width: 3,
          color: '#38BDF8'
        },
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: {
          color: '#38BDF8',
          borderWidth: 2,
          borderColor: '#FFFFFF'
        }
      },
      {
        name: '最高工资',
        type: 'line',
        yAxisIndex: 0,
        data: sortedData.maxSalaries,
        smooth: true,
        tooltip: { valueFormatter: (value: number | string) => value + ' k' },
        lineStyle: {
          width: 3,
          color: '#60A5FA'
        },
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: {
          color: '#60A5FA',
          borderWidth: 2,
          borderColor: '#FFFFFF'
        }
      },
      {
        name: '职位数量',
        type: 'bar',
        yAxisIndex: 1,
        data: sortedData.jobCounts,
        tooltip: { valueFormatter: (value: number | string) => value + ' 职位' },
        itemStyle: {
          color: '#93C5FD',
          borderRadius: [4, 4, 0, 0]
        },
        emphasis: {
          itemStyle: {
            color: '#60A5FA'
          }
        }
      }
    ],
    legend: {
      selected: currentSelected
    },
    animationDurationUpdate: 600,
    animationEasingUpdate: 'elasticOut',
    animationDelayUpdate: (idx: number) => idx * 20,
    transition: ['all'],
    animationThreshold: 50
  } as EChartsOption, false); // 使用false，不使用合并模式而是完全替换
};

// 处理窗口大小变化
const handleResize = () => {
  if (myChart) {
    myChart.resize();
  }
};

// 组件挂载后初始化图表和获取数据
onMounted(async () => {
  await fetchSalaryData();

  if (salaryJobList.value.length > 0) {
    initChart();
  }

  window.addEventListener('resize', handleResize);
});

// 组件销毁前清理
onBeforeUnmount(() => {
  // 移除事件监听器
  if (myChart) {
    // ECharts 5 推荐使用 off 解绑事件
    myChart.off('legendselectchanged');
    // 销毁图表实例
    myChart.dispose();
    myChart = null;
  }
  window.removeEventListener('resize', handleResize);
});
</script>

<style lang="scss" scoped>
.salary-analysis-card {
  margin: var(--spacing-md);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--box-shadow);
  background-color: var(--card-color);
  transition: box-shadow 0.3s ease;
  overflow: hidden;

  &:hover {
    box-shadow: var(--box-shadow-lg);
  }

  .salary-header {
    padding: var(--spacing-md) var(--spacing-lg);
    border-bottom: 1px solid var(--border-color);
    margin-bottom: var(--spacing-md);

    .salary-title {
      font-size: var(--font-size-xl);
      color: var(--text-primary);
      margin: 0;
      font-weight: 600;
      display: inline-flex;
      align-items: center;

      &::before {
        content: '';
        display: inline-block;
        width: 4px;
        height: 18px;
        background: var(--gradient-primary, linear-gradient(45deg, #3B82F6, #60A5FA));
        margin-right: var(--spacing-sm);
        border-radius: 2px;
      }
    }
  }

  .data-status-alert {
    margin: var(--spacing-md) var(--spacing-lg);
    border-radius: var(--border-radius-md);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
  }

  .loading-container, .error-container {
    padding: var(--spacing-lg);

    .skeleton-content {
      margin-top: var(--spacing-md);
    }

    .error-alert {
      margin-bottom: var(--spacing-md);
    }

    .retry-button {
      background-color: var(--primary-color);
      border-color: var(--primary-color);
      color: white;
      padding: 8px 20px;
      border-radius: var(--border-radius-md);
      font-weight: 500;
      transition: all 0.3s ease;

      &:hover {
        background-color: var(--primary-dark);
        border-color: var(--primary-dark);
        transform: translateY(-2px);
      }
    }
  }

  .chart-container {
    padding: var(--spacing-md);

    .chart-content {
      height: 550px;
      width: 100%;
      border-radius: var(--border-radius-md);
      background-color: #FFFFFF;
      overflow: hidden;
      box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.05);
      position: relative;

      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        height: 2px;
        background: linear-gradient(to right, var(--primary-light), var(--primary-color), var(--primary-light));
        z-index: 1;
      }
    }
  }
}

// 响应式调整
@media (max-width: 768px) {
  .salary-analysis-card {
    margin: var(--spacing-sm);

    .salary-header {
      padding: var(--spacing-sm) var(--spacing-md);

      .salary-title {
        font-size: var(--font-size-lg);
      }
    }

    .data-status-alert {
      margin: var(--spacing-sm);
    }

    .chart-container {
      padding: var(--spacing-sm);

      .chart-content {
        height: 450px;
      }
    }
  }
}

// 打印样式优化
@media print {
  .salary-analysis-card {
    box-shadow: none !important;
    margin: 0 !important;

    .chart-container .chart-content {
      height: 100% !important;
      page-break-inside: avoid;
    }
  }
}
</style>
