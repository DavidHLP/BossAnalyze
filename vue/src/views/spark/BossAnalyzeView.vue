<template>
  <div class="boss-analyze-container">
    <div class="page-header">
      <h1 class="page-title">职位数据分析看板</h1>
      <div v-if="dataReady && !loading" class="data-summary">
        <div class="summary-item">
          <div class="summary-value">{{ originalJobsData.reduce((sum, item) => sum + (item.职位数量 || 0), 0) }}</div>
          <div class="summary-label">数据总量</div>
        </div>
        <div class="summary-item">
          <div class="summary-value">{{ averageSalary }}K</div>
          <div class="summary-label">平均薪资</div>
        </div>
        <div class="summary-item">
          <div class="summary-value">{{ uniqueCities }}</div>
          <div class="summary-label">覆盖城市</div>
        </div>
        <div class="summary-item">
          <div class="summary-value">{{ uniqueIndustries }}</div>
          <div class="summary-label">行业数量</div>
        </div>
      </div>
    </div>

    <div v-if="!dataReady && !loading" class="empty-data">
      <el-empty description="暂无数据，请稍候"></el-empty>
    </div>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="10" animated />
    </div>

    <template v-if="dataReady && !loading">
      <div class="chart-grid" :data-layout="chartLayout">
        <div class="chart-item" :class="{'chart-item-large': chartLayout === 'mixed' && index === 0}" v-for="(chart, index) in charts" :key="chart.id">
          <BossAnalyzeCharts
            :chartId="chart.id"
            :title="chart.title"
            :chartType="chart.type as any"
            :chartData="chartData"
            :loading="loading"
            :height="getChartHeight(index)"
            :colorTheme="colorTheme"
            @refresh="fetchData"
          />
        </div>
      </div>

      <el-card class="data-table-card">
        <template #header>
          <div class="card-header">
            <span>职位数据列表</span>
            <div class="header-operations">
              <el-tooltip content="切换布局" placement="top">
                <el-dropdown @command="handleLayoutChange">
                  <el-button circle :icon="Grid" size="small"></el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="grid">网格布局</el-dropdown-item>
                      <el-dropdown-item command="column">列布局</el-dropdown-item>
                      <el-dropdown-item command="mixed">混合布局</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </el-tooltip>
              <el-tooltip content="切换颜色主题" placement="top">
                <el-dropdown @command="handleThemeChange">
                  <el-button circle :icon="Brush" size="small"></el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="default">默认主题</el-dropdown-item>
                      <el-dropdown-item command="vivid">明亮主题</el-dropdown-item>
                      <el-dropdown-item command="cool">清凉主题</el-dropdown-item>
                      <el-dropdown-item command="warm">暖色主题</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </el-tooltip>
              <el-tooltip content="刷新数据" placement="top">
                <el-button circle @click="fetchData" :icon="Refresh" size="small"></el-button>
              </el-tooltip>
            </div>
          </div>
        </template>
        <el-table
          v-loading="loading"
          :data="tableData"
          border
          style="width: 100%"
          max-height="500"
          stripe
        >
          <el-table-column prop="职位名称" label="职位名称" min-width="120" />
          <el-table-column prop="城市" label="城市" min-width="100" />
          <el-table-column label="薪资范围" min-width="120">
            <template #default="scope">
              {{ `${scope.row.最低薪资K || 0}-${scope.row.最高薪资K || 0}K` }}
            </template>
          </el-table-column>
          <el-table-column prop="平均薪资K" label="平均薪资(K)" min-width="100" />
          <el-table-column prop="所属行业" label="所属行业" min-width="150" show-overflow-tooltip />
          <el-table-column prop="职位数量" label="职位数量" min-width="100" />
          <el-table-column label="学历要求" min-width="150" show-overflow-tooltip>
            <template #default="scope">
              {{ getEducationText(scope.row) }}
            </template>
          </el-table-column>
          <el-table-column prop="有效时间年月" label="统计时间" min-width="120" />
        </el-table>
        <div class="pagination-container">
          <el-pagination
            v-model:currentPage="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
    </template>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import { Refresh, Grid, Brush } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import BossAnalyzeCharts from '@/components/spark/BossAnalyzeCharts.vue';
import {
  getAllJobs,
} from '@/api/spark/boss';
import type {
  JobData,
  BossAnalyzeData,
} from '@/types/boss';

// 分页参数
const currentPage = ref(1);
const pageSize = ref(5);
const total = ref(0);

// 数据状态
const loading = ref(false);
const originalJobsData = ref<JobData[]>([]);
const tableData = ref<JobData[]>([]);
const dataReady = ref(false);

// 图表布局与主题
const chartLayout = ref('grid'); // grid, column, mixed
const colorTheme = ref('default'); // default, vivid, cool, warm

// 图表配置
const charts = [
  { id: 'salaryChart', title: '薪资分布', type: 'salary' },
  { id: 'cityChart', title: '城市分布', type: 'city' },
  { id: 'industryChart', title: '行业分布', type: 'industry' },
  { id: 'educationChart', title: '学历要求分布', type: 'education' }
];

// 统计分析数据
const chartData = ref<BossAnalyzeData>({
  salaryRanges: [],
  cityDistribution: [],
  skillsRequired: [],
  industryDistribution: [],
  educationDistribution: []
});

// 计算统计数值
const averageSalary = computed(() => {
  if (!originalJobsData.value.length) return 0;

  const total = originalJobsData.value.reduce((sum, job) => {
    return sum + (job['平均薪资K'] || 0);
  }, 0);

  return Math.round(total / originalJobsData.value.length);
});

const uniqueCities = computed(() => {
  if (!chartData.value.cityDistribution) return 0;
  return chartData.value.cityDistribution.length;
});

const uniqueIndustries = computed(() => {
  if (!chartData.value.industryDistribution) return 0;
  return chartData.value.industryDistribution.length;
});

// 处理图表布局变更
const handleLayoutChange = (layout: string) => {
  chartLayout.value = layout;
};

// 处理主题变更
const handleThemeChange = (theme: string) => {
  colorTheme.value = theme;
};

// 获取图表高度
const getChartHeight = (index: number) => {
  if (chartLayout.value === 'mixed' && index === 0) {
    return '340px'; // 400px的85%
  }
  return '255px'; // 默认高度已经在组件中调整为85%
};

// 生命周期钩子 - 页面加载时自动加载数据
onMounted(async () => {
  // 默认加载所有职位数据
  await fetchData();
});

// 获取数据方法
const fetchData = async () => {
  loading.value = true;
  try {
    // 查询所有数据
    const res = await getAllJobs();

    // 确保原始数据是有效的数组
    if (res && Array.isArray(res.data)) {
      originalJobsData.value = res.data;
    } else if (res && Array.isArray(res)) {
      originalJobsData.value = res;
    } else {
      originalJobsData.value = [];
      ElMessage.warning('返回数据格式不正确或为空');
    }

    await nextTick();
    processData();
    dataReady.value = originalJobsData.value.length > 0;
  } catch (error) {
    console.error('获取职位数据失败：', error);
    ElMessage.error('获取职位数据失败');
    originalJobsData.value = [];
    tableData.value = [];
    chartData.value = {
      salaryRanges: [],
      cityDistribution: [],
      skillsRequired: [],
      industryDistribution: [],
      educationDistribution: []
    };
    dataReady.value = false;
  } finally {
    loading.value = false;
  }
};

// 处理数据，生成图表和表格数据
const processData = () => {
  if (!originalJobsData.value.length) {
    total.value = 0;
    tableData.value = [];
    chartData.value = {
      salaryRanges: [],
      cityDistribution: [],
      skillsRequired: [],
      industryDistribution: [],
      educationDistribution: []
    };
    return;
  }

  // 按职位数量从大到小排序
  originalJobsData.value.sort((a, b) => (b['职位数量'] || 0) - (a['职位数量'] || 0));

  // 设置分页总数
  total.value = originalJobsData.value.length;

  // 更新表格数据 - 分页
  updateTableData();

  // 处理图表数据
  processChartData();
};

// 更新表格数据（分页）
const updateTableData = () => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  tableData.value = originalJobsData.value.slice(start, end);
};

// 处理图表数据
const processChartData = () => {
  // 薪资分布处理
  const salaryMap = new Map<string, number>();
  // 城市分布处理
  const cityMap = new Map<string, number>();
  // 行业分布处理
  const industryMap = new Map<string, number>();
  // 学历要求处理
  const educationMap = new Map<string, number>();

  originalJobsData.value.forEach(job => {
    // 薪资范围统计
    const lowSalary = job['最低薪资K'] || 0;
    const highSalary = job['最高薪资K'] || 0;

    if (lowSalary === 0 && highSalary === 0) {
      // 跳过没有薪资数据的职位
      return;
    }

    let salaryRange;
    if (highSalary <= 5) {
      salaryRange = '0-5K';
    } else if (highSalary <= 10) {
      salaryRange = '5-10K';
    } else if (highSalary <= 15) {
      salaryRange = '10-15K';
    } else if (highSalary <= 20) {
      salaryRange = '15-20K';
    } else if (highSalary <= 30) {
      salaryRange = '20-30K';
    } else {
      salaryRange = '30K以上';
    }

    salaryMap.set(salaryRange, (salaryMap.get(salaryRange) || 0) + (job['职位数量'] || 1));

    // 城市分布统计
    const city = job['城市'] || '未知';
    cityMap.set(city, (cityMap.get(city) || 0) + (job['职位数量'] || 1));

    // 行业分布统计
    const industry = job['所属行业'] || '未知';
    // 处理可能有多个行业的情况
    if (industry.includes('、')) {
      industry.split('、').forEach(ind => {
        if (ind.trim()) {
          industryMap.set(ind.trim(), (industryMap.get(ind.trim()) || 0) + (job['职位数量'] || 1) / industry.split('、').length);
        }
      });
    } else {
      industryMap.set(industry, (industryMap.get(industry) || 0) + (job['职位数量'] || 1));
    }

    // 学历分布统计
    const educations = [
      { key: '学历_初中及以下', label: '初中及以下' },
      { key: '学历_高中', label: '高中' },
      { key: '学历_中专/中技', label: '中专/中技' },
      { key: '学历_大专', label: '大专' },
      { key: '学历_本科', label: '本科' },
      { key: '学历_学历不限', label: '学历不限' }
    ];

    educations.forEach(edu => {
      const count = job[edu.key] || 0;
      if (count > 0) {
        educationMap.set(edu.label, (educationMap.get(edu.label) || 0) + count);
      }
    });
  });

  // 转换为图表所需格式
  chartData.value = {
    salaryRanges: Array.from(salaryMap.entries())
      .map(([range, count]) => ({ range, count }))
      .sort((a, b) => {
        // 按照薪资范围排序
        const rangeOrder = ['0-5K', '5-10K', '10-15K', '15-20K', '20-30K', '30K以上'];
        return rangeOrder.indexOf(a.range) - rangeOrder.indexOf(b.range);
      }),
    cityDistribution: Array.from(cityMap.entries())
      .map(([city, count]) => ({ city, count })),
    skillsRequired: [], // 没有技能数据
    industryDistribution: Array.from(industryMap.entries())
      .map(([industry, count]) => ({ industry, count: Math.round(count) })),
    educationDistribution: Array.from(educationMap.entries())
      .map(([education, count]) => ({ education, count }))
  };
};

// 获取学历要求文本
const getEducationText = (job: JobData): string => {
  const educations = [
    { key: '学历_初中及以下', label: '初中及以下' },
    { key: '学历_高中', label: '高中' },
    { key: '学历_中专/中技', label: '中专/中技' },
    { key: '学历_大专', label: '大专' },
    { key: '学历_本科', label: '本科' },
    { key: '学历_学历不限', label: '学历不限' }
  ];

  const educationParts = educations
    .filter(edu => job[edu.key] && job[edu.key] > 0)
    .map(edu => `${edu.label}(${job[edu.key]})`);

  return educationParts.length > 0 ? educationParts.join('、') : '无学历要求';
};

// 分页大小变更
const handleSizeChange = (val: number) => {
  pageSize.value = val;
  updateTableData();
};

// 当前页变更
const handleCurrentChange = (val: number) => {
  currentPage.value = val;
  updateTableData();
};
</script>

<style lang="scss" scoped>
.boss-analyze-container {
  padding: 17px; // 20px的85%
  max-width: 100%;
  overflow-x: hidden;
  max-height: 100%;
}

.page-header {
  margin-bottom: 20px; // 24px的85%左右
  display: flex;
  flex-direction: column;
  gap: 14px; // 16px的85%左右
}

.page-title {
  margin: 0;
  color: var(--el-color-primary);
  font-size: 22px; // 24px的85%左右
  font-weight: 700;
}

.data-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 14px; // 16px的85%
  margin-top: 7px; // 8px的85%
}

.summary-item {
  padding: 14px; // 16px的85%
  background: linear-gradient(135deg, rgba(66, 184, 131, 0.1), rgba(53, 73, 94, 0.05));
  border-radius: 8px;
  flex: 1;
  min-width: 102px; // 120px的85%
  text-align: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
  }

  .summary-value {
    font-size: 24px; // 28px的85%左右
    font-weight: 700;
    color: var(--el-color-primary);
    margin-bottom: 3px; // 4px的85%
  }

  .summary-label {
    font-size: 12px; // 14px的85%左右
    color: var(--el-text-color-secondary);
  }
}

// 动态网格布局 - 根据选择的布局方式调整
.chart-grid {
  display: grid;
  gap: 17px; // 20px的85%
  margin-bottom: 17px; // 20px的85%
  width: 100%;
  transition: all 0.3s ease;

  &:not(.loading) {
    grid-template-columns: repeat(auto-fit, minmax(255px, 1fr)); // 300px的85%
  }
}

.chart-item {
  min-width: 0;
  transition: all 0.3s ease;
  height: 100%;

  &-large {
    grid-column: 1 / -1;
  }
}

.data-table-card {
  margin-top: 17px; // 20px的85%
  border-radius: 7px; // 8px的85%
  width: 100%;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 5px 17px rgba(0, 0, 0, 0.08);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 14px; // 16px的85%左右
}

.header-operations {
  display: flex;
  gap: 8px; // 10px的85%
}

.pagination-container {
  margin-top: 17px; // 20px的85%
  display: flex;
  justify-content: flex-end;
}

.empty-data {
  padding: 51px 0; // 60px的85%
  text-align: center;
  width: 100%;
}

.loading-container {
  padding: 17px; // 20px的85%
  width: 100%;
}

// 添加对表格的响应式控制
:deep(.el-table) {
  max-width: 100%;
  table-layout: fixed;

  .el-table__body,
  .el-table__header {
    width: 100% !important;
  }
}

// 确保所有卡片不会超出容器
:deep(.el-card) {
  overflow: hidden;
  width: 100%;
}

// 媒体查询适配不同屏幕尺寸和布局方式
@media (max-width: 768px) {
  .chart-grid {
    grid-template-columns: 1fr !important; // 小屏幕单列显示
  }

  .data-summary {
    flex-direction: column;

    .summary-item {
      min-width: auto;
    }
  }
}

@media (min-width: 769px) {
  .chart-grid {
    &[data-layout="grid"] {
      grid-template-columns: repeat(2, 1fr);
    }

    &[data-layout="column"] {
      grid-template-columns: 1fr;
    }

    &[data-layout="mixed"] {
      .chart-item:first-child {
        grid-column: 1 / -1;
      }

      .chart-item:not(:first-child) {
        grid-column: auto;
      }
    }
  }
}

// 动态应用布局样式
.chart-grid[data-layout="column"] {
  grid-template-columns: 1fr;
}

.chart-grid[data-layout="grid"] {
  grid-template-columns: repeat(auto-fit, minmax(255px, 1fr)); // 300px的85%
}

.chart-grid[data-layout="mixed"] .chart-item-large {
  grid-column: 1 / -1;
}
</style>
