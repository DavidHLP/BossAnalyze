<template>
  <div class="dashboard-layout" :class="{ 'dashboard-loading': loading }">
    <!-- 加载状态 -->
    <Transition name="loading">
      <div v-if="loading" class="loading-state">
        <div class="loading-content">
          <div class="loading-spinner"></div>
          <p class="loading-text">正在加载仪表板数据...</p>
        </div>
      </div>
    </Transition>

    <!-- 仪表板内容 -->
    <Transition name="dashboard" appear>
      <div v-if="!loading" class="dashboard-grid">
        <!-- 头部指标区域 -->
        <section class="metrics-area" aria-label="关键指标">
          <div class="metrics-grid">
            <article
              v-for="metric in metrics"
              :key="metric.key"
              class="metric-card"
              :class="`metric-${metric.type}`"
            >
              <div class="metric-icon" :class="`icon-${metric.type}`">
                <el-icon>
                  <component :is="metric.icon" />
                </el-icon>
              </div>
              <div class="metric-info">
                <div class="metric-value">{{ metric.value }}</div>
                <div class="metric-label">{{ metric.label }}</div>
                <div class="metric-trend" v-if="metric.trend">
                  <span :class="metric.trend.type">{{ metric.trend.text }}</span>
                </div>
              </div>
            </article>
          </div>
        </section>

        <!-- 图表区域 -->
        <section class="charts-area" aria-label="数据可视化">
          <!-- 主要图表区域 -->
          <div class="charts-primary">
            <article class="chart-wrapper heatmap-chart">
              <header class="chart-header">
                <h3 class="chart-title">访问量热力图</h3>
                <div class="chart-actions">
                  <el-tooltip content="显示每天每小时的访问量分布" placement="top">
                    <el-button link size="small">
                      <el-icon><InfoFilled /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-button link size="small" @click="refreshChart('heatmap')">
                    <el-icon><Refresh /></el-icon>
                  </el-button>
                </div>
              </header>
              <div
                ref="heatmapRef"
                class="chart-container"
                role="img"
                aria-label="访问量热力图"
              ></div>
            </article>

            <article class="chart-wrapper browser-chart">
              <header class="chart-header">
                <h3 class="chart-title">浏览器分布</h3>
                <div class="chart-actions">
                  <el-tooltip content="访问者使用的浏览器类型统计" placement="top">
                    <el-button link size="small">
                      <el-icon><InfoFilled /></el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
              </header>
              <div
                ref="browserChartRef"
                class="chart-container"
                role="img"
                aria-label="浏览器分布图"
              ></div>
            </article>
          </div>

          <!-- 次要图表区域 -->
          <div class="charts-secondary">
            <article class="chart-wrapper http-chart">
              <header class="chart-header">
                <h3 class="chart-title">HTTP方法</h3>
                <div class="chart-actions">
                  <el-tooltip content="请求使用的HTTP方法统计" placement="top">
                    <el-button link size="small">
                      <el-icon><InfoFilled /></el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
              </header>
              <div
                ref="httpMethodChartRef"
                class="chart-container"
                role="img"
                aria-label="HTTP方法分布图"
              ></div>
            </article>

            <article class="chart-wrapper trend-chart">
              <header class="chart-header">
                <h3 class="chart-title">访问趋势</h3>
                <div class="chart-actions">
                  <el-tooltip content="一周内各天的访问量趋势" placement="top">
                    <el-button link size="small">
                      <el-icon><InfoFilled /></el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
              </header>
              <div
                ref="weekdayChartRef"
                class="chart-container"
                role="img"
                aria-label="访问趋势图"
              ></div>
            </article>
          </div>
        </section>

        <!-- 数据表格区域 -->
        <!-- <section class="data-area" aria-label="详细数据">
          <article class="data-card">
            <header class="data-header">
              <h3 class="data-title">地理位置统计</h3>
              <div class="data-actions">
                <el-input
                  v-model="searchQuery"
                  placeholder="搜索国家或地区"
                  clearable
                  size="small"
                  class="search-input"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <el-button type="primary" size="small" @click="exportData">
                  <el-icon><Download /></el-icon>
                  导出
                </el-button>
              </div>
            </header>
            <div
              ref="ipStatsChartRef"
              class="chart-container geo-chart"
              role="img"
              aria-label="地理位置统计图"
            ></div>
          </article>
        </section> -->
      </div>
    </Transition>

    <!-- IP详情侧边栏 -->
    <Transition name="drawer">
      <aside v-if="ipDrawerVisible" class="ip-drawer" role="dialog" aria-labelledby="drawer-title">
        <div class="drawer-overlay" @click="closeIpDrawer"></div>
        <div class="drawer-panel">
          <header class="drawer-header">
            <h3 id="drawer-title" class="drawer-title">
              <el-icon><Location /></el-icon>
              IP详情: {{ selectedIp?.ip || '' }}
            </h3>
            <el-button link @click="closeIpDrawer">
              <el-icon><Close /></el-icon>
            </el-button>
          </header>

          <main class="drawer-body" v-if="selectedIp">
            <div class="ip-summary">
              <div class="summary-grid">
                <div class="summary-item">
                  <label>IP地址</label>
                  <el-tag type="primary">{{ selectedIp.ip }}</el-tag>
                </div>
                <div class="summary-item">
                  <label>访问次数</label>
                  <el-tag type="success">{{ selectedIp.count }}</el-tag>
                </div>
                <div class="summary-item">
                  <label>国家/地区</label>
                  <span>{{ selectedIp.country_name || '未知' }}</span>
                </div>
                <div class="summary-item">
                  <label>城市</label>
                  <span>{{ selectedIp.city || '未知' }}</span>
                </div>
              </div>
            </div>

            <el-divider>
              <el-icon><Clock /></el-icon>
              访问记录
            </el-divider>

            <div class="access-records">
              <el-empty description="暂无详细记录" :image-size="100">
                <template #image>
                  <el-icon size="100"><Document /></el-icon>
                </template>
              </el-empty>
            </div>
          </main>
        </div>
      </aside>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, reactive, computed, watch } from 'vue'
import { getAccessLogStats } from '@/api/log/accesslog'
import type { AccessLogStats } from '@/api/log/accesslog.d'
import {
  Monitor,
  User,
  Platform,
  Connection,
  InfoFilled,
  Refresh,
  Search,
  Download,
  Location,
  Close,
  Clock,
  Document,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'

interface IpTableItem {
  ip: string
  count: number
  country_name?: string
  city?: string
}
// 响应式状态
const loading = ref(true)
const statsData = reactive<AccessLogStats>({
  totalVisits: 0,
  uniqueIps: 0,
  avgResponseTime: 0,
  errorRate: 0,
  heatmap: { minute_heatmap: {}, hour_heatmap: {} },
  ipStats: {},
  httpMethods: {},
  weekdayStats: {},
  browserStats: {},
})

// 日期过滤功能
const dateRange = ref<string[]>([])

// IP表格分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const ipSearchQuery = ref('')

// 添加表格排序功能和修复分页选择问题
const sortOrder = ref<'ascending' | 'descending'>('descending')
const sortBy = ref<string>('count')

// 表格排序
const sortTable = (column: string) => {
  if (sortBy.value === column) {
    sortOrder.value = sortOrder.value === 'ascending' ? 'descending' : 'ascending'
  } else {
    sortBy.value = column
    sortOrder.value = 'descending'
  }
}

// 处理分页变化（修复）
const handleSizeChange = (val: number | string) => {
  pageSize.value = Number(val)
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
}

// 导出IP数据
const exportIpData = () => {
  // 实现导出功能
  console.log('导出IP数据')
}

// 图表引用
const heatmapRef = ref<HTMLDivElement | null>(null)
const browserChartRef = ref<HTMLDivElement | null>(null)
const httpMethodChartRef = ref<HTMLDivElement | null>(null)
const weekdayChartRef = ref<HTMLDivElement | null>(null)
const ipStatsChartRef = ref<HTMLDivElement | null>(null)

// 图表实例
let heatmapChart: echarts.ECharts | null = null
let browserChart: echarts.ECharts | null = null
let httpMethodChart: echarts.ECharts | null = null
let weekdayChart: echarts.ECharts | null = null
let ipStatsChart: echarts.ECharts | null = null

// IP详情抽屉
const ipDrawerVisible = ref(false)
const selectedIp = ref<IpTableItem | null>(null)

// 新增的响应式状态
const searchQuery = ref('')

// 指标数据计算
const metrics = computed(() => [
  {
    key: 'visits',
    type: 'primary',
    icon: Monitor,
    value: statsData.totalVisits,
    label: '总访问量',
    trend: { type: 'up', text: '+12%' },
  },
  {
    key: 'ips',
    type: 'success',
    icon: User,
    value: statsData.uniqueIps,
    label: '独立IP数',
    trend: { type: 'up', text: '+8%' },
  },
  {
    key: 'browsers',
    type: 'warning',
    icon: Platform,
    value: Object.keys(statsData.browserStats).length,
    label: '浏览器类型',
  },
  {
    key: 'methods',
    type: 'info',
    icon: Connection,
    value: Object.keys(statsData.httpMethods).length,
    label: 'HTTP方法',
  },
])

// 新增方法
const refreshChart = (chartType: string) => {
  console.log('刷新图表:', chartType)
  // 这里可以添加具体的图表刷新逻辑
}

const exportData = () => {
  console.log('导出数据')
  // 这里可以添加数据导出逻辑
}

// 转换IP统计数据为表格数据
const ipTableData = computed(() => {
  return Object.entries(statsData.ipStats).map(([ip, data]) => ({
    ip,
    count: data.count,
    country_name: data.country_name || '未知',
    city: data.city || '未知',
  }))
})

// 过滤后的IP表格数据
const filteredIpTableData = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize.value
  let data = ipTableData.value

  // 搜索过滤
  if (ipSearchQuery.value) {
    data = data.filter(
      (item) =>
        item.ip.includes(ipSearchQuery.value) ||
        item.country_name?.includes(ipSearchQuery.value) ||
        item.city?.includes(ipSearchQuery.value),
    )
  }

  // 分页
  return data.slice(startIndex, startIndex + pageSize.value)
})

// 按排序规则排序后的表格数据
const sortedAndFilteredIpData = computed(() => {
  const result = [...filteredIpTableData.value]

  // 排序
  result.sort((a, b) => {
    const valueA = a[sortBy.value as keyof IpTableItem]
    const valueB = b[sortBy.value as keyof IpTableItem]

    if (typeof valueA === 'number' && typeof valueB === 'number') {
      return sortOrder.value === 'ascending' ? valueA - valueB : valueB - valueA
    }

    // 字符串比较
    const strA = String(valueA || '')
    const strB = String(valueB || '')

    return sortOrder.value === 'ascending' ? strA.localeCompare(strB) : strB.localeCompare(strA)
  })

  return result
})

// 显示IP详情
const showIpDetails = (record: IpTableItem) => {
  selectedIp.value = record
  ipDrawerVisible.value = true
}

// 关闭抽屉
const closeIpDrawer = () => {
  ipDrawerVisible.value = false
}

// 初始化热力图
const initHeatmapChart = () => {
  if (!heatmapRef.value) return

  // 准备热力图数据
  const heatmapData = Object.entries(statsData.heatmap.hour_heatmap).map(([time, data]) => {
    const [date, hour] = time.split(' ')
    return [date, parseInt(hour), data.count]
  })

  // 如果没有数据，提供默认数据
  if (heatmapData.length === 0) {
    const today = new Date().toISOString().split('T')[0]
    heatmapData.push([today, 0, 0])
  }

  // 提取所有不同的日期，并按日期排序
  const days = Array.from(new Set(heatmapData.map((item) => item[0])))
  days.sort()

  // 提取小时范围，确保有0-23的所有小时
  const hours = Array.from({ length: 24 }, (_, i) => i)

  // 格式化日期为更友好的显示
  const formatDay = (dateStr: string) => {
    try {
      const date = new Date(dateStr)
      return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
    } catch {
      return dateStr
    }
  }

  // 计算最大访问量，设置合理的最大值
  const maxValue = Math.max(...heatmapData.map((item) => Number(item[2])), 1)

  // 初始化图表
  heatmapChart = echarts.init(heatmapRef.value)
  heatmapChart.setOption({
    tooltip: {
      position: 'top',
      formatter: (params: echarts.DefaultLabelFormatterCallbackParams) => {
        const data = params.data as [string, number, number]
        return `${formatDay(data[0])} ${data[1]}时<br/>访问量: ${data[2]}`
      },
    },
    grid: {
      top: '10%',
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: days,
      axisLabel: {
        formatter: formatDay,
      },
      splitArea: {
        show: true,
      },
    },
    yAxis: {
      type: 'category',
      data: hours,
      splitArea: {
        show: true,
      },
      axisLabel: {
        formatter: (value: number) => `${value}时`,
      },
    },
    visualMap: {
      min: 0,
      max: maxValue,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '0%',
      textStyle: {
        color: '#000',
      },
      inRange: {
        color: ['#e8f5fe', '#8ecdf8', '#4faef6', '#2189e3', '#1265ae'],
      },
    },
    series: [
      {
        name: '访问量',
        type: 'heatmap',
        data: heatmapData,
        label: {
          show: false,
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
      },
    ],
  })

  // 监听窗口大小变化，调整图表大小
  window.addEventListener('resize', () => heatmapChart?.resize())
}

// 初始化浏览器分布图表
const initBrowserChart = () => {
  if (!browserChartRef.value) return

  // 准备饼图数据
  const browserData = Object.entries(statsData.browserStats).map(([browser, data]) => ({
    name: browser,
    value: data.count,
  }))

  // 初始化图表
  browserChart = echarts.init(browserChartRef.value)
  browserChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)',
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      data: browserData.map((item) => item.name),
      type: 'scroll',
      formatter: (name: string) => {
        // 如果名称太长，截断并添加省略号
        return name.length > 10 ? name.substring(0, 10) + '...' : name
      },
    },
    series: [
      {
        name: '浏览器分布',
        type: 'pie',
        radius: ['50%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
          position: 'center',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '18',
            fontWeight: 'bold',
          },
        },
        labelLine: {
          show: false,
        },
        data: browserData,
        color: [
          '#5470c6',
          '#91cc75',
          '#fac858',
          '#ee6666',
          '#73c0de',
          '#3ba272',
          '#fc8452',
          '#9a60b4',
        ],
      },
    ],
  })

  window.addEventListener('resize', () => browserChart?.resize())
}

// 初始化HTTP方法图表
const initHttpMethodChart = () => {
  if (!httpMethodChartRef.value) return

  // 准备条形图数据
  const methodData = Object.entries(statsData.httpMethods).map(([method, data]) => ({
    name: method,
    value: data.count,
  }))

  // 按值排序
  methodData.sort((a, b) => b.value - a.value)

  // 设置方法对应的颜色
  const methodColors = {
    GET: '#91cc75',
    POST: '#5470c6',
    PUT: '#fac858',
    DELETE: '#ee6666',
    PATCH: '#73c0de',
    OPTIONS: '#3ba272',
    HEAD: '#fc8452',
  }

  // 初始化图表
  httpMethodChart = echarts.init(httpMethodChartRef.value)
  httpMethodChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'value',
    },
    yAxis: {
      type: 'category',
      data: methodData.map((item) => item.name),
      axisLine: {
        lineStyle: {
          color: '#ddd',
        },
      },
      axisLabel: {
        color: '#333',
      },
    },
    series: [
      {
        name: '请求数',
        type: 'bar',
        data: methodData.map((item) => ({
          value: item.value,
          itemStyle: {
            color: methodColors[item.name as keyof typeof methodColors] || '#78909c',
          },
        })),
        showBackground: true,
        backgroundStyle: {
          color: 'rgba(180, 180, 180, 0.1)',
        },
        label: {
          show: true,
          position: 'right',
          formatter: '{c}',
        },
      },
    ],
  })

  window.addEventListener('resize', () => httpMethodChart?.resize())
}

// 初始化工作日图表
const initWeekdayChart = () => {
  if (!weekdayChartRef.value) return

  // 准备折线图数据（直接使用后端数据）
  const weekdayData = Object.entries(statsData.weekdayStats).map(([day, data]) => ({
    key: parseInt(day, 10),
    name: data.dayName,
    value: data.count,
  }))

  // 按星期顺序排序 (e.g. 1 for Sunday, up to 7 for Saturday)
  weekdayData.sort((a, b) => a.key - b.key)

  // 初始化图表
  weekdayChart = echarts.init(weekdayChartRef.value)
  weekdayChart.setOption({
    tooltip: {
      trigger: 'axis',
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: weekdayData.map((item) => item.name),
      axisLine: {
        lineStyle: {
          color: '#ddd',
        },
      },
      axisLabel: {
        color: '#333',
      },
    },
    yAxis: {
      type: 'value',
      axisLine: {
        show: true,
        lineStyle: {
          color: '#ddd',
        },
      },
      splitLine: {
        lineStyle: {
          color: '#eee',
        },
      },
    },
    series: [
      {
        data: weekdayData.map((item) => item.value),
        type: 'line',
        smooth: true,
        lineStyle: {
          width: 3,
          color: '#5470c6',
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            {
              offset: 0,
              color: 'rgba(84, 112, 198, 0.5)',
            },
            {
              offset: 1,
              color: 'rgba(84, 112, 198, 0.1)',
            },
          ]),
        },
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: {
          color: '#5470c6',
          borderColor: '#fff',
          borderWidth: 2,
        },
      },
    ],
  })

  window.addEventListener('resize', () => weekdayChart?.resize())
}

// 初始化IP统计图表
const initIpStatsChart = () => {
  if (!ipStatsChartRef.value) return

  // 准备数据
  const countryData = new Map<string, { cities: Map<string, number>; total: number }>()

  // 处理IP统计数据
  Object.entries(statsData.ipStats).forEach(([, data]) => {
    const country = data.country_name || '未知'
    const city = data.city || '未知'

    if (!countryData.has(country)) {
      countryData.set(country, { cities: new Map(), total: 0 })
    }

    const countryInfo = countryData.get(country)!
    countryInfo.total += data.count

    if (!countryInfo.cities.has(city)) {
      countryInfo.cities.set(city, 0)
    }
    countryInfo.cities.set(city, countryInfo.cities.get(city)! + data.count)
  })

  // 只保留有访问数据的国家
  const countries = Array.from(countryData.entries())
    .filter(([, info]) => info.total > 0)
    .map(([country]) => country)

  // 只保留有访问数据的城市
  const citiesSet = new Set<string>()
  countries.forEach((country) => {
    const info = countryData.get(country)
    if (info) {
      info.cities.forEach((count, city) => {
        if (count > 0) {
          citiesSet.add(city)
        }
      })
    }
  })
  const cities = Array.from(citiesSet)

  const series = cities.map((city) => ({
    name: city,
    type: 'bar',
    stack: 'total',
    emphasis: {
      focus: 'series',
    },
    data: countries.map((country) => {
      const countryInfo = countryData.get(country)
      return countryInfo?.cities.get(city) || 0
    }),
  }))

  // 初始化图表
  ipStatsChart = echarts.init(ipStatsChartRef.value)
  ipStatsChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
    },
    legend: {
      type: 'scroll',
      orient: 'vertical',
      right: 10,
      top: 20,
      bottom: 20,
      data: cities,
    },
    grid: {
      left: '3%',
      right: '15%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: countries,
      axisLabel: {
        interval: 0,
        rotate: 30,
      },
    },
    yAxis: {
      type: 'value',
      name: '访问次数',
    },
    series: series,
    color: [
      '#5470c6',
      '#91cc75',
      '#fac858',
      '#ee6666',
      '#73c0de',
      '#3ba272',
      '#fc8452',
      '#9a60b4',
      '#ea7ccc',
      '#48b3bd',
    ],
  })

  // 监听窗口大小变化
  window.addEventListener('resize', () => ipStatsChart?.resize())
}

// 初始化所有图表
const initCharts = () => {
  initHeatmapChart()
  initBrowserChart()
  initHttpMethodChart()
  initWeekdayChart()
  initIpStatsChart()
}

// 获取统计数据
const fetchStatsData = async () => {
  loading.value = true
  try {
    const data = await getAccessLogStats()
    console.log('获取到的仪表板数据:', JSON.stringify(data, null, 2))

    // 更新状态 - 后端现在直接提供所有需要的数据
    Object.assign(statsData, data)

    // 初始化图表
    setTimeout(() => {
      initCharts()
    }, 0)
  } catch (error) {
    console.error('获取统计数据失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 计算总访问量（备用函数，现在不需要了）
 */
const calculateTotalVisits = (httpMethods: Record<string, number> = {}): number => {
  return Object.values(httpMethods).reduce((sum, count) => sum + count, 0)
}

// 监听搜索词变化，重置页码
watch(ipSearchQuery, () => {
  currentPage.value = 1
})

// 组件挂载时获取数据
onMounted(() => {
  fetchStatsData()
})

// 导出所有模板中需要的变量和函数
defineExpose({
  loading,
  statsData,
  metrics,
  searchQuery,
  refreshChart,
  exportData,
  dateRange,
  currentPage,
  pageSize,
  ipSearchQuery,
  handleSizeChange,
  handleCurrentChange,
  exportIpData,
  heatmapRef,
  browserChartRef,
  httpMethodChartRef,
  weekdayChartRef,
  ipStatsChartRef,
  ipTableData,
  filteredIpTableData,
  sortedAndFilteredIpData,
  sortTable,
  sortBy,
  sortOrder,
  ipDrawerVisible,
  selectedIp,
  showIpDetails,
  closeIpDrawer,
})
</script>

<style scoped lang="scss">
// 导入设计系统
@use '@/assets/style/variables.scss' as *;

// ==============================================
// 现代化仪表板布局系统
// ==============================================
.dashboard-layout {
  min-height: 100%;
  width: 100%;
  background: linear-gradient(
    135deg,
    var(--background-primary) 0%,
    var(--background-secondary) 100%
  );
  position: relative;
  overflow: hidden;

  &.dashboard-loading {
    overflow: hidden;
  }
}

// ==============================================
// 加载状态
// ==============================================
.loading-state {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  @include flex-center;
  background: var(--background-overlay);
  backdrop-filter: blur(8px);
  z-index: var(--z-modal);

  .loading-content {
    @include flex-column;
    align-items: center;
    gap: var(--spacing-lg);
    padding: var(--spacing-xl);
    background: var(--background-card);
    border-radius: var(--radius-xl);
    @include card-shadow;
  }

  .loading-spinner {
    width: 60px;
    height: 60px;
    border: 4px solid var(--border-light);
    border-top: 4px solid var(--primary-color);
    border-radius: var(--radius-full);
    animation: spin 1.2s linear infinite;
  }

  .loading-text {
    color: var(--text-secondary);
    font-size: var(--font-size-md);
    font-weight: var(--font-weight-medium);
    margin: 0;
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

// ==============================================
// 仪表板网格布局
// ==============================================
.dashboard-grid {
  display: grid;
  grid-template-areas:
    'metrics'
    'charts'
    'data';
  grid-template-rows: auto auto 1fr;
  gap: var(--spacing-xl);
  padding: var(--spacing-xl);
  min-height: 100vh;
  container-type: inline-size;
  container-name: dashboard;
}

// ==============================================
// 指标区域
// ==============================================
.metrics-area {
  grid-area: metrics;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--spacing-lg);
}

.metric-card {
  background: var(--background-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  @include card-shadow;
  border: 1px solid var(--border-light);
  transition: all var(--transition-normal) var(--transition-timing);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, var(--primary-color), var(--primary-light));
  }

  &:hover {
    @include hover-lift;
    border-color: var(--primary-light);
  }

  // 类型变体
  &.metric-primary::before {
    background: linear-gradient(90deg, var(--primary-color), var(--primary-light));
  }

  &.metric-success::before {
    background: linear-gradient(90deg, var(--success-color), #34d399);
  }

  &.metric-warning::before {
    background: linear-gradient(90deg, var(--warning-color), #fbbf24);
  }

  &.metric-info::before {
    background: linear-gradient(90deg, var(--info-color), #60a5fa);
  }
}

.metric-icon {
  @include flex-center;
  width: 72px;
  height: 72px;
  border-radius: var(--radius-xl);
  font-size: 32px;
  flex-shrink: 0;
  position: relative;

  // 类型样式
  &.icon-primary {
    background: linear-gradient(135deg, var(--primary-lighter), var(--primary-light));
    color: var(--primary-color);
  }

  &.icon-success {
    background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(16, 185, 129, 0.2));
    color: var(--success-color);
  }

  &.icon-warning {
    background: linear-gradient(135deg, rgba(245, 158, 11, 0.1), rgba(245, 158, 11, 0.2));
    color: var(--warning-color);
  }

  &.icon-info {
    background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(59, 130, 246, 0.2));
    color: var(--info-color);
  }

  .el-icon {
    font-size: inherit;
  }
}

.metric-info {
  flex: 1;
  min-width: 0;
}

.metric-value {
  font-size: 2.5rem;
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  line-height: 1.2;
  margin-bottom: var(--spacing-xs);
}

.metric-label {
  font-size: var(--font-size-md);
  color: var(--text-secondary);
  font-weight: var(--font-weight-medium);
  margin-bottom: var(--spacing-xs);
}

.metric-trend {
  .up {
    color: var(--success-color);
    font-weight: var(--font-weight-semibold);
  }

  .down {
    color: var(--error-color);
    font-weight: var(--font-weight-semibold);
  }
}

// ==============================================
// 图表区域
// ==============================================
.charts-area {
  grid-area: charts;
}

.charts-primary {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--spacing-xl);
  margin-bottom: var(--spacing-xl);
}

.charts-secondary {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-xl);
}

.chart-wrapper {
  background: var(--background-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  @include card-shadow;
  border: 1px solid var(--border-light);
  transition: all var(--transition-normal) var(--transition-timing);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, var(--primary-color), transparent);
  }

  &:hover {
    @include hover-lift;
    border-color: var(--primary-light);
  }
}

.chart-header {
  @include flex-between;
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--border-light);

  .chart-title {
    font-size: var(--font-size-lg);
    font-weight: var(--font-weight-semibold);
    color: var(--text-primary);
    margin: 0;
  }

  .chart-actions {
    @include flex-start;
    gap: var(--spacing-xs);
  }
}

.chart-container {
  height: 350px;
  width: 100%;
  position: relative;

  &.geo-chart {
    height: 400px;
  }
}

// ==============================================
// 数据区域
// ==============================================
.data-area {
  grid-area: data;
}

.data-card {
  background: var(--background-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  @include card-shadow;
  border: 1px solid var(--border-light);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.data-header {
  @include flex-between;
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--border-light);

  .data-title {
    font-size: var(--font-size-lg);
    font-weight: var(--font-weight-semibold);
    color: var(--text-primary);
    margin: 0;
  }

  .data-actions {
    @include flex-start;
    gap: var(--spacing-md);

    .search-input {
      width: 240px;
    }
  }
}

/* 表格样式 */
.table-card {
  background-color: var(--background-card);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  @include card-shadow;
  width: 100%;
}

.table-wrapper {
  width: 100%;
  overflow-x: auto;
}

.table-header {
  @include flex-between;
  font-weight: var(--font-weight-semibold);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--border-light);
  margin-bottom: var(--spacing-md);
  font-size: var(--font-size-lg);
  color: var(--text-primary);
}

.table-actions {
  @include flex-start;
  gap: var(--spacing-sm);
}

.search-input {
  position: relative;
  width: 200px;
}

.search-icon {
  position: absolute;
  left: var(--spacing-sm);
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-secondary);
}

.input-field {
  width: 100%;
  padding: var(--spacing-sm) var(--spacing-xl) var(--spacing-sm) var(--spacing-xl);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  outline: none;
  transition: border-color var(--transition-normal) var(--transition-timing);
  font-size: var(--font-size-md);
  background-color: var(--background-card);
}

.input-field:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.clear-icon {
  position: absolute;
  right: var(--spacing-sm);
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-disabled);
  cursor: pointer;
  font-style: normal;
  transition: color var(--transition-normal) var(--transition-timing);

  &:hover {
    color: var(--text-secondary);
  }
}

.btn-primary {
  background: var(--gradient-primary);
  color: var(--text-inverse);
  border: none;
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius-md);
  cursor: pointer;
  @include flex-center;
  gap: var(--spacing-xs);
  transition: all var(--transition-normal) var(--transition-timing);
  font-weight: var(--font-weight-medium);
}

.btn-primary:hover {
  @include hover-lift;
  opacity: 0.9;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.data-table th,
.data-table td {
  border: 1px solid var(--border-light);
  padding: var(--spacing-sm) var(--spacing-md);
  text-align: left;
}

.data-table th {
  background-color: var(--background-secondary);
  color: var(--text-primary);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-md);
}

.sortable-header {
  cursor: pointer;
  user-select: none;
  transition: color var(--transition-normal) var(--transition-timing);

  &:hover {
    color: var(--primary-color);
  }
}

.sort-icon {
  margin-left: var(--spacing-xs);
  color: var(--text-secondary);
}

.data-table tbody tr {
  transition: background-color var(--transition-fast) var(--transition-timing);
}

.data-table tbody tr:hover {
  background-color: var(--table-row-hover-bg);
}

.btn-link {
  background: none;
  border: none;
  color: var(--primary-color);
  cursor: pointer;
  @include flex-center;
  gap: var(--spacing-xs);
  transition: color var(--transition-normal) var(--transition-timing);
  font-weight: var(--font-weight-medium);
}

.btn-link:hover {
  color: var(--primary-dark);
}

.pagination-container {
  margin-top: var(--spacing-lg);
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.pagination {
  @include flex-center;
  gap: var(--spacing-md);
}

.pagination-info {
  color: var(--text-secondary);
  font-size: var(--font-size-md);
}

.page-size-selector select {
  padding: var(--spacing-xs);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  outline: none;
  background-color: var(--background-card);
  transition: border-color var(--transition-normal) var(--transition-timing);

  &:focus {
    border-color: var(--primary-color);
  }
}

.pagination-buttons {
  @include flex-center;
  gap: var(--spacing-sm);
}

.page-btn {
  border: 1px solid var(--border-light);
  background-color: var(--background-card);
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-normal) var(--transition-timing);
  font-weight: var(--font-weight-medium);
}

.page-btn:hover:not(:disabled) {
  color: var(--primary-color);
  border-color: var(--primary-light);
  background-color: var(--primary-lighter);
}

.page-btn:disabled {
  cursor: not-allowed;
  color: var(--text-disabled);
  opacity: 0.5;
}

.page-indicator {
  padding: var(--spacing-xs) var(--spacing-sm);
  background-color: var(--primary-color);
  color: var(--text-inverse);
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-medium);
}

// ==============================================
// IP详情抽屉
// ==============================================
.ip-drawer {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: var(--z-modal);
  display: flex;
  justify-content: flex-end;

  .drawer-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: var(--background-overlay);
    backdrop-filter: blur(8px);
  }

  .drawer-panel {
    background: var(--background-card);
    height: 100vh;
    width: 480px;
    max-width: 100vw;
    @include card-shadow;
    position: relative;
    @include flex-column;
    overflow: hidden;
  }
}

.drawer-header {
  @include flex-between;
  padding: var(--spacing-xl);
  border-bottom: 1px solid var(--border-light);
  background: linear-gradient(135deg, var(--background-card) 0%, var(--background-secondary) 100%);

  .drawer-title {
    @include flex-start;
    gap: var(--spacing-sm);
    font-size: var(--font-size-lg);
    font-weight: var(--font-weight-semibold);
    color: var(--text-primary);
    margin: 0;
  }
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-xl);
}

.ip-summary {
  margin-bottom: var(--spacing-xl);

  .summary-grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: var(--spacing-lg);
  }

  .summary-item {
    label {
      display: block;
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-medium);
      color: var(--text-secondary);
      margin-bottom: var(--spacing-xs);
    }

    span:not(.el-tag) {
      color: var(--text-primary);
      font-weight: var(--font-weight-medium);
    }
  }
}

.access-records {
  margin-top: var(--spacing-lg);
}

// ==============================================
// 过渡动画
// ==============================================
.loading-enter-active,
.loading-leave-active {
  transition: all var(--transition-normal) var(--transition-timing);
}

.loading-enter-from,
.loading-leave-to {
  opacity: 0;
}

.dashboard-enter-active {
  transition: all var(--transition-slow) var(--transition-timing);
}

.dashboard-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.drawer-enter-active,
.drawer-leave-active {
  transition: all var(--transition-normal) var(--transition-timing);
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;

  .drawer-panel {
    transform: translateX(100%);
  }
}

// ==============================================
// 响应式设计
// ==============================================

// 容器查询支持
@container dashboard (max-width: 1200px) {
  .metrics-grid {
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  }

  .charts-primary {
    grid-template-columns: 1fr;
  }

  .charts-secondary {
    grid-template-columns: 1fr;
  }
}

@container dashboard (max-width: 768px) {
  .dashboard-grid {
    padding: var(--spacing-lg);
    gap: var(--spacing-lg);
  }

  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .metric-card {
    padding: var(--spacing-lg);

    .metric-icon {
      width: 60px;
      height: 60px;
      font-size: 24px;
    }

    .metric-value {
      font-size: 2rem;
    }
  }

  .chart-container {
    height: 280px;

    &.geo-chart {
      height: 320px;
    }
  }

  .data-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--spacing-md);

    .data-actions {
      justify-content: stretch;

      .search-input {
        width: 100%;
      }
    }
  }
}

// 传统响应式断点
@include responsive(xl) {
  .dashboard-grid {
    grid-template-areas:
      'metrics metrics'
      'charts charts'
      'data data';
    grid-template-columns: 1fr 1fr;
  }
}

@include responsive(lg) {
  .charts-primary {
    grid-template-columns: 1fr;
  }

  .charts-secondary {
    grid-template-columns: 1fr;
  }
}

@include responsive(md) {
  .dashboard-grid {
    grid-template-areas:
      'metrics'
      'charts'
      'data';
    grid-template-columns: 1fr;
    padding: var(--spacing-md);
    gap: var(--spacing-lg);
  }

  .metrics-grid {
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  }

  .chart-wrapper {
    padding: var(--spacing-lg);
  }

  .chart-container {
    height: 250px;

    &.geo-chart {
      height: 300px;
    }
  }

  .ip-drawer .drawer-panel {
    width: 100vw;
  }
}

@include responsive(sm) {
  .dashboard-grid {
    padding: var(--spacing-sm);
    gap: var(--spacing-md);
  }

  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .metric-card {
    flex-direction: column;
    text-align: center;
    gap: var(--spacing-md);

    .metric-icon {
      width: 48px;
      height: 48px;
      font-size: 20px;
      margin: 0 auto;
    }

    .metric-value {
      font-size: 1.75rem;
    }
  }

  .chart-wrapper {
    padding: var(--spacing-md);
  }

  .chart-container {
    height: 200px;

    &.geo-chart {
      height: 250px;
    }
  }

  .drawer-header {
    padding: var(--spacing-md);

    .drawer-title {
      font-size: var(--font-size-md);
    }
  }

  .drawer-body {
    padding: var(--spacing-md);
  }
}

// 深色模式适配
@media (prefers-color-scheme: dark) {
  .dashboard-layout {
    background: linear-gradient(135deg, var(--background-primary) 0%, rgba(0, 0, 0, 0.05) 100%);
  }
}

// 高对比度模式
@media (prefers-contrast: high) {
  .metric-card,
  .chart-wrapper,
  .data-card {
    border-width: 2px;
  }
}

// 减少动画
@media (prefers-reduced-motion: reduce) {
  .loading-enter-active,
  .loading-leave-active,
  .dashboard-enter-active,
  .drawer-enter-active,
  .drawer-leave-active {
    transition: none;
  }

  .metric-card,
  .chart-wrapper {
    &:hover {
      transform: none;
    }
  }
}
</style>
