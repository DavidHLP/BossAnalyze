<template>
  <el-card class="scatter-plot-card">
    <el-page-header class="scatter-header">
      <template #title>
        <h1 class="scatter-title">职位分析散点图</h1>
      </template>
    </el-page-header>
    <el-row :gutter="10" class="filter-row">
      <el-col :span="6" :xs="6">
        <el-form-item label="X轴:">
          <el-select v-model="xAxis" @change="fetchData">
            <el-option value="salary_value" label="薪资"></el-option>
            <el-option value="degree_value" label="学历要求"></el-option>
            <el-option value="experience_value" label="经验要求"></el-option>
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="6" :xs="6">
        <el-form-item label="Y轴:">
          <el-select v-model="yAxis" @change="fetchData">
            <el-option value="degree_value" label="学历要求"></el-option>
            <el-option value="salary_value" label="薪资"></el-option>
            <el-option value="experience_value" label="经验要求"></el-option>
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="6" :xs="6">
        <el-form-item label="城市筛选:">
          <el-select v-model="cityFilter" @change="fetchData">
            <el-option value="all" label="全部城市"></el-option>
            <el-option v-for="city in cities" :key="city" :value="city" :label="city"></el-option>
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="6" :xs="6">
        <el-form-item label="行业筛选:">
          <el-select v-model="positionFilter" @change="fetchData">
            <el-option value="all" label="全部职位"></el-option>
            <el-option v-for="position in positions" :key="position" :value="position" :label="position"></el-option>
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>
    <el-alert
      :title="loading ? '加载数据中...' : `已加载 ${jobData.length} 条数据`"
      :type="loading ? 'info' : 'success'"
      :closable="false"
      class="data-status-alert"
    />
    <div class="chart-container">
      <div id="chart" ref="chartRef" class="chart-content"></div>
    </div>
    <el-drawer
      v-model="showDetails"
      title="公司详情"
      direction="rtl"
      size="35%"
      :with-header="false"
      :append-to-body="true"
    >
      <company-details-card
        :company-info="companyInfo"
        @open-company-url="openCompanyUrl"
        @open-job-url="openJobUrl"
        @open-google-maps="openGoogleMaps"
      />
    </el-drawer>
  </el-card>
</template>

<script lang="ts" setup>
import {ref, onMounted, reactive, computed, onUnmounted} from 'vue';
import * as echarts from 'echarts';
import type {EChartsOption, ECharts} from 'echarts';
import type {JobData, CompanyInfo} from '@/api/boss/user/user.d';
import {getTwoDimensionalAnalysisChart, getCityNameList, getPositionNameList} from '@/api/boss/user/user';
import {
  ElCard,
  ElRow,
  ElCol,
  ElSelect,
  ElOption,
  ElFormItem,
  ElAlert,
  ElDrawer,
  ElPageHeader,
} from 'element-plus';
// 导入公司详情卡组件
import CompanyDetailsCard from './components/CompanyDetailsCard.vue';

// 组件状态
const jobData = ref<JobData[]>([]);
const cities = ref<string[]>([]);
const positions = ref<string[]>([]);
const chartRef = ref<HTMLDivElement | null>(null);
const chartInstance = ref<ECharts | null>(null);
const showDetails = ref(false);
const xAxis = ref('salary_value');
const yAxis = ref('degree_value');
const cityFilter = ref('all');
const positionFilter = ref('all');
const loading = ref(false);
const overlapMethod = ref('jitter'); // 默认使用随机抖动处理重叠

// 公司详情信息
const companyInfo = reactive<CompanyInfo>({
  companyName: '',
  positionName: '',
  cityName: '',
  salary: '',
  degree: '',
  experience: '',
  companySize: '',
  financingStage: '',
  companyUrl: '',
  jobUrl: '',
  employeeBenefits: [],
  jobRequirements: [],
  address: ''
});

// 全局常量
const DEGREES_ARRAY = ['不限', '初中', '高中/中专', '大专', '在校/应届', '本科', '硕士', '博士'];

// 计算属性：筛选后的数据
const filteredData = computed(() => {
  return jobData.value;
});

// 打开公司URL
const openCompanyUrl = () => {
  if (companyInfo.companyUrl) {
    window.open(companyInfo.companyUrl, '_blank');
  }
};

// 打开职位URL
const openJobUrl = () => {
  if (companyInfo.jobUrl) {
    window.open(companyInfo.jobUrl, '_blank');
  }
};

// 解析学历要求文本为数值
const getDegreeValue = (degreeStr: string): number => {
  if (!degreeStr) return 0;

  const degreeMap: Record<string, number> = {
    '不限': 0,
    '初中': 1,
    '中专': 2,
    '高中': 2,
    '高中/中专': 2,
    '大专': 3,
    '本科': 4,
    '硕士': 5,
    '博士': 6
  };

  for (const [key, value] of Object.entries(degreeMap)) {
    if (degreeStr.includes(key)) {
      return value;
    }
  }

  return 0;
};

// 解析经验要求文本为数值
const getExperienceValue = (expStr: string): number => {
  if (!expStr) return 0;

  // 匹配如"1-3年"、"3年以上"、"1年以下"等格式
  const matchYears = expStr.match(/(\d+)(?:-\d+)?年/);
  if (matchYears && matchYears[1]) {
    return Number(matchYears[1]);
  }

  // 处理特殊情况
  if (expStr.includes('应届') || expStr.includes('实习') || expStr.includes('在校')) {
    return 0;
  }

  if (expStr.includes('不限')) {
    return 0;
  }

  return 0;
};

// 解析薪资字符串，提取范围值
const parseSalaryRange = (salaryStr: string): {min: number; max: number} => {
  if (!salaryStr) return {min: 0, max: 0};

  // 移除可能的空格
  const cleanSalary = salaryStr.replace(/\s+/g, '');

  // 匹配形如"4-5K"的格式
  const matchKRange = cleanSalary.match(/^(\d+)-(\d+)[Kk]$/);
  if (matchKRange && matchKRange[1] && matchKRange[2]) {
    return {
      min: Number(matchKRange[1]),
      max: Number(matchKRange[2])
    };
  }

  // 匹配形如"4K-5K"的格式
  const matchKKRange = cleanSalary.match(/^(\d+)[Kk]-(\d+)[Kk]$/);
  if (matchKKRange && matchKKRange[1] && matchKKRange[2]) {
    return {
      min: Number(matchKKRange[1]),
      max: Number(matchKKRange[2])
    };
  }

  // 匹配形如"4千-5千"的格式
  const matchThousandRange = cleanSalary.match(/^(\d+)千-(\d+)千$/);
  if (matchThousandRange && matchThousandRange[1] && matchThousandRange[2]) {
    return {
      min: Number(matchThousandRange[1]),
      max: Number(matchThousandRange[2])
    };
  }

  // 匹配形如"1万-2万"的格式
  const matchTenThousandRange = cleanSalary.match(/^(\d+)万-(\d+)万$/);
  if (matchTenThousandRange && matchTenThousandRange[1] && matchTenThousandRange[2]) {
    return {
      min: Number(matchTenThousandRange[1]) * 10,
      max: Number(matchTenThousandRange[2]) * 10
    };
  }

  // 对于单一值，使用parseSalary获取值，并设置相同的min和max
  const singleValue = parseSalary(salaryStr);
  return {
    min: singleValue,
    max: singleValue
  };
};

// 解析经验字符串，提取范围值
const getExperienceRange = (expStr: string): {min: number; max: number} => {
  if (!expStr) return {min: 0, max: 0};

  // 匹配形如"3-5年"的格式
  const matchYearsRange = expStr.match(/^(\d+)-(\d+)年/);
  if (matchYearsRange && matchYearsRange[1] && matchYearsRange[2]) {
    return {
      min: Number(matchYearsRange[1]),
      max: Number(matchYearsRange[2])
    };
  }

  // 匹配形如"3年以上"的格式
  const matchYearsAbove = expStr.match(/^(\d+)年以上/);
  if (matchYearsAbove && matchYearsAbove[1]) {
    const minYears = Number(matchYearsAbove[1]);
    return {
      min: minYears,
      max: minYears + 2 // 为"以上"添加一个合理的范围
    };
  }

  // 匹配形如"1年以下"的格式
  const matchYearsBelow = expStr.match(/^(\d+)年以下/);
  if (matchYearsBelow && matchYearsBelow[1]) {
    const maxYears = Number(matchYearsBelow[1]);
    return {
      min: 0,
      max: maxYears
    };
  }

  // 处理特殊情况
  if (expStr.includes('应届') || expStr.includes('实习') || expStr.includes('在校')) {
    return {min: 0, max: 1};
  }

  if (expStr.includes('不限')) {
    return {min: 0, max: 0};
  }

  // 简单匹配单个年数
  const matchYears = expStr.match(/(\d+)年/);
  if (matchYears && matchYears[1]) {
    const years = Number(matchYears[1]);
    return {min: years, max: years};
  }

  return {min: 0, max: 0};
};

// 定义抖动因子数组
const JITTER_FACTORS = [0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.7, 0.8];

// 获取随机抖动因子
const getRandomJitterFactor = (): number => {
  const randomIndex = Math.floor(Math.random() * JITTER_FACTORS.length);
  return JITTER_FACTORS[randomIndex];
};

// 解析薪资字符串，提取最低薪资值
const parseSalary = (salaryStr: string): number => {
  if (!salaryStr) return 0;

  // 移除可能的空格
  const cleanSalary = salaryStr.replace(/\s+/g, '');

  // 匹配模式：数字-数字K 或 数字K 或 数字-数字千 或 数字-数字万 或 数字千 或 数字万
  const matchK = cleanSalary.match(/^(\d+)(?:-\d+)?[Kk]$/);
  if (matchK && matchK[1]) {
    return Number(matchK[1]);
  }

  const matchThousand = cleanSalary.match(/^(\d+)(?:-\d+)?千$/);
  if (matchThousand && matchThousand[1]) {
    return Number(matchThousand[1]);
  }

  const matchTenThousand = cleanSalary.match(/^(\d+)(?:-\d+)?万$/);
  if (matchTenThousand && matchTenThousand[1]) {
    return Number(matchTenThousand[1]) * 10; // 转换为K单位
  }

  // 匹配月薪范围：数字K-数字K
  const matchRange = cleanSalary.match(/^(\d+)[Kk]-(\d+)[Kk]$/);
  if (matchRange && matchRange[1]) {
    return Number(matchRange[1]); // 返回范围最小值
  }

  // 如果是纯数字，则直接返回
  if (!isNaN(Number(cleanSalary))) {
    return Number(cleanSalary);
  }

  // 如果包含"面议"、"若干"等无法确定的值，返回0
  if (cleanSalary.includes('面议') || cleanSalary.includes('若干')) {
    return 0;
  }

  return 0;
};

// 将数值格式化为只保留一位小数，且只能是整数或.5结尾
const formatDataValue = (value: number): number => {
  // 四舍五入到一位小数
  const roundedValue = Math.round(value * 10) / 10;
  // 否则返回整数
  return roundedValue;
};

// 添加打开地图的方法
const openGoogleMaps = () => {
  if (companyInfo.address) {
    const searchQuery = encodeURIComponent(companyInfo.address);
    window.open(`https://www.google.com.hk/maps/search/${searchQuery}`, '_blank');
  }
};

// 生命周期钩子
onMounted(async () => {
  await fetchCitiesAndIndustries();
  await fetchData();
  initChart();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  if (chartInstance.value) {
    chartInstance.value.dispose();
  }
  window.removeEventListener('resize', handleResize);
});

// 方法
const fetchCitiesAndIndustries = async () => {
  try {
    // 使用API函数获取所有城市
    cities.value = await getCityNameList();

    // 使用API函数获取所有行业
    positions.value = await getPositionNameList();
  } catch (error) {
    console.error('获取筛选数据失败:', error);
  }
};

const fetchData = async () => {
  try {
    loading.value = true;

    // 使用getTwoDimensionalAnalysisChart API获取数据
    jobData.value = await getTwoDimensionalAnalysisChart(
      cityFilter.value,
      positionFilter.value,
      xAxis.value,
      yAxis.value
    );

    positions.value = await getPositionNameList(cityFilter.value);

    // 如果图表已初始化，更新图表
    if (chartInstance.value) {
      updateChart();
    }
  } catch (error) {
    console.error('获取数据失败:', error);
  } finally {
    loading.value = false;
  }
};

const initChart = () => {
  if (chartRef.value) {
    chartInstance.value = echarts.init(chartRef.value);
    updateChart();

    // 添加点击事件
    chartInstance.value.on('click', (params: echarts.ECElementEvent) => {
      handleChartClick(params);
    });
  }
};

const handleResize = () => {
  if (chartInstance.value) {
    chartInstance.value.resize();
  }
};

const getAxisLabel = (axis: string): string => {
  const labels: Record<string, string> = {
    'salary_value': '月薪 (K)',
    'degree_value': '学历要求',
    'experience_value': '经验要求 (年)'
  };
  return labels[axis] || axis;
};

const getVisualMap = (xAxisValue: string, yAxisValue: string): echarts.VisualMapComponentOption => {
  // 默认视觉映射配置
  const defaultVisualMap: echarts.VisualMapComponentOption = {
    show: true,
    type: 'continuous',
    min: 0,
    max: 50,
    dimension: xAxisValue === 'salary_value' ? 0 : 1,
    right: 0,
    top: 'center',
    calculable: true,
    text: ['高', '低'],
    orient: 'vertical',
    textStyle: {
      color: '#344767'
    }
  };

  // 根据轴类型决定视觉映射
  if (xAxisValue === 'salary_value' || yAxisValue === 'salary_value') {
    // 薪资视觉映射，蓝色系
    return {
      ...defaultVisualMap,
      text: ['月薪高', '月薪低'],
      inRange: {
        color: ['#93C5FD', '#2563EB']
      }
    };
  } else if (xAxisValue === 'experience_value' || yAxisValue === 'experience_value') {
    // 经验视觉映射，青色系
    return {
      ...defaultVisualMap,
      max: 10,
      text: ['经验丰富', '经验较少'],
      inRange: {
        color: ['#0EA5E9', '#38BDF8']
      }
    };
  } else {
    // 学历视觉映射，绿色系
    return {
      ...defaultVisualMap,
      max: 6,
      text: ['学历高', '学历低'],
      inRange: {
        color: ['#10B981', '#A7F3D0']
      }
    };
  }
};

// 使用元组类型定义散点图数据格式
type ChartDataPoint = [number, number, string, number | number[], number?];

const prepareChartData = (): ChartDataPoint[] => {
  const filtered = filteredData.value;
  const data: ChartDataPoint[] = [];

  // 根据不同的重叠处理方法准备数据
  if (overlapMethod.value === 'size') {
    // 使用点大小映射：将相同坐标的点聚合，并用大小表示数量
    const pointsMap = new Map<string, {x: number; y: number; count: number; indices: number[]}>();

    filtered.forEach((job, index) => {
      let xValue = 0;
      let yValue = 0;

      // 处理X轴数据
      if (xAxis.value === 'salary_value') {
        xValue = typeof job.salary_value === 'number' ? job.salary_value :
                (typeof job.salary === 'string' ? parseSalary(job.salary) : 0);
      } else if (xAxis.value === 'degree_value') {
        xValue = typeof job.degree_value === 'number' ? job.degree_value :
                (typeof job.degree === 'string' ? getDegreeValue(job.degree) : 0);
      } else if (xAxis.value === 'experience_value') {
        xValue = typeof job.experience_value === 'number' ? job.experience_value :
                (typeof job.experience === 'string' ? getExperienceValue(job.experience) : 0);
      } else {
        xValue = typeof job[xAxis.value as keyof typeof job] === 'number'
                ? Number(job[xAxis.value as keyof typeof job])
                : 0;
      }

      // 处理Y轴数据
      if (yAxis.value === 'salary_value') {
        yValue = typeof job.salary_value === 'number' ? job.salary_value :
                (typeof job.salary === 'string' ? parseSalary(job.salary) : 0);
      } else if (yAxis.value === 'degree_value') {
        yValue = typeof job.degree_value === 'number' ? job.degree_value :
                (typeof job.degree === 'string' ? getDegreeValue(job.degree) : 0);
      } else if (yAxis.value === 'experience_value') {
        yValue = typeof job.experience_value === 'number' ? job.experience_value :
                (typeof job.experience === 'string' ? getExperienceValue(job.experience) : 0);
      } else {
        yValue = typeof job[yAxis.value as keyof typeof job] === 'number'
                ? Number(job[yAxis.value as keyof typeof job])
                : 0;
      }

      const key = `${xValue},${yValue}`;

      if (!pointsMap.has(key)) {
        pointsMap.set(key, {
          x: xValue,
          y: yValue,
          count: 1,
          indices: [index]
        });
      } else {
        const point = pointsMap.get(key)!;
        point.count++;
        point.indices.push(index);
      }
    });

    // 转换为echarts数据格式，使用symbolSize表示点的数量
    data.push(...Array.from(pointsMap.values()).map(point => [
      point.x,
      point.y,
      filtered[point.indices[0]].position_name,
      point.indices,  // 保存所有重叠点的索引
      point.count     // 记录点的数量
    ] as ChartDataPoint));
  } else if (overlapMethod.value === 'opacity') {
    // 使用透明度：所有点都显示，但设置透明度使重叠区域可见
    filtered.forEach((job, index) => {
      let xValue = 0;
      let yValue = 0;

      // 处理X轴数据
      if (xAxis.value === 'salary_value') {
        xValue = typeof job.salary_value === 'number' ? job.salary_value :
                (typeof job.salary === 'string' ? parseSalary(job.salary) : 0);
      } else if (xAxis.value === 'degree_value') {
        xValue = typeof job.degree_value === 'number' ? job.degree_value :
                (typeof job.degree === 'string' ? getDegreeValue(job.degree) : 0);
      } else if (xAxis.value === 'experience_value') {
        xValue = typeof job.experience_value === 'number' ? job.experience_value :
                (typeof job.experience === 'string' ? getExperienceValue(job.experience) : 0);
      } else {
        xValue = typeof job[xAxis.value as keyof typeof job] === 'number'
                ? Number(job[xAxis.value as keyof typeof job])
                : 0;
      }

      // 处理Y轴数据
      if (yAxis.value === 'salary_value') {
        yValue = typeof job.salary_value === 'number' ? job.salary_value :
                (typeof job.salary === 'string' ? parseSalary(job.salary) : 0);
      } else if (yAxis.value === 'degree_value') {
        yValue = typeof job.degree_value === 'number' ? job.degree_value :
                (typeof job.degree === 'string' ? getDegreeValue(job.degree) : 0);
      } else if (yAxis.value === 'experience_value') {
        yValue = typeof job.experience_value === 'number' ? job.experience_value :
                (typeof job.experience === 'string' ? getExperienceValue(job.experience) : 0);
      } else {
        yValue = typeof job[yAxis.value as keyof typeof job] === 'number'
                ? Number(job[yAxis.value as keyof typeof job])
                : 0;
      }

      data.push([
        xValue,
        yValue,
        job.positionName,
        index
      ]);
    });
  } else {
    // 使用随机抖动：给每个点添加微小的随机偏移
    filtered.forEach((job, index) => {
      let xValue = 0;
      let yValue = 0;
      let xRange = {min: 0, max: 0};
      let yRange = {min: 0, max: 0};

      // 处理X轴数据
      if (xAxis.value === 'salary_value') {
        xValue = typeof job.salary_value === 'number' ? job.salary_value :
                (typeof job.salary === 'string' ? parseSalary(job.salary) : 0);
        // 获取薪资范围用于抖动
        if (typeof job.salary === 'string') {
          xRange = parseSalaryRange(job.salary);
        } else {
          xRange = {min: xValue, max: xValue};
        }
      } else if (xAxis.value === 'degree_value') {
        xValue = typeof job.degree_value === 'number' ? job.degree_value :
                (typeof job.degree === 'string' ? getDegreeValue(job.degree) : 0);
        xRange = {min: xValue, max: xValue};
      } else if (xAxis.value === 'experience_value') {
        xValue = typeof job.experience_value === 'number' ? job.experience_value :
                (typeof job.experience === 'string' ? getExperienceValue(job.experience) : 0);
        // 获取经验范围用于抖动
        if (typeof job.experience === 'string') {
          xRange = getExperienceRange(job.experience);
        } else {
          xRange = {min: xValue, max: xValue};
        }
      } else {
        xValue = typeof job[xAxis.value as keyof typeof job] === 'number'
                ? Number(job[xAxis.value as keyof typeof job])
                : 0;
        xRange = {min: xValue, max: xValue};
      }

      // 处理Y轴数据
      if (yAxis.value === 'salary_value') {
        yValue = typeof job.salary_value === 'number' ? job.salary_value :
                (typeof job.salary === 'string' ? parseSalary(job.salary) : 0);
        // 获取薪资范围用于抖动
        if (typeof job.salary === 'string') {
          yRange = parseSalaryRange(job.salary);
        } else {
          yRange = {min: yValue, max: yValue};
        }
      } else if (yAxis.value === 'degree_value') {
        yValue = typeof job.degree_value === 'number' ? job.degree_value :
                (typeof job.degree === 'string' ? getDegreeValue(job.degree) : 0);
        yRange = {min: yValue, max: yValue};
      } else if (yAxis.value === 'experience_value') {
        yValue = typeof job.experience_value === 'number' ? job.experience_value :
                (typeof job.experience === 'string' ? getExperienceValue(job.experience) : 0);
        // 获取经验范围用于抖动
        if (typeof job.experience === 'string') {
          yRange = getExperienceRange(job.experience);
        } else {
          yRange = {min: yValue, max: yValue};
        }
      } else {
        yValue = typeof job[yAxis.value as keyof typeof job] === 'number'
                ? Number(job[yAxis.value as keyof typeof job])
                : 0;
        yRange = {min: yValue, max: yValue};
      }

      // X轴抖动计算
      let jitterX: number;
      if (xRange.min !== xRange.max) {
        // 如果有范围，则在范围内抖动，只向正方向抖动
        const rangeWidth = xRange.max - xRange.min;
        jitterX = xRange.min + Math.random() * rangeWidth;
        // 格式化为整数或.5结尾
        jitterX = formatDataValue(jitterX);
      } else {
        // 为每个数据点单独获取随机抖动因子
        const jitterFactor = (xAxis.value === 'degree_value' || xAxis.value === 'experience_value')
          ? getRandomJitterFactor() // 每次调用都获取一个新的随机因子
          : 0.3;
        jitterX = xValue + Math.random() * jitterFactor;
        // 格式化为整数或.5结尾
        jitterX = formatDataValue(jitterX);
      }

      // Y轴抖动计算
      let jitterY: number;
      if (yRange.min !== yRange.max) {
        // 如果有范围，则在范围内抖动，只向正方向抖动
        const rangeWidth = yRange.max - yRange.min;
        jitterY = yRange.min + Math.random() * rangeWidth;
        // 格式化为整数或.5结尾
        jitterY = formatDataValue(jitterY);
      } else {
        // 为每个数据点单独获取随机抖动因子
        const jitterFactor = (yAxis.value === 'degree_value' || yAxis.value === 'experience_value')
          ? getRandomJitterFactor() // 每次调用都获取一个新的随机因子
          : 0.3;
        jitterY = yValue + Math.random() * jitterFactor;
        // 格式化为整数或.5结尾
        jitterY = formatDataValue(jitterY);
      }

      data.push([
        jitterX,
        jitterY,
        job.positionName,
        index
      ]);
    });
  }

  return data;
};

const updateChart = () => {
  if (!chartInstance.value) return;

  const data = prepareChartData();
  const xAxisValue = xAxis.value;
  const yAxisValue = yAxis.value;

  const option: EChartsOption = {
    title: {
      text: '职位分析(薪资为月工资)',
      left: 'center',
      top: 0,
      textStyle: {
        color: '#344767'
      }
    },
    tooltip: {
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderWidth: 1,
      borderColor: '#E5E7EB',
      textStyle: {
        color: '#344767',
        fontSize: 14
      },
      padding: [16, 20],
      extraCssText: 'box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1); border-radius: 12px;',
      appendToBody: true,
      formatter: function(params) {
        // 确保params是单个数据点
        const param = Array.isArray(params) ? params[0] : params;
        // 现在可以安全地访问data属性
        const pointData = param.data as unknown as ChartDataPoint;

        // 格式化X和Y坐标值
        const xCoord = formatDataValue(pointData[0]);
        const yCoord = formatDataValue(pointData[1]);

        // 坐标信息文本
        const coordInfo = `X: ${xCoord}, Y: ${yCoord}`;

        if (overlapMethod.value === 'size' && Array.isArray(pointData[3])) {
          // 对于聚合点，显示重叠数量和第一个职位的详细信息
          const indices = pointData[3] as number[];
          const count = indices.length;
          const job = filteredData.value[indices[0]];
          return `
              <div style="padding: 16px; color: #344767;">
                <div style="font-weight: bold; font-size: 18px; margin-bottom: 12px; color: #344767;">${job.positionName || '未知职位'}</div>

                <div style="margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #E5E7EB;">
                  <div style="color: #67748E; margin-bottom: 8px; font-size: 12px;">${coordInfo}</div>
                  <span style="color: #3B82F6; font-size: 16px; font-weight: bold; background: rgba(59, 130, 246, 0.1); padding: 4px 8px; border-radius: 4px;">此位置有 ${count} 个职位</span>
                </div>

                <div style="margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #E5E7EB;">
                  <div style="color: #67748E; margin-bottom: 8px; font-size: 13px;">公司信息</div>
                  <div style="color: #344767; font-weight: 500; font-size: 15px;">${job.companyName || '未知公司'}</div>
                </div>

                <div style="background: rgba(243, 244, 246, 0.5); border-radius: 8px; padding: 12px; margin-bottom: 12px;">
                  <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px;">
                    <div>
                      <div style="color: #67748E; font-size: 12px; margin-bottom: 4px;">薪资</div>
                      <div style="color: #3B82F6; font-weight: 500;">${job.salary || '未知'}</div>
                    </div>
                    <div>
                      <div style="color: #67748E; font-size: 12px; margin-bottom: 4px;">学历</div>
                      <div style="color: #344767; font-weight: 500;">${job.degree || '未知'}</div>
                    </div>
                    <div>
                      <div style="color: #67748E; font-size: 12px; margin-bottom: 4px;">经验</div>
                      <div style="color: #344767; font-weight: 500;">${job.experience || '未知'}</div>
                    </div>
                    <div>
                      <div style="color: #67748E; font-size: 12px; margin-bottom: 4px;">城市</div>
                      <div style="color: #344767; font-weight: 500;">${job.cityName || '未知'}</div>
                    </div>
                  </div>
                </div>

                <div style="text-align: center; margin-top: 12px;">
                  <span style="color:#3B82F6; font-weight: 500; cursor: pointer; background: rgba(59, 130, 246, 0.1); padding: 4px 10px; border-radius: 4px;">点击查看详情</span>
                </div>
              </div>
            </div>
          `;
        } else {
          // 普通点
          const jobIndex = pointData[3] as number;
          const job = filteredData.value[jobIndex];
          return `
              <div style="padding: 16px; color: #344767;">
                <div style="font-weight: bold; font-size: 18px; margin-bottom: 12px; color: #344767;">${job.positionName || '未知职位'}</div>

                <div style="margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #E5E7EB;">
                  <div style="color: #67748E; margin-bottom: 8px; font-size: 12px;">${coordInfo}</div>
                </div>

                <div style="margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #E5E7EB;">
                  <div style="color: #67748E; margin-bottom: 8px; font-size: 13px;">公司信息</div>
                  <div style="color: #344767; font-weight: 500; font-size: 15px;">${job.companyName || '未知公司'}</div>
                </div>

                <div style="background: rgba(243, 244, 246, 0.5); border-radius: 8px; padding: 12px; margin-bottom: 12px;">
                  <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px;">
                    <div>
                      <div style="color: #67748E; font-size: 12px; margin-bottom: 4px;">薪资</div>
                      <div style="color: #3B82F6; font-weight: 500;">${job.salary || '未知'}</div>
                    </div>
                    <div>
                      <div style="color: #67748E; font-size: 12px; margin-bottom: 4px;">学历</div>
                      <div style="color: #344767; font-weight: 500;">${job.degree || '未知'}</div>
                    </div>
                    <div>
                      <div style="color: #67748E; font-size: 12px; margin-bottom: 4px;">经验</div>
                      <div style="color: #344767; font-weight: 500;">${job.experience || '未知'}</div>
                    </div>
                    <div>
                      <div style="color: #67748E; font-size: 12px; margin-bottom: 4px;">城市</div>
                      <div style="color: #344767; font-weight: 500;">${job.cityName || '未知'}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          `;
        }
      }
    },
    visualMap: getVisualMap(xAxisValue, yAxisValue),
    grid: {
      left: '3%',
      right: '15%',
      bottom: '10%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      name: getAxisLabel(xAxisValue),
      nameLocation: 'middle',
      nameGap: 30,
      type: 'value',
      axisLabel: {
        color: '#67748E',
        formatter: function(value: number) {
          // 格式化轴上的数值为整数或.5结尾
          const formattedValue = formatDataValue(value);

          if (xAxisValue === 'salary_value') {
            if (formattedValue >= 10) {
              return (formattedValue/10).toFixed(1) + '万';
            }
            return formattedValue.toFixed(1) + 'K';
          }
          if (xAxisValue === 'degree_value') {
            const index = Math.round(formattedValue);
            if (index >= 0 && index < DEGREES_ARRAY.length) {
              return DEGREES_ARRAY[index];
            }
            return formattedValue.toFixed(1);
          }
          if (xAxisValue === 'experience_value') {
            return formattedValue.toFixed(1) + '年';
          }
          return formattedValue.toFixed(1);
        }
      },
      axisLine: {
        lineStyle: {
          color: '#E5E7EB'
        }
      },
      splitLine: {
        lineStyle: {
          type: 'dashed',
          color: '#E5E7EB'
        }
      },
      nameTextStyle: {
        color: '#344767'
      },
      min: function(value: {min: number; max: number}) {
        if (xAxisValue === 'salary_value') {
          return 0;
        }
        return value.min;
      },
      max: function(value: {min: number; max: number}) {
        if (xAxisValue === 'salary_value') {
          return Math.min(100, value.max);
        }
        return value.max;
      },
      splitNumber: xAxisValue === 'salary_value' ? 10 : 5
    },
    yAxis: {
      name: getAxisLabel(yAxisValue),
      nameRotate: 90,
      nameLocation: 'middle',
      nameGap: 55,
      type: 'value',
      axisLabel: {
        color: '#67748E',
        formatter: function(value: number) {
          // 格式化轴上的数值为整数或.5结尾
          const formattedValue = formatDataValue(value);

          if (yAxisValue === 'salary_value') {
            if (formattedValue >= 10) {
              return (formattedValue/10).toFixed(1) + '万';
            }
            return formattedValue.toFixed(1) + 'K';
          }
          if (yAxisValue === 'degree_value') {
            const index = Math.round(formattedValue);
            if (index >= 0 && index < DEGREES_ARRAY.length) {
              return DEGREES_ARRAY[index];
            }
            return formattedValue.toFixed(1);
          }
          if (yAxisValue === 'experience_value') {
            return formattedValue.toFixed(1) + '年';
          }
          return formattedValue.toFixed(1);
        }
      },
      axisLine: {
        lineStyle: {
          color: '#E5E7EB'
        }
      },
      splitLine: {
        lineStyle: {
          type: 'dashed',
          color: '#E5E7EB'
        }
      },
      nameTextStyle: {
        color: '#344767'
      },
      min: function(value: {min: number; max: number}) {
        if (yAxisValue === 'salary_value') {
          return 0;
        }
        return value.min;
      },
      max: function(value: {min: number; max: number}) {
        if (yAxisValue === 'salary_value') {
          return Math.min(100, value.max);
        }
        return value.max;
      },
      splitNumber: yAxisValue === 'degree_value' ? 6 : 5
    },
    series: [
      {
        symbolSize: function(val) {
          if (overlapMethod.value === 'size' && Array.isArray(val[3])) {
            // 根据点数量设置大小，最小10，最大30
            const count = val[4] || (val[3] as number[]).length;
            return Math.max(10, Math.min(30, 10 + count * 2));
          }
          return 10;
        },
        data: data,
        type: 'scatter',
        itemStyle: {
          color: function(params) {
            // 确保安全访问data
            if (!params.data) return 'rgba(100, 100, 100, 0.8)';

            const dataArray = params.data as number[];
            const x = dataArray[0];
            const y = dataArray[1];

            let value;
            if (xAxisValue === 'salary_value' || yAxisValue === 'salary_value') {
              value = xAxisValue === 'salary_value' ? x : y;
              const intensity = Math.min(1, value / 50);
              // 设置透明度
              const opacity = overlapMethod.value === 'opacity' ? 0.7 : 0.9;
              return `rgba(${59 + 50 * intensity}, ${130 + 30 * intensity}, ${246 - 20 * intensity}, ${opacity})`;
            } else {
              value = (x + y) / 2;
              // 设置透明度
              const opacity = overlapMethod.value === 'opacity' ? 0.7 : 0.9;
              return `rgba(${96 + 40 * value/10}, ${165 + 30 * value/10}, ${250 - 10 * value/10}, ${opacity})`;
            }
          },
          borderColor: '#fff',
          borderWidth: 1
        },
        emphasis: {
          itemStyle: {
            borderColor: '#fff',
            borderWidth: 2,
            shadowBlur: 10,
            shadowColor: 'rgba(59, 130, 246, 0.3)'
          }
        }
      } as echarts.ScatterSeriesOption
    ]
  };

  chartInstance.value.setOption(option);
};

const handleChartClick = (params: echarts.ECElementEvent) => {
  const pointData = params.data as unknown as ChartDataPoint;
  let jobIndex: number;

  if (overlapMethod.value === 'size' && Array.isArray(pointData[3])) {
    // 对于聚合点，使用第一个职位的详细信息
    const indices = pointData[3] as number[];
    jobIndex = indices[0];
  } else {
    // 普通点
    jobIndex = pointData[3] as number;
  }

  const job = filteredData.value[jobIndex];

  if (!job) return;

  // 更新公司详情
  companyInfo.companyName = String(job.companyName || '未知');
  companyInfo.positionName = String(job.positionName || '未知');
  companyInfo.cityName = String(job.cityName || '未知');
  companyInfo.salary = String(job.salary || '未知');
  companyInfo.degree = String(job.degree || '未知');
  companyInfo.experience = String(job.experience || '未知');
  companyInfo.companySize = String(job.companySize || '未知');
  companyInfo.financingStage = String(job.financingStage || '未知');
  companyInfo.companyUrl = String(job.companyUrl || '');
  companyInfo.jobUrl = String(job.jobUrl || '');
  companyInfo.employeeBenefits = Array.isArray(job.employeeBenefits) ? job.employeeBenefits : [];
  companyInfo.jobRequirements = Array.isArray(job.jobRequirements) ? job.jobRequirements : [];
  companyInfo.address = String(job.address || '');
  // 显示公司详情区域
  showDetails.value = true;
};
</script>

<style lang="scss" scoped>
.scatter-plot-card {
  margin: var(--spacing-md);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--box-shadow);
  background-color: var(--card-color);
  transition: box-shadow 0.3s ease;
  overflow: hidden;

  &:hover {
    box-shadow: var(--box-shadow-lg);
  }
}

.scatter-header {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--border-color);
  margin-bottom: var(--spacing-md);

  .scatter-title {
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

.filter-row {
  margin-top: var(--spacing-md);
  padding: 0 var(--spacing-lg);
  display: flex;
  flex-wrap: nowrap;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-md);
  justify-content: space-between;

  @media (max-width: 768px) {
    padding: 0 var(--spacing-sm);
    gap: 5px;

    :deep(.el-form-item__label) {
      font-size: 12px;
      padding-right: 5px;
    }

    :deep(.el-select) {
      width: 100%;
    }

    :deep(.el-input__wrapper) {
      padding: 0 6px;
    }
  }
}

.el-col {
  transition: all 0.3s ease;
  padding: 0 5px;

  &:hover {
    transform: translateY(-2px);
  }

  :deep(.el-form-item__label) {
    white-space: nowrap;
  }
}

.el-form-item {
  margin-bottom: var(--spacing-md);

  :deep(.el-form-item__label) {
    font-weight: 500;
    color: var(--text-primary);
  }
}

.el-select {
  width: 100%;

  :deep(.el-input__wrapper) {
    box-shadow: 0 0 0 1px var(--border-color) inset;
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 0 0 1px var(--primary-color) inset;
    }

    &.is-focus {
      box-shadow: 0 0 0 1px var(--primary-color) inset;
    }
  }
}

.data-status-alert {
  margin: var(--spacing-md) var(--spacing-lg);
  border-radius: var(--border-radius-md);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
}

.chart-container {
  padding: var(--spacing-md);

  .chart-content {
    height: 550px;
    width: 100%;
    border-radius: var(--border-radius-md);
    background-color: #FFFFFF;
    padding: var(--spacing-md);
    box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.05);
    position: relative;
    overflow: hidden;

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

// 打印样式优化
@media print {
  .scatter-plot-card {
    box-shadow: none !important;
    margin: 0 !important;

    .chart-container .chart-content {
      height: 100% !important;
      page-break-inside: avoid;
    }
  }
}
</style>




