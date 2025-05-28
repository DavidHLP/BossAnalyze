<template>
  <div class="dashboard-container">

    <div v-loading="loading" element-loading-text="加载中..." element-loading-background="rgba(255, 255, 255, 0.8)">
      <!-- 数据概览卡片 -->
      <el-row :gutter="20" class="dashboard-cards">
        <el-col :span="6" v-for="(card, index) in statCards" :key="index">
          <el-card class="dashboard-card" :body-style="{ padding: '0px' }" shadow="hover">
            <div class="card-content">
              <div class="card-icon" :style="{ backgroundColor: card.color }">
                <el-icon>
                  <component :is="card.icon" />
                </el-icon>
              </div>
              <div class="card-info">
                <div class="card-title">{{ card.title }}</div>
                <div class="card-value">{{ card.value }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 主要可视化区域 -->
      <el-row :gutter="20" class="dashboard-charts">
        <!-- 访问热力图分析 - 全宽度 -->
        <el-col :span="24">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span>访问热力图分析</span>
                <el-tag size="small" type="success">时间分布</el-tag>
              </div>
            </template>
            <div id="heatmap-chart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 地理与安全分析 -->
      <el-row :gutter="20" class="dashboard-charts">
        <!-- IP地理位置分布 -->
        <el-col :span="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span>IP地理分布分析</span>
                <el-tag size="small" type="primary">地域分析</el-tag>
              </div>
            </template>
            <div id="geo-distribution-chart" class="chart-container"></div>
          </el-card>
        </el-col>
        <!-- 异常检测 -->
        <el-col :span="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span>访问异常检测</span>
                <el-tag size="small" type="danger">安全监控</el-tag>
              </div>
            </template>
            <div id="anomaly-chart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 访问资源分析 -->
      <el-row :gutter="20" class="dashboard-charts">
        <!-- IP TOP10 -->
        <el-col :span="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span>访问IP TOP10</span>
                <el-tag size="small" type="info">实时统计</el-tag>
              </div>
            </template>
            <div id="ip-chart" class="chart-container"></div>
          </el-card>
        </el-col>
        <!-- URL TOP10 -->
        <el-col :span="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span>访问URL TOP10</span>
                <el-tag size="small" type="info">实时统计</el-tag>
              </div>
            </template>
            <div id="url-chart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- API与请求分析 -->
      <el-row :gutter="20" class="dashboard-charts">
        <!-- API调用序列分析 -->
        <el-col :span="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span>API调用序列分析</span>
                <el-tag size="small" type="warning">行为路径</el-tag>
              </div>
            </template>
            <div id="api-sequence-chart" class="chart-container"></div>
          </el-card>
        </el-col>
        <!-- HTTP方法分布 -->
        <el-col :span="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span>HTTP方法分布</span>
                <el-tag size="small" type="info">请求类型</el-tag>
              </div>
            </template>
            <div id="http-method-chart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 时间与用户分析 -->
      <el-row :gutter="20" class="dashboard-charts">
        <!-- 工作日/周末访问分布 -->
        <el-col :span="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span>工作日/周末访问分布</span>
                <el-tag size="small" type="success">时间模式</el-tag>
              </div>
            </template>
            <div id="weekday-chart" class="chart-container"></div>
          </el-card>
        </el-col>
        <!-- 用户代理分析 -->
        <el-col :span="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span>用户代理分析</span>
                <el-tag size="small" type="primary">用户画像</el-tag>
              </div>
            </template>
            <div id="user-agent-chart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 来源与路径分析 -->
      <el-row :gutter="20" class="dashboard-charts">
        <!-- 来源页面分析 -->
        <el-col :span="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span>来源页面分析</span>
                <el-tag size="small" type="warning">流量来源</el-tag>
              </div>
            </template>
            <div id="referrer-chart" class="chart-container"></div>
          </el-card>
        </el-col>
        <!-- API类别分布 -->
        <el-col :span="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span>API类别分布</span>
                <el-tag size="small" type="info">接口分类</el-tag>
              </div>
            </template>
            <div id="api-category-chart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, watch, computed } from 'vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import {
  getAllStats,
  getAnomalyStats,
  getUserBehaviorStats,
  getHeatmapData
} from '@/api/log/accesslog';
import type {
  AccessLogSummary,
  IpStats,
  UrlStats,
  TimeStats,
  AnomalyStats,
  UserBehaviorStats
} from '@/api/log/accesslog.d';

// 状态变量
const loading = ref(false);

// 数据变量
const summary = ref<AccessLogSummary>({} as AccessLogSummary);
const ipStats = ref<IpStats | null>(null);
const urlStats = ref<UrlStats | null>(null);
const timeStats = ref<TimeStats | null>(null);
const anomalyStats = ref<AnomalyStats | null>(null);
const userBehaviorStats = ref<UserBehaviorStats | null>(null);
const heatmapData = ref<Record<string, number>>({});

// 图表实例
let ipChart: echarts.ECharts | null = null;
let urlChart: echarts.ECharts | null = null;
let apiCategoryChart: echarts.ECharts | null = null;
let anomalyChart: echarts.ECharts | null = null;
let userAgentChart: echarts.ECharts | null = null;
let heatmapChart: echarts.ECharts | null = null;
let geoDistributionChart: echarts.ECharts | null = null;
let httpMethodChart: echarts.ECharts | null = null;
let weekdayChart: echarts.ECharts | null = null;
let referrerChart: echarts.ECharts | null = null;
let apiSequenceChart: echarts.ECharts | null = null;

// 数据卡片计算属性
const statCards = computed(() => [
  {
    title: '总请求数',
    value: summary.value.totalRequests || '暂无数据',
    icon: 'Monitor',
    color: '#409EFF'
  },
  {
    title: '唯一IP数',
    value: summary.value.uniqueIPs || '暂无数据',
    icon: 'User',
    color: '#67C23A'
  },
  {
    title: 'URL数量',
    value: urlStats.value?.uniqueCount || '暂无数据',
    icon: 'Link',
    color: '#E6A23C'
  },
  {
    title: '高峰时间',
    value: (timeStats.value?.peakHour || '暂无数据') + (timeStats.value?.peakHour ? '点' : ''),
    icon: 'Clock',
    color: '#F56C6C'
  },
  {
    title: '唯一路径数',
    value: summary.value.uniquePaths || '暂无数据',
    icon: 'DataLine',
    color: '#909399'
  },
  {
    title: 'API类别数',
    value: Object.keys(summary.value.apiCategoryStats || {}).length || '暂无数据',
    icon: 'TrendCharts',
    color: '#9B59B6'
  },
  {
    title: '异常请求数',
    value: anomalyStats.value ? Object.keys(anomalyStats.value.stdAnomalies || {}).length : '暂无数据',
    icon: 'Warning',
    color: '#E74C3C'
  },
  {
    title: '设备类型数',
    value: userBehaviorStats.value ? Object.keys(userBehaviorStats.value.uaStats || {}).length : '暂无数据',
    icon: 'Mobile',
    color: '#3498DB'
  }
]);

// 初始化图表
const initCharts = () => {
  // 公共配置 - 图表主题
  const chartTheme = {
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9B59B6', '#36C9C6', '#FF9900', '#FF6666', '#3399FF'],
    backgroundColor: 'rgba(0, 0, 0, 0)',
    textStyle: {}
  };

  // IP图表
  ipChart = echarts.init(document.getElementById('ip-chart'));
  ipChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: '{b}: {c} 次'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      boundaryGap: [0, 0.01]
    },
    yAxis: {
      type: 'category',
      data: []
    },
    series: [
      {
        name: '访问次数',
        type: 'bar',
        data: [],
        itemStyle: {
          borderRadius: [0, 4, 4, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#409EFF' },
            { offset: 1, color: '#36C9C6' }
          ])
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#37A2FF' },
              { offset: 1, color: '#32BBB8' }
            ])
          }
        },
        label: {
          show: true,
          position: 'right',
          formatter: '{c}'
        }
      }
    ]
  });

  // URL图表
  urlChart = echarts.init(document.getElementById('url-chart'));
  urlChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: '{b}: {c} 次'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      boundaryGap: [0, 0.01]
    },
    yAxis: {
      type: 'category',
      data: [],
      axisLabel: {
        formatter: function(value: string) {
          // 截断长URL，显示最后30个字符
          if (value.length > 30) {
            return '...' + value.substring(value.length - 30);
          }
          return value;
        }
      }
    },
    series: [
      {
        name: '访问次数',
        type: 'bar',
        data: [],
        itemStyle: {
          borderRadius: [0, 4, 4, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#67C23A' },
            { offset: 1, color: '#95D475' }
          ])
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#5FB235' },
              { offset: 1, color: '#89C966' }
            ])
          }
        },
        label: {
          show: true,
          position: 'right',
          formatter: '{c}'
        }
      }
    ]
  });

  // 时间图表已移除，使用访问热力图代替

  // API类别图表（新增）
  apiCategoryChart = echarts.init(document.getElementById('api-category-chart'));
  apiCategoryChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      type: 'scroll'
    },
    series: [
      {
        name: 'API类别',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
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
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: []
      }
    ]
  });

  // 热门路径图表已移除，集成到其他API分析图表中

  // 异常检测图表
  anomalyChart = echarts.init(document.getElementById('anomaly-chart'));
  anomalyChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: '{b}: {c} 次'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: []
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '异常请求量',
        type: 'bar',
        data: [],
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#F56C6C' },
            { offset: 1, color: '#FFAAAA' }
          ])
        },
        markLine: {
          data: [
            { name: '阈值', yAxis: 0, lineStyle: { color: '#E74C3C', type: 'dashed' } }
          ]
        }
      }
    ]
  });

  // 用户代理分析图表
  userAgentChart = echarts.init(document.getElementById('user-agent-chart'));
  userAgentChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      type: 'scroll'
    },
    series: [
      {
        name: '用户代理',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        data: [],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  });

  // 访问热力图
  heatmapChart = echarts.init(document.getElementById('heatmap-chart'));
  heatmapChart.setOption({
    tooltip: {
      position: 'top',
      formatter: function (params: any) {
        return `${params.data[0]}-${params.data[1]}: ${params.data[2]} 次访问`;
      }
    },
    grid: {
      height: '70%',
      top: '10%'
    },
    xAxis: {
      type: 'category',
      data: Array.from({ length: 24 }, (_, i) => i.toString() + '时'),
      splitArea: {
        show: true
      }
    },
    yAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      splitArea: {
        show: true
      }
    },
    visualMap: {
      min: 0,
      max: 10,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '5%'
    },
    series: [{
      name: '访问热度',
      type: 'heatmap',
      data: [],
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

  // 窗口大小变化时重新调整图表大小
  // IP地理位置分布图表
  geoDistributionChart = echarts.init(document.getElementById('geo-distribution-chart'));
  geoDistributionChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      type: 'scroll'
    },
    series: [
      {
        name: 'IP地理位置',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        data: [],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        },
        label: {
          formatter: '{b}: {c} ({d}%)'
        }
      }
    ]
  });

  // HTTP方法分布图表
  httpMethodChart = echarts.init(document.getElementById('http-method-chart'));
  httpMethodChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      bottom: '5%',
      left: 'center'
    },
    series: [
      {
        name: 'HTTP方法',
        type: 'pie',
        radius: '50%',
        center: ['50%', '40%'],
        data: [],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  });

  // 工作日/周末访问分布图表
  weekdayChart = echarts.init(document.getElementById('weekday-chart'));
  weekdayChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['工作日', '周末']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '访问次数',
        type: 'bar',
        data: [],
        itemStyle: {
          color: function(params: any) {
            // 工作日蓝色，周末绿色
            return params.dataIndex < 5 ? '#409EFF' : '#67C23A';
          }
        }
      }
    ]
  });

  // 来源页面分析图表
  referrerChart = echarts.init(document.getElementById('referrer-chart'));
  referrerChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      type: 'scroll'
    },
    series: [
      {
        name: '来源页面',
        type: 'pie',
        radius: ['30%', '70%'],
        center: ['40%', '50%'],
        roseType: 'area',
        data: [],
        label: {
          formatter: '{b}\n{c} ({d}%)'
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  });

  // API调用序列图表
  apiSequenceChart = echarts.init(document.getElementById('api-sequence-chart'));
  apiSequenceChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}'
    },
    series: [
      {
        type: 'sankey',
        layout: 'none',
        emphasis: {
          focus: 'adjacency'
        },
        data: [],
        links: []
      }
    ]
  });

  window.addEventListener('resize', () => {
    ipChart?.resize();
    urlChart?.resize();
    apiCategoryChart?.resize();
    anomalyChart?.resize();
    userAgentChart?.resize();
    heatmapChart?.resize();
    geoDistributionChart?.resize();
    httpMethodChart?.resize();
    weekdayChart?.resize();
    referrerChart?.resize();
    apiSequenceChart?.resize();
  });
};

// 更新图表数据
const updateCharts = () => {
  // 更新热力图
  if (heatmapData.value && Object.keys(heatmapData.value).length > 0) {
    const data: Array<[string, string, number]> = [];
    Object.entries(heatmapData.value).forEach(([key, value]) => {
      // 假设key格式为: "2023-01-01-13" (年-月-日-小时)
      const parts = key.split('-');
      if (parts.length === 4) {
        const date = `${parts[0]}-${parts[1]}-${parts[2]}`;
        const hour = parts[3];
        data.push([date, hour, value]);
      }
    });

    const days = [...new Set(data.map(item => item[0]))].sort();
    const hours = [...Array(24).keys()].map(h => h.toString().padStart(2, '0'));

    heatmapChart?.setOption({
      xAxis: { data: hours },
      yAxis: { data: days },
      series: [{ data: data.map(item => [item[1], days.indexOf(item[0]), item[2]]) }]
    });
  } else {
    const hours = [...Array(24).keys()].map(h => h.toString().padStart(2, '0'));
    const days = ['暂无数据'];

    heatmapChart?.setOption({
      xAxis: { data: hours },
      yAxis: { data: days },
      series: [{ data: [] }]
    });
  }

  // 更新IP地理位置分布图表
  if (ipStats.value?.geoDistribution && Object.keys(ipStats.value.geoDistribution).length > 0) {
    const data = Object.entries(ipStats.value.geoDistribution).map(([region, count]) => ({
      name: region && region.trim() !== '' ? region : '未知地区',
      value: count
    })).sort((a, b) => b.value - a.value);

    geoDistributionChart?.setOption({
      series: [{ data }]
    });
  } else {
    geoDistributionChart?.setOption({
      series: [{ data: [{ name: '暂无数据', value: 0 }] }]
    });
  }

  // 更新HTTP方法分布图表
  if (urlStats.value?.methodDistribution && Object.keys(urlStats.value.methodDistribution).length > 0) {
    const data = Object.entries(urlStats.value.methodDistribution).map(([method, count]) => ({
      name: method || '未知方法',
      value: count
    }));

    httpMethodChart?.setOption({
      series: [{ data }]
    });
  } else {
    httpMethodChart?.setOption({
      series: [{ data: [{ name: '暂无数据', value: 0 }] }]
    });
  }

  // 更新工作日/周末访问分布图表
  if (timeStats.value?.weekdayStats && Object.keys(timeStats.value.weekdayStats).length > 0) {
    const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
    const data = weekdays.map(day => timeStats.value?.weekdayStats?.[day] || 0);

    weekdayChart?.setOption({
      series: [{ data }]
    });
  } else {
    weekdayChart?.setOption({
      series: [{ data: [0, 0, 0, 0, 0, 0, 0] }]
    });
  }

  // 更新来源页面分析图表
  if (userBehaviorStats.value?.topReferrers && Object.keys(userBehaviorStats.value.topReferrers).length > 0) {
    const data = Object.entries(userBehaviorStats.value.topReferrers).map(([referrer, count]) => ({
      name: referrer && referrer.trim() !== '' ? (referrer.length > 30 ? referrer.substring(0, 27) + '...' : referrer) : '直接访问',
      value: count
    })).sort((a, b) => b.value - a.value).slice(0, 10);

    referrerChart?.setOption({
      series: [{ data }]
    });
  } else {
    referrerChart?.setOption({
      series: [{ data: [{ name: '暂无数据', value: 0 }] }]
    });
  }

  // 更新API调用序列图表
  if (urlStats.value?.apiSequence && Object.entries(urlStats.value.apiSequence).length > 0) {
    const nodes: Array<{name: string}> = [];
    const links: Array<{source: string, target: string, value: number}> = [];

    // 收集所有节点
    Object.entries(urlStats.value.apiSequence).forEach(([source, targets]) => {
      if (!nodes.some(node => node.name === source)) {
        nodes.push({ name: source });
      }

      targets.forEach(target => {
        if (!nodes.some(node => node.name === target)) {
          nodes.push({ name: target });
        }

        // 为简化图表，我们假设每个链接值为1
        // 实际应用中可能需要统计真实的调用次数
        const existingLink = links.find(link => link.source === source && link.target === target);
        if (existingLink) {
          existingLink.value += 1;
        } else {
          links.push({ source, target, value: 1 });
        }
      });
    });

    apiSequenceChart?.setOption({
      series: [{
        data: nodes,
        links: links
      }]
    });
  } else {
    apiSequenceChart?.setOption({
      series: [{
        data: [{ name: '暂无API调用序列数据' }],
        links: []
      }]
    });
  }

  // 更新IP图表
  if (ipStats.value?.topIPs && Object.keys(ipStats.value.topIPs).length > 0) {
    const ipsRaw = Object.keys(ipStats.value.topIPs);
    const ips = ipsRaw.map(ip => ip && ip.trim() !== '' ? ip : '未知IP');

    if (ips.length > 0) {
      const counts = ips.map(ip => {
        const originalKey = ip === '未知IP' ? '' : ip;
        return ipStats.value?.topIPs[originalKey] || 0;
      });

      // 组合数据并排序
      const data = ips.map((ip, index) => ({
        name: ip,
        value: counts[index]
      })).sort((a, b) => b.value - a.value).slice(0, 10);

      ipChart?.setOption({
        yAxis: { data: data.map(item => item.name) },
        series: [{ data: data.map(item => item.value) }]
      });
    } else {
      ipChart?.setOption({
        yAxis: { data: ['暂无数据'] },
        series: [{ data: [0] }]
      });
    }
  } else {
    ipChart?.setOption({
      yAxis: { data: ['暂无数据'] },
      series: [{ data: [0] }]
    });
  }

  // 更新URL图表
  if (urlStats.value?.topUrls && Object.keys(urlStats.value.topUrls).length > 0) {
    const urlsRaw = Object.keys(urlStats.value.topUrls);
    const urls = urlsRaw.map(url => url && url.trim() !== '' ? url : '未知URL');

    if (urls.length > 0) {
      const counts = urls.map(url => {
        const originalKey = url === '未知URL' ? '' : url;
        return urlStats.value?.topUrls[originalKey] || 0;
      });

      // 组合数据并排序
      const data = urls.map((url, index) => ({
        name: url,
        value: counts[index]
      })).sort((a, b) => b.value - a.value).slice(0, 10);

      urlChart?.setOption({
        yAxis: { data: data.map(item => item.name) },
        series: [{ data: data.map(item => item.value) }]
      });
    } else {
      urlChart?.setOption({
        yAxis: { data: ['暂无数据'] },
        series: [{ data: [0] }]
      });
    }
  } else {
    urlChart?.setOption({
      yAxis: { data: ['暂无数据'] },
      series: [{ data: [0] }]
    });
  }

  // 时间图表已移除，使用访问热力图和工作日/周末分布图替代

  // 更新API类别图表（新增）
  if (summary.value?.apiCategoryStats && Object.keys(summary.value.apiCategoryStats).length > 0) {
    const categories = Object.keys(summary.value.apiCategoryStats);

    if (categories.length > 0) {
      const data = categories.map(category => ({
        name: category || '未分类',
        value: summary.value.apiCategoryStats[category] || 0
      }));

      apiCategoryChart?.setOption({
        series: [{ data }]
      });
    } else {
      apiCategoryChart?.setOption({
        series: [{ data: [{ name: '暂无数据', value: 0 }] }]
      });
    }
  } else {
    apiCategoryChart?.setOption({
      series: [{ data: [{ name: '暂无数据', value: 0 }] }]
    });
  }

  // 更新热门路径图表（新增）
  if (summary.value?.popularPaths && Object.keys(summary.value.popularPaths).length > 0) {
    // 热门路径图表已移除，不需要更新
  }

  // 更新异常检测图表
  if (anomalyStats.value) {
    const anomalies = anomalyStats.value.stdAnomalies || {};
    const dates = Object.keys(anomalies).sort();
    const counts = dates.map(date => anomalies[date]);

    anomalyChart?.setOption({
      xAxis: { data: dates },
      series: [{
        data: counts,
        markLine: {
          data: [
            { name: '阈值', yAxis: anomalyStats.value.stdThreshold || 0 }
          ]
        }
      }],
      visualMap: {
        max: Math.max(...counts, 1)
      }
    });
  } else {
    anomalyChart?.setOption({
      xAxis: { data: ['暂无数据'] },
      series: [{ data: [0] }]
    });
  }

  // 更新用户代理分析图表
  if (userBehaviorStats.value && userBehaviorStats.value.uaStats) {
    const uaStats = userBehaviorStats.value.uaStats;
    const data = Object.keys(uaStats).map(ua => ({
      name: formatUserAgent(ua),
      value: uaStats[ua]
    }));

    userAgentChart?.setOption({
      series: [{ data }]
    });
  } else {
    userAgentChart?.setOption({
      series: [{ data: [{ name: '暂无数据', value: 0 }] }]
    });
  }

  // 更新热力图
  if (heatmapData.value && Object.keys(heatmapData.value).length > 0) {
    const data: [number, number, number][] = [];
    const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];

    let max = 0;
    for (const [key, value] of Object.entries(heatmapData.value)) {
      const [date, hour] = key.split('_');
      // 简化处理：假设键格式为 "日期_小时"
      // 在实际应用中可能需要根据日期计算星期几
      const dayIndex = Math.floor(Math.random() * 7); // 示例用随机数代替
      const hourIndex = parseInt(hour);

      if (!isNaN(hourIndex) && hourIndex >= 0 && hourIndex < 24) {
        const count = typeof value === 'number' ? value : 0;
        data.push([hourIndex, dayIndex, count]);
        max = Math.max(max, count);
      }
    }

    heatmapChart?.setOption({
      visualMap: {
        max: max || 10
      },
      series: [{
        data
      }]
    });
  } else {
    // 生成示例数据，实际应用中应删除
    const sampleData: [number, number, number][] = [];
    for (let i = 0; i < 7; i++) {
      for (let j = 0; j < 24; j++) {
        sampleData.push([j, i, Math.round(Math.random() * 10)]);
      }
    }

    heatmapChart?.setOption({
      series: [{
        data: sampleData
      }]
    });
  }
};

// 格式化用户代理字符串
const formatUserAgent = (ua: string): string => {
  if (!ua) return '未知设备';

  // 简化的用户代理检测
  if (ua.includes('Firefox')) return 'Firefox浏览器';
  if (ua.includes('Chrome') && !ua.includes('Edg')) return 'Chrome浏览器';
  if (ua.includes('Safari') && !ua.includes('Chrome')) return 'Safari浏览器';
  if (ua.includes('Edg')) return 'Edge浏览器';
  if (ua.includes('MSIE') || ua.includes('Trident')) return 'IE浏览器';
  if (ua.includes('iPhone')) return 'iPhone';
  if (ua.includes('Android')) return 'Android设备';
  if (ua.includes('iPad')) return 'iPad';

  return ua.substring(0, 20) + '...';
};

// 获取所有数据
const fetchAllData = async () => {
  loading.value = true;
  try {
    // 获取基础统计数据
    const res = await getAllStats();
    if (res) {
      if (res.summary) {
        summary.value = res.summary;
      }
      if (res.ipStats) {
        ipStats.value = res.ipStats;
      }
      if (res.urlStats) {
        urlStats.value = res.urlStats;
      }
      if (res.timeStats) {
        timeStats.value = res.timeStats;
      }
    }

    // 获取异常检测数据
    try {
      const anomalyRes = await getAnomalyStats();
      if (anomalyRes) {
        anomalyStats.value = anomalyRes as unknown as AnomalyStats;
      }
    } catch (error) {
      console.error('获取异常检测数据失败', error);
    }

    // 获取用户行为分析数据
    try {
      const userBehaviorRes = await getUserBehaviorStats();
      if (userBehaviorRes) {
        userBehaviorStats.value = userBehaviorRes as unknown as UserBehaviorStats;
      }
    } catch (error) {
      console.error('获取用户行为数据失败', error);
    }

    // 获取热力图数据
    try {
      const heatmapRes = await getHeatmapData();
      if (heatmapRes) {
        heatmapData.value = heatmapRes as Record<string, number>;
      }
    } catch (error) {
      console.error('获取热力图数据失败', error);
    }

    updateCharts();
  } catch (error) {
    ElMessage.error('获取数据失败，请稍后重试');
    console.error(error);
  } finally {
    loading.value = false;
  }
};

// 监听数据变化，更新图表
watch([ipStats, urlStats, timeStats, summary, anomalyStats, userBehaviorStats, heatmapData], () => {
  updateCharts();
});

// 组件挂载时初始化图表并获取数据
onMounted(() => {
  initCharts();
  fetchAllData();
});
</script>

<style scoped>

.dashboard-container {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 84px);
  overflow-y: auto;
  height: 100%;
  max-height: 100vh;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.dashboard-title {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.dashboard-cards {
  margin-bottom: 24px;
}

.dashboard-card {
  height: 100px;
  transition: all 0.3s;
  border: none;
}

.card-content {
  display: flex;
  height: 100%;
}

.card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100px;
  color: white;
  font-size: 24px;
}

.card-info {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.card-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.card-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.dashboard-charts {
  margin-bottom: 24px;
}

.chart-card {
  margin-bottom: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  border: none;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  height: 350px;
  padding: 10px;
}

:deep(.el-card__header) {
  padding: 16px;
  font-weight: bold;
  border-bottom: 1px solid #EBEEF5;
}

:deep(.el-card__body) {
  padding: 16px;
}

/* 动画效果 */
.dashboard-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.chart-card:hover {
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}
</style>
