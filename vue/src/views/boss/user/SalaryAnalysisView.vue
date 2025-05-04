<template>
  <el-card class="container">
    <el-page-header>
      <template #title>
        <h1>薪资分析视图</h1>
      </template>
    </el-page-header>

    <el-alert
      :title="loading ? '加载数据中...' : `已加载 ${salaryJobList.length} 条数据`"
      :type="loading ? 'info' : 'success'"
      :closable="false"
      class="data-status"
    />

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="6" animated />
    </div>
    <div v-else-if="error" class="error-state">
      <el-alert :title="error" type="error" :closable="false" />
      <el-button type="primary" @click="handleRetry">重试</el-button>
    </div>
    <div v-else class="chart-container">
      <div ref="chartRef" class="chart-container"></div>
    </div>
  </el-card>
</template>

<script lang="ts" setup>
import { ref, onMounted, onBeforeUnmount} from 'vue';
import * as echarts from 'echarts';
import type { EChartsOption } from 'echarts';
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
let myChart: echarts.ECharts | null = null;

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
      crossStyle: { color: 'rgba(0, 0, 0, 0.5)' },
      label: {
        backgroundColor: '#6a7985'
      }
    },
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderWidth: 0,
    textStyle: {
      color: '#333',
      fontSize: 14
    },
    padding: [12, 16],
    extraCssText: 'box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1); border-radius: 8px;',
    formatter: function(params: echarts.DefaultLabelFormatterCallbackParams[]) {
      const item = salaryJobList.value[params[0].dataIndex];
      let result = `<div style="font-weight: bold; font-size: 16px; margin-bottom: 8px; color: #1a1a1a;">${item.positionName}</div>`;

      // 主要指标
      result += `<div style="margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #eee;">`;
      result += `<span style="color: #2196F3; font-size: 24px; font-weight: bold;">${item.avgSalary}k</span>`;
      result += `<span style="color: #666; margin-left: 8px;">平均薪资</span>`;
      result += '</div>';

      // 薪资范围
      result += `<div style="display: flex; justify-content: space-between; margin-bottom: 8px;">`;
      result += `<span style="color: #666;">薪资范围</span>`;
      result += `<span style="color: #333;">${item.minSalary}k - ${item.maxSalary}k</span>`;
      result += '</div>';

      // 职位数量
      result += `<div style="display: flex; justify-content: space-between; margin-bottom: 8px;">`;
      result += `<span style="color: #666;">职位数量</span>`;
      result += `<span style="color: #333;">${item.jobCount}个</span>`;
      result += '</div>';

      // 推荐城市信息
      result += `<div style="margin-top: 12px; padding-top: 8px; border-top: 1px solid #eee;">`;
      result += `<div style="color: #666; margin-bottom: 4px;">推荐城市: <span style="color: #333; font-weight: bold;">${item.recommendedCity}</span></div>`;
      result += `<div style="display: flex; justify-content: space-between;">`;
      result += `<span style="color: #666;">城市薪资</span>`;
      result += `<span style="color: #333;">${item.recommendedCitySalary}k</span>`;
      result += '</div>';
      result += `<div style="display: flex; justify-content: space-between;">`;
      result += `<span style="color: #666;">城市职位数</span>`;
      result += `<span style="color: #333;">${item.recommendedCityJobCount}个</span>`;
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
    }
  },
  legend: {
    type: 'scroll',
    top: '5%'
  },
  grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
  xAxis: [
    {
      type: 'category',
      axisPointer: { type: 'shadow' },
      axisLabel: { interval: 0, rotate: 45 }
    }
  ],
  yAxis: [
    { type: 'value', name: '薪资 (k)', axisLabel: { formatter: '{value} k' } },
    { type: 'value', name: '职位数量', axisLabel: { formatter: '{value}' } }
  ],
  // 系列结构定义，data 将在 updateChart 函数中设置
  series: [
    { name: '平均薪资', type: 'bar', yAxisIndex: 0, tooltip: { valueFormatter: (value: number | string) => value + ' k' } },
    { name: '最低工资', type: 'line', yAxisIndex: 0, smooth: true, tooltip: { valueFormatter: (value: number | string) => value + ' k' } },
    { name: '最高工资', type: 'line', yAxisIndex: 0, smooth: true, tooltip: { valueFormatter: (value: number | string) => value + ' k' } },
    { name: '职位数量', type: 'bar', yAxisIndex: 1, tooltip: { valueFormatter: (value: number | string) => value + ' 职位' } }
  ],
  // 添加基础动画配置
  animationDuration: 800,
  animationEasing: 'cubicInOut',
  animationThreshold: 50
};

/**
 * 根据指定的 key 对数据进行排序，并更新图表
 * @param sortKey 用于排序的 SalaryJob 对象的键
 * @param currentSelected 当前图例选中状态 (可选)
 * @param isFirstLoad 是否首次加载 (首次加载不应用过渡动画)
 */
const updateChart = (
  sortKey: keyof SalaryJob,
  currentSelected?: Record<string, boolean>,
  isFirstLoad = false
) => {
  if (!myChart) return;

  // --- 1. 对 salaryJobList 进行排序 ---
  salaryJobList.value.sort((a, b) => {
    // 降序排序
    const valA = a[sortKey] as number;
    const valB = b[sortKey] as number;
    return valB - valA;
  });

  // --- 2. 从排序后的 salaryJobList 重新生成数据数组 ---
  const positionNames = salaryJobList.value.map(item => item.positionName);
  const avgSalaries = salaryJobList.value.map(item => item.avgSalary);
  const minSalaries = salaryJobList.value.map(item => item.minSalary);
  const maxSalaries = salaryJobList.value.map(item => item.maxSalary);
  const jobCounts = salaryJobList.value.map(item => item.jobCount);
  // --- 3. 准备更新 ECharts 的选项 ---
  const updateOption = {
    xAxis: [
      {
        data: positionNames, // 更新 x 轴数据
      }
    ],
    series: [
      { name: '平均薪资', data: avgSalaries }, // 更新系列数据
      { name: '最低工资', data: minSalaries },
      { name: '最高工资', data: maxSalaries },
      { name: '职位数量', data: jobCounts },
    ],
    legend: {
      // 如果提供了 selected 状态，则更新图例状态
      selected: currentSelected
    }
  };

  // 针对排序操作设置特定的动画配置
  if (!isFirstLoad) {
    // 如果不是首次加载，则应用更好的排序动画效果
    Object.assign(updateOption, {
      // 更长的动画时间，让排序过程更连贯
      animationDurationUpdate: 600,
      // 平滑的缓动效果，使动画更自然
      animationEasingUpdate: 'elasticOut',
      // 指定不同系列的动画延迟，创建错落有致的动画效果
      animationDelayUpdate: (idx: number) => idx * 20
    });
  } else {
    // 首次加载时，可以使用更快的初始动画
    Object.assign(updateOption, {
      animationDurationUpdate: 400,
      animationEasingUpdate: 'cubicOut'
    });
  }

  // --- 4. 应用更新 ---
  // 使用默认的合并策略，只更新数据部分
  myChart.setOption(updateOption);
};

// 初始化图表
const initChart = () => {
  if (chartRef.value) {
    myChart = echarts.init(chartRef.value);

    // --- 1. 设置基础选项 (结构) ---
    myChart.setOption(baseOption as EChartsOption);

    // --- 2. 首次加载时，使用默认排序更新数据 ---
    // 获取初始的图例选中状态 (所有都选中)
    const initialSelected: Record<string, boolean> = {};
    // 将所有系列设置为选中状态
    (baseOption.series as {name: string}[]).forEach(item => {
      initialSelected[item.name] = true;
    });
    updateChart(defaultSortKey, initialSelected, true);

    // --- 3. 监听图例选择变化事件 ---
    myChart.on('legendselectchanged', function(params) {
      // 类型断言
      const selected = (params as {selected: Record<string, boolean>}).selected;
      let currentSortKey: keyof SalaryJob = defaultSortKey; // 默认排序键

      // 查找优先级最高的、当前被选中的图例对应的 key
      for (const priority of sortPriority) {
        if (selected[priority.name]) { // 如果该图例项被选中
          currentSortKey = priority.key;
          break; // 找到最高优先级的就停止查找
        }
      }

      // --- 4. 使用新的排序键和选中状态更新图表 ---
      updateChart(currentSortKey, selected);
    });
  }
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

<style scoped>
.container {
  width: 95%;
  margin: 20px auto;
}

.data-status {
  margin-bottom: 15px;
}

.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  width: 100%;
}

.chart-container {
  height: 700px;
  margin-top: 20px;
}
</style>
