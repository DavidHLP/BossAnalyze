<template>
  <div class="dashboard-view">
    <h1 class="page-title">系统监控仪表盘</h1>

    <!-- 系统状态概览卡片 -->
    <el-row :gutter="20" class="data-overview">
      <el-col :xs="24" :sm="12" :md="6" v-for="(card, index) in overviewCards" :key="index">
        <el-card shadow="hover" class="overview-card" :body-style="{ padding: '20px' }">
          <div class="card-content">
            <el-icon class="card-icon" :style="{ color: card.color }">
              <component :is="card.icon" />
            </el-icon>
            <div class="card-info">
              <h3 class="card-title">{{ card.title }}</h3>
              <p class="card-value">{{ card.value }}</p>
              <p class="card-change" :class="card.status">
                <el-tag :type="card.status === 'normal' ? 'success' : 'danger'" size="small">
                  {{ card.statusText }}
                </el-tag>
              </p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- API监控图表区域 -->
    <el-row :gutter="20" class="chart-section">
      <el-col :xs="24" :md="16">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="chart-header">
              <h3>API请求监控</h3>
              <el-radio-group v-model="timeRange" size="small">
                <el-radio-button label="realtime">实时</el-radio-button>
                <el-radio-button label="hour">1小时</el-radio-button>
                <el-radio-button label="day">24小时</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="chart-container" ref="apiChartRef"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="chart-header">
              <h3>请求状态分布</h3>
            </div>
          </template>
          <div class="chart-container" ref="statusChartRef"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 路由监控和系统信息 -->
    <el-row :gutter="20" class="bottom-section">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="route-monitor">
          <template #header>
            <div class="card-header">
              <h3>路由监控</h3>
              <el-button type="primary" size="small" @click="refreshRouteData">刷新</el-button>
            </div>
          </template>
          <el-table :data="routeMonitorData" style="width: 100%" height="350">
            <el-table-column prop="path" label="路径" width="180" />
            <el-table-column prop="method" label="方法" width="100">
              <template #default="scope">
                <el-tag :type="getMethodTagType(scope.row.method)">{{ scope.row.method }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="avgTime" label="平均响应时间(ms)" width="150" />
            <el-table-column prop="count" label="访问次数" width="100" />
            <el-table-column prop="status" label="状态">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'normal' ? 'success' : 'danger'">
                  {{ scope.row.status === 'normal' ? '正常' : '异常' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="system-info">
          <template #header>
            <div class="card-header">
              <h3>系统信息</h3>
              <el-tag type="success">运行中</el-tag>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="服务器地址">{{ systemInfo.host }}</el-descriptions-item>
            <el-descriptions-item label="运行时长">{{ systemInfo.uptime }}</el-descriptions-item>
            <el-descriptions-item label="CPU使用率">
              <el-progress :percentage="systemInfo.cpuUsage" :status="getCpuStatus(systemInfo.cpuUsage)" />
            </el-descriptions-item>
            <el-descriptions-item label="内存使用率">
              <el-progress :percentage="systemInfo.memoryUsage" :status="getMemoryStatus(systemInfo.memoryUsage)" />
            </el-descriptions-item>
            <el-descriptions-item label="系统版本">{{ systemInfo.version }}</el-descriptions-item>
            <el-descriptions-item label="最后更新时间">{{ systemInfo.lastUpdateTime }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" setup name="DashboardView">
import { ref, onMounted, onUnmounted, shallowRef, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { Monitor, Connection, Timer, Warning } from '@element-plus/icons-vue'
import type { EChartsOption } from 'echarts'

// 系统概览卡片数据
const overviewCards = ref([
  {
    title: '服务在线数',
    value: '8/10',
    status: 'warning',
    statusText: '2个服务离线',
    icon: 'Monitor',
    color: '#409EFF'
  },
  {
    title: '平均响应时间',
    value: '126ms',
    status: 'normal',
    statusText: '正常',
    icon: 'Timer',
    color: '#67C23A'
  },
  {
    title: '实时请求量',
    value: '1,284',
    status: 'normal',
    statusText: '正常',
    icon: 'Connection',
    color: '#E6A23C'
  },
  {
    title: '错误率',
    value: '0.5%',
    status: 'normal',
    statusText: '正常',
    icon: 'Warning',
    color: '#F56C6C'
  }
])

// 路由监控数据
const routeMonitorData = ref([
  {
    path: '/api/users',
    method: 'GET',
    avgTime: 85,
    count: 1562,
    status: 'normal'
  },
  {
    path: '/api/auth',
    method: 'POST',
    avgTime: 150,
    count: 856,
    status: 'normal'
  },
  {
    path: '/api/products',
    method: 'GET',
    avgTime: 95,
    count: 2341,
    status: 'normal'
  },
  {
    path: '/api/orders',
    method: 'POST',
    avgTime: 320,
    count: 432,
    status: 'warning'
  }
])

// 系统信息
const systemInfo = ref({
  host: 'http://localhost:8080',
  uptime: '15天 6小时 32分钟',
  cpuUsage: 45,
  memoryUsage: 62,
  version: 'v1.2.0',
  lastUpdateTime: '2024-03-15 14:30:00'
})

// 图表相关
const apiChartRef = ref()
const statusChartRef = ref()
const timeRange = ref('hour')
const apiChart = shallowRef<echarts.ECharts | null>(null)
const statusChart = shallowRef<echarts.ECharts | null>(null)

// 初始化API监控图表
const initApiChart = () => {
  if (!apiChartRef.value) return

  apiChart.value = echarts.init(apiChartRef.value)
  const option: EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['请求数', '响应时间']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: generateTimeData()
    },
    yAxis: [
      {
        type: 'value',
        name: '请求数',
        position: 'left'
      },
      {
        type: 'value',
        name: '响应时间(ms)',
        position: 'right'
      }
    ],
    series: [
      {
        name: '请求数',
        type: 'line',
        data: generateRandomData(60, 100, 500),
        smooth: true,
        areaStyle: {}
      },
      {
        name: '响应时间',
        type: 'line',
        yAxisIndex: 1,
        data: generateRandomData(60, 50, 200),
        smooth: true
      }
    ]
  }

  apiChart.value.setOption(option)
}

// 初始化状态分布图表
const initStatusChart = () => {
  if (!statusChartRef.value) return

  statusChart.value = echarts.init(statusChartRef.value)
  const option: EChartsOption = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center'
    },
    series: [
      {
        name: '请求状态',
        type: 'pie',
        radius: ['50%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '14',
            fontWeight: 'bold'
          }
        },
        data: [
          { value: 1048, name: '200 成功' },
          { value: 235, name: '404 未找到' },
          { value: 148, name: '500 服务器错误' },
          { value: 84, name: '403 未授权' }
        ]
      }
    ]
  }

  statusChart.value.setOption(option)
}

// 辅助函数
const generateTimeData = () => {
  const now = new Date()
  const times = []
  for (let i = 59; i >= 0; i--) {
    const time = new Date(now.getTime() - i * 60000)
    times.push(`${time.getHours().toString().padStart(2, '0')}:${time.getMinutes().toString().padStart(2, '0')}`)
  }
  return times
}

const generateRandomData = (count: number, min: number, max: number) => {
  return Array.from({ length: count }, () => Math.floor(Math.random() * (max - min + 1) + min))
}

const getMethodTagType = (method: string) => {
  const types: Record<string, string> = {
    GET: 'success',
    POST: 'warning',
    PUT: 'primary',
    DELETE: 'danger'
  }
  return types[method] || 'info'
}

const getCpuStatus = (usage: number) => usage > 80 ? 'exception' : usage > 60 ? 'warning' : 'success'
const getMemoryStatus = (usage: number) => usage > 85 ? 'exception' : usage > 70 ? 'warning' : 'success'

const refreshRouteData = () => {
  // 这里添加刷新路由监控数据的逻辑
}

// 生命周期钩子
onMounted(() => {
  nextTick(() => {
    initApiChart()
    initStatusChart()
    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  apiChart.value?.dispose()
  statusChart.value?.dispose()
})

// 窗口大小变化处理
const handleResize = () => {
  apiChart.value?.resize()
  statusChart.value?.resize()
}

// 监听时间范围变化
watch(timeRange, (newValue) => {
  // 根据时间范围更新图表数据
  const updateChartData = () => {
    if (!apiChart.value) return

    const option = apiChart.value.getOption()
    let xData: string[] = []
    let requestData: number[] = []
    let responseTimeData: number[] = []

    switch (newValue) {
      case 'realtime':
        xData = generateTimeData()
        requestData = generateRandomData(60, 100, 500)
        responseTimeData = generateRandomData(60, 50, 200)
        break
      case 'hour':
        xData = Array.from({ length: 60 }, (_, i) => `${59 - i}分钟前`)
        requestData = generateRandomData(60, 200, 800)
        responseTimeData = generateRandomData(60, 80, 300)
        break
      case 'day':
        xData = Array.from({ length: 24 }, (_, i) => `${23 - i}小时前`)
        requestData = generateRandomData(24, 1000, 5000)
        responseTimeData = generateRandomData(24, 100, 400)
        break
    }

    apiChart.value.setOption({
      xAxis: {
        data: xData
      },
      series: [
        {
          name: '请求数',
          data: requestData
        },
        {
          name: '响应时间',
          data: responseTimeData
        }
      ]
    })
  }

  updateChartData()
})
</script>

<style lang="scss" scoped>
.dashboard-view {
  padding: 20px;
}

.page-title {
  font-size: 24px;
  margin-bottom: 24px;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.data-overview {
  margin-bottom: 20px;
}

.overview-card {
  height: 120px;
  transition: transform 0.3s;

  &:hover {
    transform: translateY(-5px);
  }

  .card-content {
    display: flex;
    align-items: center;
  }

  .card-icon {
    font-size: 48px;
    margin-right: 16px;
  }

  .card-info {
    flex: 1;
  }

  .card-title {
    font-size: 14px;
    color: var(--el-text-color-secondary);
    margin: 0 0 8px;
  }

  .card-value {
    font-size: 24px;
    font-weight: 600;
    margin: 0 0 4px;
    color: var(--el-text-color-primary);
  }

  .card-change {
    margin: 0;
  }
}

.chart-section,
.bottom-section {
  margin-bottom: 20px;
}

.chart-card {
  margin-bottom: 20px;

  .chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 500;
    }
  }

  .chart-container {
    height: 350px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 500;
  }
}

.route-monitor {
  .el-table {
    margin-top: 10px;
  }
}

.system-info {
  :deep(.el-descriptions__cell) {
    padding: 12px;
  }

  :deep(.el-progress) {
    margin-right: 20px;
  }
}
</style>
