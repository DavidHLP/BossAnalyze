<template>
  <div class="dashboard-container">
    <div class="loading-overlay" v-if="loading">
      <div class="loading-spinner"></div>
              </div>

    <div class="dashboard-content" v-else>
      <!-- 指标卡片区域 -->
      <div class="metrics-section">
        <div class="metric-cards">
          <div class="metric-card">
            <div class="metric-icon visits-icon">
              <i class="icon"><ViewIcon /></i>
              </div>
            <div class="metric-content">
              <div class="metric-value">{{ statsData.totalVisits }}</div>
              <div class="metric-label">总访问量</div>
              </div>
              </div>

          <div class="metric-card">
            <div class="metric-icon ips-icon">
              <i class="icon"><User /></i>
            </div>
            <div class="metric-content">
              <div class="metric-value">{{ statsData.uniqueIps }}</div>
              <div class="metric-label">独立IP数</div>
            </div>
          </div>

          <div class="metric-card">
            <div class="metric-icon browser-icon">
              <i class="icon"><ChromeFilled /></i>
            </div>
            <div class="metric-content">
              <div class="metric-value">{{ Object.keys(statsData.browserStats).length }}</div>
              <div class="metric-label">浏览器类型</div>
            </div>
          </div>

          <div class="metric-card">
            <div class="metric-icon http-icon">
              <i class="icon"><Connection /></i>
            </div>
            <div class="metric-content">
              <div class="metric-value">{{ Object.keys(statsData.httpMethods).length }}</div>
              <div class="metric-label">HTTP方法</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 图表区域 -->
      <div class="charts-section">
        <!-- 上部图表 -->
        <div class="charts-row">
          <div class="chart-card large">
            <div class="chart-header">
              <span>访问量热力图</span>
              <div class="tooltip-wrapper">
                <i class="icon info-icon"><InfoFilled /></i>
                <div class="tooltip-content">显示每天每小时的访问量分布</div>
              </div>
            </div>
            <div ref="heatmapRef" class="chart-container"></div>
          </div>

          <div class="chart-card small">
            <div class="chart-header">
              <span>浏览器分布</span>
              <div class="tooltip-wrapper">
                <i class="icon info-icon"><InfoFilled /></i>
                <div class="tooltip-content">访问者使用的浏览器类型统计</div>
              </div>
            </div>
            <div ref="browserChartRef" class="chart-container"></div>
          </div>
        </div>

        <!-- 下部图表 -->
        <div class="charts-row">
          <div class="chart-card medium">
            <div class="chart-header">
              <span>HTTP方法分布</span>
              <div class="tooltip-wrapper">
                <i class="icon info-icon"><InfoFilled /></i>
                <div class="tooltip-content">请求使用的HTTP方法统计</div>
              </div>
            </div>
            <div ref="httpMethodChartRef" class="chart-container"></div>
          </div>

          <div class="chart-card medium">
            <div class="chart-header">
              <span>每周访问趋势</span>
              <div class="tooltip-wrapper">
                <i class="icon info-icon"><InfoFilled /></i>
                <div class="tooltip-content">一周内各天的访问量趋势</div>
              </div>
            </div>
            <div ref="weekdayChartRef" class="chart-container"></div>
          </div>
        </div>
      </div>

      <!-- IP地址统计表格区域 -->
      <div class="table-section">
        <div class="table-card">
          <div class="table-header">
            <span>IP地址访问统计</span>
          </div>

          <div ref="ipStatsChartRef" class="chart-container"></div>
        </div>
      </div>
    </div>

    <!-- IP详情抽屉 -->
    <div class="drawer-container" v-if="ipDrawerVisible">
      <div class="drawer-backdrop" @click="closeIpDrawer"></div>
      <div class="drawer">
        <div class="drawer-header">
          <h3>IP详情: {{ selectedIp?.ip || '' }}</h3>
          <button class="close-btn" @click="closeIpDrawer">×</button>
        </div>

        <div class="drawer-content" v-if="selectedIp">
          <dl class="details-list">
            <dt>IP地址</dt>
            <dd><span class="tag">{{ selectedIp.ip }}</span></dd>

            <dt>访问次数</dt>
            <dd><span class="badge">{{ selectedIp.count }}</span></dd>

            <dt>国家/地区</dt>
            <dd>{{ selectedIp.country_name || '未知' }}</dd>

            <dt>城市</dt>
            <dd>{{ selectedIp.city || '未知' }}</dd>
          </dl>

          <div class="divider">
            <span>访问记录</span>
          </div>

          <!-- 这里可以添加IP访问的详细记录 -->
          <div class="empty-container">
            <div class="empty-icon">📭</div>
            <p>暂无详细记录</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, onMounted, ref, reactive, computed, watch } from 'vue';
import { getAccessLogStats } from '@/api/log/accesslog';
import type { AccessLogStats } from '@/api/log/accesslog.d';
import {
  View as ViewIcon,
  User,
  ChromeFilled,
  Connection,
  InfoFilled,
} from '@element-plus/icons-vue';
import * as echarts from 'echarts';

interface IpTableItem {
  ip: string;
  count: number;
  country_name?: string;
  city?: string;
}

export default defineComponent({
  name: 'DashboardView',
  components: {
    ViewIcon,
    User,
    ChromeFilled,
    Connection,
    InfoFilled,
  },
  setup() {
    // 响应式状态
    const loading = ref(true);
    const statsData = reactive<AccessLogStats>({
      totalVisits: 0,
      uniqueIps: 0,
      avgResponseTime: 0,
      errorRate: 0,
      heatmap: { minute_heatmap: {}, hour_heatmap: {} },
      ipStats: {},
      httpMethods: {},
      weekdayStats: {},
      browserStats: {}
    });

    // 日期过滤功能
    const dateRange = ref<string[]>([]);

    // IP表格分页相关
    const currentPage = ref(1);
    const pageSize = ref(10);
    const ipSearchQuery = ref('');

    // 添加表格排序功能和修复分页选择问题
    const sortOrder = ref<'ascending' | 'descending'>('descending');
    const sortBy = ref<string>('count');

    // 表格排序
    const sortTable = (column: string) => {
      if (sortBy.value === column) {
        sortOrder.value = sortOrder.value === 'ascending' ? 'descending' : 'ascending';
      } else {
        sortBy.value = column;
        sortOrder.value = 'descending';
      }
    };

    // 处理分页变化（修复）
    const handleSizeChange = (val: number | string) => {
      pageSize.value = Number(val);
    };

    const handleCurrentChange = (val: number) => {
      currentPage.value = val;
    };

    // 导出IP数据
    const exportIpData = () => {
      // 实现导出功能
      console.log('导出IP数据');
    };

    // 图表引用
    const heatmapRef = ref<HTMLDivElement | null>(null);
    const browserChartRef = ref<HTMLDivElement | null>(null);
    const httpMethodChartRef = ref<HTMLDivElement | null>(null);
    const weekdayChartRef = ref<HTMLDivElement | null>(null);
    const ipStatsChartRef = ref<HTMLDivElement | null>(null);

    // 图表实例
    let heatmapChart: echarts.ECharts | null = null;
    let browserChart: echarts.ECharts | null = null;
    let httpMethodChart: echarts.ECharts | null = null;
    let weekdayChart: echarts.ECharts | null = null;
    let ipStatsChart: echarts.ECharts | null = null;

    // IP详情抽屉
    const ipDrawerVisible = ref(false);
    const selectedIp = ref<IpTableItem | null>(null);

    // 转换IP统计数据为表格数据
    const ipTableData = computed(() => {
      return Object.entries(statsData.ipStats).map(([ip, data]) => ({
        ip,
        count: data.count,
        country_name: data.country_name || '未知',
        city: data.city || '未知'
      }));
    });

    // 过滤后的IP表格数据
    const filteredIpTableData = computed(() => {
      const startIndex = (currentPage.value - 1) * pageSize.value;
      let data = ipTableData.value;

      // 搜索过滤
      if (ipSearchQuery.value) {
        data = data.filter(item =>
          item.ip.includes(ipSearchQuery.value) ||
          item.country_name?.includes(ipSearchQuery.value) ||
          item.city?.includes(ipSearchQuery.value)
        );
      }

      // 分页
      return data.slice(startIndex, startIndex + pageSize.value);
    });

    // 按排序规则排序后的表格数据
    const sortedAndFilteredIpData = computed(() => {
      const result = [...filteredIpTableData.value];

      // 排序
      result.sort((a, b) => {
        const valueA = a[sortBy.value as keyof IpTableItem];
        const valueB = b[sortBy.value as keyof IpTableItem];

        if (typeof valueA === 'number' && typeof valueB === 'number') {
          return sortOrder.value === 'ascending' ? valueA - valueB : valueB - valueA;
        }

        // 字符串比较
        const strA = String(valueA || '');
        const strB = String(valueB || '');

        return sortOrder.value === 'ascending'
          ? strA.localeCompare(strB)
          : strB.localeCompare(strA);
      });

      return result;
    });

    // 显示IP详情
    const showIpDetails = (record: IpTableItem) => {
      selectedIp.value = record;
      ipDrawerVisible.value = true;
    };

    // 关闭抽屉
    const closeIpDrawer = () => {
      ipDrawerVisible.value = false;
    };

    // 初始化热力图
    const initHeatmapChart = () => {
      if (!heatmapRef.value) return;

      // 准备热力图数据
      const heatmapData = Object.entries(statsData.heatmap.hour_heatmap).map(([time, count]) => {
        const [date, hour] = time.split(' ');
        return [date, parseInt(hour), count];
      });

      // 如果没有数据，提供默认数据
      if (heatmapData.length === 0) {
        const today = new Date().toISOString().split('T')[0];
        heatmapData.push([today, 0, 0]);
      }

      // 提取所有不同的日期，并按日期排序
      const days = Array.from(new Set(heatmapData.map(item => item[0])));
      days.sort();

      // 提取小时范围，确保有0-23的所有小时
      const hours = Array.from({ length: 24 }, (_, i) => i);

      // 格式化日期为更友好的显示
      const formatDay = (dateStr: string) => {
        try {
          const date = new Date(dateStr);
          return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' });
        } catch {
          return dateStr;
        }
      };

      // 计算最大访问量，设置合理的最大值
      const maxValue = Math.max(...heatmapData.map(item => Number(item[2])), 1);

      // 初始化图表
      heatmapChart = echarts.init(heatmapRef.value);
      heatmapChart.setOption({
        tooltip: {
          position: 'top',
          formatter: (params: echarts.DefaultLabelFormatterCallbackParams) => {
            const data = params.data as [string, number, number];
            return `${formatDay(data[0])} ${data[1]}时<br/>访问量: ${data[2]}`;
          }
        },
        grid: {
          top: '10%',
          left: '3%',
          right: '4%',
          bottom: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: days,
          axisLabel: {
            formatter: formatDay
          },
          splitArea: {
            show: true
          }
        },
        yAxis: {
          type: 'category',
          data: hours,
          splitArea: {
            show: true
          },
          axisLabel: {
            formatter: (value: number) => `${value}时`
          }
        },
        visualMap: {
          min: 0,
          max: maxValue,
          calculable: true,
          orient: 'horizontal',
          left: 'center',
          bottom: '0%',
          textStyle: {
            color: '#000'
          },
          inRange: {
            color: ['#e8f5fe', '#8ecdf8', '#4faef6', '#2189e3', '#1265ae']
          }
        },
        series: [{
          name: '访问量',
          type: 'heatmap',
          data: heatmapData,
          label: {
            show: false
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }]
      });

      // 监听窗口大小变化，调整图表大小
      window.addEventListener('resize', () => heatmapChart?.resize());
    };

    // 初始化浏览器分布图表
    const initBrowserChart = () => {
      if (!browserChartRef.value) return;

      // 准备饼图数据
      const browserData = Object.entries(statsData.browserStats).map(([browser, count]) => ({
        name: browser,
        value: count
      }));

      // 初始化图表
      browserChart = echarts.init(browserChartRef.value);
      browserChart.setOption({
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'center',
          data: browserData.map(item => item.name),
          type: 'scroll',
          formatter: (name: string) => {
            // 如果名称太长，截断并添加省略号
            return name.length > 10 ? name.substring(0, 10) + '...' : name;
          }
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
              borderWidth: 2
            },
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: '18',
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: false
            },
            data: browserData,
            color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']
          }
        ]
      });

      window.addEventListener('resize', () => browserChart?.resize());
    };

    // 初始化HTTP方法图表
    const initHttpMethodChart = () => {
      if (!httpMethodChartRef.value) return;

      // 准备条形图数据
      const methodData = Object.entries(statsData.httpMethods).map(([method, count]) => ({
        name: method,
        value: count
      }));

      // 按值排序
      methodData.sort((a, b) => b.value - a.value);

      // 设置方法对应的颜色
      const methodColors = {
        GET: '#91cc75',
        POST: '#5470c6',
        PUT: '#fac858',
        DELETE: '#ee6666',
        PATCH: '#73c0de',
        OPTIONS: '#3ba272',
        HEAD: '#fc8452'
      };

      // 初始化图表
      httpMethodChart = echarts.init(httpMethodChartRef.value);
      httpMethodChart.setOption({
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
          type: 'value'
        },
        yAxis: {
          type: 'category',
          data: methodData.map(item => item.name),
          axisLine: {
            lineStyle: {
              color: '#ddd'
            }
          },
          axisLabel: {
            color: '#333'
          }
        },
        series: [
          {
            name: '请求数',
            type: 'bar',
            data: methodData.map(item => ({
              value: item.value,
              itemStyle: {
                color: methodColors[item.name as keyof typeof methodColors] || '#78909c'
              }
            })),
            showBackground: true,
            backgroundStyle: {
              color: 'rgba(180, 180, 180, 0.1)'
            },
            label: {
              show: true,
              position: 'right',
              formatter: '{c}'
            }
          }
        ]
      });

      window.addEventListener('resize', () => httpMethodChart?.resize());
    };

    // 初始化工作日图表
    const initWeekdayChart = () => {
      if (!weekdayChartRef.value) return;

      // 星期映射（修改为与后端数据匹配）
      const weekdayMap: Record<string, string> = {
        '1': '周一',
        '2': '周二',
        '3': '周三',
        '4': '周四',
        '5': '周五',
        '6': '周六',
        '7': '周日'
      };

      // 准备折线图数据（直接使用后端数据）
      const weekdayData = Object.entries(statsData.weekdayStats).map(([day, count]) => ({
        name: weekdayMap[day] || `星期${day}`,
        value: count
      }));

      // 按星期顺序排序
      weekdayData.sort((a, b) => {
        const dayA = Object.keys(weekdayMap).findIndex(key => weekdayMap[key] === a.name);
        const dayB = Object.keys(weekdayMap).findIndex(key => weekdayMap[key] === b.name);
        return dayA - dayB;
      });

      // 初始化图表
      weekdayChart = echarts.init(weekdayChartRef.value);
      weekdayChart.setOption({
        tooltip: {
          trigger: 'axis'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: weekdayData.map(item => item.name),
          axisLine: {
            lineStyle: {
              color: '#ddd'
            }
          },
          axisLabel: {
            color: '#333'
          }
        },
        yAxis: {
          type: 'value',
          axisLine: {
            show: true,
            lineStyle: {
              color: '#ddd'
            }
          },
          splitLine: {
            lineStyle: {
              color: '#eee'
            }
          }
        },
        series: [
          {
            data: weekdayData.map(item => item.value),
            type: 'line',
            smooth: true,
            lineStyle: {
              width: 3,
              color: '#5470c6'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {
                  offset: 0,
                  color: 'rgba(84, 112, 198, 0.5)'
                },
                {
                  offset: 1,
                  color: 'rgba(84, 112, 198, 0.1)'
                }
              ])
            },
            symbol: 'circle',
            symbolSize: 8,
            itemStyle: {
              color: '#5470c6',
              borderColor: '#fff',
              borderWidth: 2
            }
          }
        ]
      });

      window.addEventListener('resize', () => weekdayChart?.resize());
    };

    // 初始化IP统计图表
    const initIpStatsChart = () => {
      if (!ipStatsChartRef.value) return;

      // 准备数据
      const countryData = new Map<string, { cities: Map<string, number>, total: number }>();

      // 处理IP统计数据
      Object.entries(statsData.ipStats).forEach(([, data]) => {
        const country = data.country_name || '未知';
        const city = data.city || '未知';

        if (!countryData.has(country)) {
          countryData.set(country, { cities: new Map(), total: 0 });
        }

        const countryInfo = countryData.get(country)!;
        countryInfo.total += data.count;

        if (!countryInfo.cities.has(city)) {
          countryInfo.cities.set(city, 0);
        }
        countryInfo.cities.set(city, countryInfo.cities.get(city)! + data.count);
      });

      // 只保留有访问数据的国家
      const countries = Array.from(countryData.entries())
        .filter(([, info]) => info.total > 0)
        .map(([country]) => country);

      // 只保留有访问数据的城市
      const citiesSet = new Set<string>();
      countries.forEach(country => {
        const info = countryData.get(country);
        if (info) {
          info.cities.forEach((count, city) => {
            if (count > 0) {
              citiesSet.add(city);
            }
          });
        }
      });
      const cities = Array.from(citiesSet);

      const series = cities.map(city => ({
        name: city,
        type: 'bar',
        stack: 'total',
        emphasis: {
          focus: 'series'
        },
        data: countries.map(country => {
          const countryInfo = countryData.get(country);
          return countryInfo?.cities.get(city) || 0;
        })
      }));

      // 初始化图表
      ipStatsChart = echarts.init(ipStatsChartRef.value);
      ipStatsChart.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        legend: {
          type: 'scroll',
          orient: 'vertical',
          right: 10,
          top: 20,
          bottom: 20,
          data: cities
        },
        grid: {
          left: '3%',
          right: '15%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: countries,
          axisLabel: {
            interval: 0,
            rotate: 30
          }
        },
        yAxis: {
          type: 'value',
          name: '访问次数'
        },
        series: series,
        color: [
          '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
          '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#48b3bd'
        ]
      });

      // 监听窗口大小变化
      window.addEventListener('resize', () => ipStatsChart?.resize());
    };

    // 初始化所有图表
    const initCharts = () => {
      initHeatmapChart();
      initBrowserChart();
      initHttpMethodChart();
      initWeekdayChart();
      initIpStatsChart();
    };

    // 获取统计数据
    const fetchStatsData = async () => {
      loading.value = true;
      try {
        const data = await getAccessLogStats();

        // 更新状态
        Object.assign(statsData, data);

        // 计算总访问量和唯一IP数
        if (!statsData.totalVisits) {
          statsData.totalVisits = calculateTotalVisits(statsData.httpMethods);
        }

        if (!statsData.uniqueIps) {
          statsData.uniqueIps = Object.keys(statsData.ipStats || {}).length;
        }

        // 初始化图表
        setTimeout(() => {
          initCharts();
        }, 0);
      } catch (error) {
        console.error('获取统计数据失败:', error);
      } finally {
        loading.value = false;
      }
    };

    /**
     * 计算总访问量
     */
    const calculateTotalVisits = (httpMethods: Record<string, number> = {}): number => {
      return Object.values(httpMethods).reduce((sum, count) => sum + count, 0);
    };

    // 监听搜索词变化，重置页码
    watch(ipSearchQuery, () => {
      currentPage.value = 1;
    });

    // 组件挂载时获取数据
    onMounted(() => {
      fetchStatsData();
    });

    return {
      loading,
      statsData,
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
      closeIpDrawer
    };
  }
});
</script>

<style scoped>
.dashboard-container {
  padding: 0;
  background-color: #f5f7fa;
  min-height: 100vh;
  width: 100%;
  box-sizing: border-box;
  overflow: auto;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 5px solid #f3f3f3;
  border-top: 5px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.dashboard-content {
  padding: 15px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

/* 各区域容器样式 */
.metrics-section,
.charts-section,
.table-section {
  width: 100%;
  margin-bottom: 20px;
}

.charts-row {
  width: 100%;
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

/* 指标卡片样式 */
.metric-cards {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.metric-card {
  height: 100px;
  display: flex;
  transition: all 0.3s;
  background-color: white;
  padding: 15px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  flex: 1;
  min-width: 200px;
}

.metric-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  font-size: 28px;
  margin: auto 15px auto 0;
}

.icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.visits-icon {
  background-color: rgba(64, 158, 255, 0.1);
  color: #409eff;
}

.ips-icon {
  background-color: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}

.browser-icon {
  background-color: rgba(230, 162, 60, 0.1);
  color: #e6a23c;
}

.http-icon {
  background-color: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
}

.metric-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
}

.metric-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

/* 图表样式 */
.chart-card {
  background-color: white;
  padding: 15px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.chart-card.large {
  flex: 2;
  min-width: 60%;
}

.chart-card.medium {
  flex: 1;
  min-width: 45%;
}

.chart-card.small {
  flex: 1;
  min-width: 30%;
}

.chart-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.chart-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 15px;
}

.chart-container {
  height: 320px;
  width: 100%;
}

.tooltip-wrapper {
  position: relative;
  display: inline-block;
}

.tooltip-wrapper:hover .tooltip-content {
  display: block;
}

.tooltip-content {
  display: none;
  position: absolute;
  background-color: #303133;
  color: white;
  padding: 5px 10px;
  border-radius: 4px;
  z-index: 1;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  white-space: nowrap;
  font-weight: normal;
  font-size: 12px;
  margin-top: 5px;
}

.tooltip-content::before {
  content: '';
  position: absolute;
  bottom: 100%;
  left: 50%;
  margin-left: -5px;
  border-width: 5px;
  border-style: solid;
  border-color: transparent transparent #303133 transparent;
}

.info-icon {
  color: #909399;
  cursor: help;
}

/* 表格样式 */
.table-card {
  background-color: white;
  padding: 15px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  width: 100%;
}

.table-wrapper {
  width: 100%;
  overflow-x: auto;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 15px;
}

.table-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-input {
  position: relative;
  width: 200px;
}

.search-icon {
  position: absolute;
  left: 8px;
  top: 50%;
  transform: translateY(-50%);
  color: #909399;
}

.input-field {
  width: 100%;
  padding: 8px 30px 8px 30px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  outline: none;
  transition: border-color 0.2s;
}

.input-field:focus {
  border-color: #409eff;
}

.clear-icon {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  color: #c0c4cc;
  cursor: pointer;
  font-style: normal;
}

.btn-primary {
  background-color: #409eff;
  color: white;
  border: none;
  padding: 8px 15px;
  border-radius: 4px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  transition: background-color 0.3s;
}

.btn-primary:hover {
  background-color: #66b1ff;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th, .data-table td {
  border: 1px solid #ebeef5;
  padding: 12px;
  text-align: left;
}

.data-table th {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 500;
}

.sortable-header {
  cursor: pointer;
  user-select: none;
}

.sort-icon {
  margin-left: 5px;
}

.data-table tbody tr:hover {
  background-color: #f5f7fa;
}

.btn-link {
  background: none;
  border: none;
  color: #409eff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.btn-link:hover {
  color: #66b1ff;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 15px;
}

.pagination-info {
  color: #606266;
}

.page-size-selector select {
  padding: 5px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  outline: none;
}

.pagination-buttons {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-btn {
  border: 1px solid #dcdfe6;
  background-color: white;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.page-btn:hover:not(:disabled) {
  color: #409eff;
  border-color: #c6e2ff;
}

.page-btn:disabled {
  cursor: not-allowed;
  color: #c0c4cc;
}

.page-indicator {
  padding: 5px 10px;
  background-color: #409eff;
  color: white;
  border-radius: 4px;
}

/* 抽屉样式 */
.drawer-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: flex-end;
  z-index: 2000;
  overflow: auto;
}

.drawer-backdrop {
  position: absolute;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
}

.drawer {
  background-color: #fff;
  height: 100vh;
  width: 500px;
  max-width: 100vw;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 2001;
  animation: slideIn 0.3s ease-out;
  overflow: hidden;
}

@keyframes slideIn {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #ebeef5;
}

.drawer-header h3 {
  margin: 0;
  font-size: 16px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #909399;
}

.drawer-content {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
  max-height: calc(100vh - 60px);
}

.details-list {
  margin: 0;
  padding: 0;
}

.details-list dt {
  font-weight: 600;
  margin-top: 15px;
  color: #606266;
}

.details-list dt:first-child {
  margin-top: 0;
}

.details-list dd {
  margin-left: 0;
  margin-top: 5px;
  color: #303133;
}

.tag {
  display: inline-block;
  background-color: #e6f7ff;
  padding: 2px 8px;
  border-radius: 4px;
  color: #1890ff;
  font-size: 14px;
}

.badge {
  display: inline-block;
  background-color: #f6ffed;
  padding: 2px 8px;
  border-radius: 4px;
  color: #52c41a;
  font-size: 14px;
}

.divider {
  position: relative;
  height: 20px;
  text-align: center;
  margin: 30px 0;
}

.divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  width: 100%;
  height: 1px;
  background-color: #dcdfe6;
}

.divider span {
  position: relative;
  background-color: white;
  padding: 0 10px;
  color: #909399;
  font-size: 14px;
}

.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px 0;
}

.empty-icon {
  font-size: 48px;
  color: #909399;
  margin-bottom: 10px;
}

.empty-container p {
  color: #909399;
  margin: 0;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .table-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .table-actions {
  width: 100%;
    flex-direction: column;
    align-items: stretch;
}

  .search-input {
    width: 100%;
  }

  .chart-container {
    height: 250px;
  }

  .charts-row {
    flex-direction: column;
  }

  .drawer {
    width: 100vw;
    min-width: 0;
    max-width: 100vw;
  }
}
</style>
