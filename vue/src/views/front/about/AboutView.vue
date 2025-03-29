<template>
  <div class="about-container">
    <el-row :gutter="20">
      <!-- 左侧内容 -->
      <el-col :span="16">
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <h2>关于我们</h2>
              <el-tag type="success" effect="dark">Spring Cloud 微服务架构</el-tag>
            </div>
          </template>

          <div class="content-section">
            <h3>项目简介</h3>
            <p>这是一个基于Spring Cloud Gateway的网关服务模拟实现项目。本项目主要展示了Spring Cloud Gateway的核心功能，包括动态路由配置、请求过滤、限流熔断、安全认证等特性。通过该项目，您可以深入理解Spring Cloud Gateway的工作原理和最佳实践。</p>

            <h3>技术栈</h3>
            <el-row :gutter="20">
              <el-col :span="12">
                <div class="tech-stack">
                  <h4>后端技术</h4>
                  <el-tag v-for="tech in backendTech"
                         :key="tech"
                         class="tech-tag"
                         :type="getTagType(tech)">
                    {{ tech }}
                  </el-tag>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="tech-stack">
                  <h4>前端技术</h4>
                  <el-tag v-for="tech in frontendTech"
                         :key="tech"
                         class="tech-tag"
                         :type="getTagType(tech)">
                    {{ tech }}
                  </el-tag>
                </div>
              </el-col>
            </el-row>

            <h3>系统架构</h3>
            <div class="architecture-diagram">
              <div class="diagram-header">
                <el-button
                  type="primary"
                  :icon="isFullscreen ? 'Aim' : 'FullScreen'"
                  circle
                  size="small"
                  @click="toggleFullScreen"
                />
              </div>
              <VueFlow
                :nodes="nodes"
                :edges="edges"
                :default-viewport="{ zoom: 1.5 }"
                :min-zoom="0.2"
                :max-zoom="4"
                class="flow-diagram"
                ref="vueFlowRef"
              >
                <template #node-service="nodeProps">
                  <ServiceNode v-bind="nodeProps" />
                </template>
                <template #edge-service="edgeProps">
                  <ServiceEdge v-bind="edgeProps" />
                </template>
              </VueFlow>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧信息 -->
      <el-col :span="8">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <h2>联系方式</h2>
            </div>
          </template>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="邮箱">
              <el-link type="primary" href="mailto:contact@example.com">
                contact@example.com
              </el-link>
            </el-descriptions-item>
            <el-descriptions-item label="GitHub">
              <el-link type="primary" href="https://github.com/yourusername" target="_blank">
                github.com/yourusername
              </el-link>
            </el-descriptions-item>
            <el-descriptions-item label="版本">
              v1.0.0
            </el-descriptions-item>
            <el-descriptions-item label="许可证">
              MIT License
            </el-descriptions-item>
          </el-descriptions>

          <div class="feature-list">
            <h3>主要特性</h3>
            <el-timeline>
              <el-timeline-item
                v-for="(feature, index) in features"
                :key="index"
                :type="feature.type"
                :timestamp="feature.title">
                {{ feature.content }}
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import type { Node, Edge } from '@vue-flow/core'
import { VueFlow } from '@vue-flow/core'
import { FullScreen, Aim } from '@element-plus/icons-vue'
import ServiceNode from '@/components/custom/ServiceNode.vue'
import ServiceEdge from '@/components/custom/ServiceEdge.vue'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'

const isFullscreen = ref(false)
const vueFlowRef = ref()

// 定义handleFullscreenChange函数
const handleFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
  // 全屏状态变化时重新计算图表大小
  setTimeout(() => {
    vueFlowRef.value?.fitView()
  }, 100)
}

const toggleFullScreen = async () => {
  if (!isFullscreen.value) {
    const element = document.querySelector('.architecture-diagram')
    if (element?.requestFullscreen) {
      await element.requestFullscreen()
      // 等待DOM更新后重新计算图表大小
      setTimeout(() => {
        vueFlowRef.value?.fitView()
      }, 100)
    }
  } else {
    if (document.exitFullscreen) {
      await document.exitFullscreen()
      // 退出全屏后重新计算图表大小
      setTimeout(() => {
        vueFlowRef.value?.fitView()
      }, 100)
    }
  }
}

onMounted(() => {
  document.addEventListener('fullscreenchange', handleFullscreenChange)
})

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})

// 技术栈数据
const backendTech = [
  'Spring Cloud',
  'Spring Boot',
  'Spring Security',
  'MyBatis-Plus',
  'MySQL',
  'Redis',
  'RabbitMQ',
  'Nacos'
]

const frontendTech = [
  'Vue 3',
  'TypeScript',
  'Element Plus',
  'Vue Flow',
  'Pinia',
  'Vue Router',
  'Axios',
  'SCSS'
]

// 特性列表
const features = [
  {
    title: '动态路由',
    content: '支持基于Nacos的动态路由配置，实现路由规则的实时更新',
    type: 'primary'
  },
  {
    title: '网关过滤器',
    content: '实现请求/响应过滤、请求重写、响应装饰等自定义过滤器链',
    type: 'success'
  },
  {
    title: '限流熔断',
    content: '集成Sentinel实现限流、熔断、系统保护等治理功能',
    type: 'warning'
  },
  {
    title: '安全认证',
    content: '提供JWT令牌验证、权限控制、黑白名单等安全机制',
    type: 'info'
  }
]

// 获取标签类型
const getTagType = (tech: string) => {
  const types = ['', 'success', 'warning', 'info', 'danger']
  return types[Math.floor(Math.random() * types.length)]
}

// Vue Flow 节点
const nodes = ref<Node[]>([
  {
    id: '1',
    type: 'service',
    position: { x: 250, y: 25 },
    data: { label: '客户端', type: '前端' },
  },
  {
    id: '2',
    type: 'service',
    position: { x: 250, y: 125 },
    data: { label: 'Gateway网关', type: '网关' },
  },
  {
    id: '3',
    type: 'service',
    position: { x: 100, y: 225 },
    data: { label: 'Nacos配置中心', type: '配置' },
  },
  {
    id: '4',
    type: 'service',
    position: { x: 250, y: 225 },
    data: { label: 'Sentinel控制台', type: '限流' },
  },
  {
    id: '5',
    type: 'service',
    position: { x: 400, y: 225 },
    data: { label: '认证中心', type: '认证' },
  },
  {
    id: '6',
    type: 'service',
    position: { x: 100, y: 325 },
    data: { label: '服务A', type: '微服务' },
  },
  {
    id: '7',
    type: 'service',
    position: { x: 250, y: 325 },
    data: { label: '服务B', type: '微服务' },
  },
  {
    id: '8',
    type: 'service',
    position: { x: 400, y: 325 },
    data: { label: '服务C', type: '微服务' },
  },
])

// Vue Flow 边
const edges = ref<Edge[]>([
  {
    id: 'e1-2',
    type: 'service',
    source: '1',
    target: '2',
    animated: true,
    data: { label: '请求' },
  },
  {
    id: 'e2-3',
    type: 'service',
    source: '2',
    target: '3',
    animated: true,
    data: { label: '路由配置' },
  },
  {
    id: 'e2-4',
    type: 'service',
    source: '2',
    target: '4',
    animated: true,
    data: { label: '限流规则' },
  },
  {
    id: 'e2-5',
    type: 'service',
    source: '2',
    target: '5',
    animated: true,
    data: { label: '认证授权' },
  },
  {
    id: 'e2-6',
    type: 'service',
    source: '2',
    target: '6',
    animated: true,
    data: { label: '路由转发' },
  },
  {
    id: 'e2-7',
    type: 'service',
    source: '2',
    target: '7',
    animated: true,
    data: { label: '路由转发' },
  },
  {
    id: 'e2-8',
    type: 'service',
    source: '2',
    target: '8',
    animated: true,
    data: { label: '路由转发' },
  },
])
</script>

<style scoped lang="scss">
.about-container {
  padding: 20px;
  min-height: calc(100vh - 40px);
  background-color: var(--el-bg-color-page);

  .el-row {
    margin-bottom: 0;
  }
}

.content-card, .info-card {
  margin-bottom: 18px;
  border-radius: 8px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 18px;
    border-bottom: 1px solid var(--el-border-color-light);

    h2 {
      margin: 0;
      color: var(--el-text-color-primary);
      font-size: 18px;
      font-weight: 600;
      letter-spacing: -0.5px;
    }

    .el-tag {
      padding: 5px 10px;
      font-size: 12px;
      font-weight: 500;
      border-radius: 6px;
    }
  }
}

.content-section {
  padding: 18px;

  h3 {
    color: var(--el-text-color-primary);
    margin: 20px 0 14px;
    font-size: 16px;
    font-weight: 600;
    letter-spacing: -0.5px;

    &:first-child {
      margin-top: 0;
    }
  }

  p {
    color: var(--el-text-color-regular);
    line-height: 1.6;
    margin-bottom: 20px;
    font-size: 13px;
  }
}

.tech-stack {
  margin-bottom: 18px;

  h4 {
    color: var(--el-text-color-regular);
    margin-bottom: 10px;
    font-size: 14px;
    font-weight: 500;
  }

  .tech-tag {
    margin: 0 5px 5px 0;
    padding: 4px 8px;
    font-size: 12px;
    border-radius: 4px;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
    }
  }
}

.architecture-diagram {
  position: relative;
  height: 324px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: hidden;
  margin-top: 18px;
  background: var(--el-bg-color-page);

  .diagram-header {
    position: absolute;
    top: 10px;
    right: 10px;
    z-index: 10;
    display: flex;
    gap: 8px;
    padding: 4px;
    border-radius: 4px;
    background: rgba(255, 255, 255, 0.9);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    transition: all 0.3s ease;

    .el-button {
      padding: 6px;
      font-size: 16px;
    }
  }

  .flow-diagram {
    width: 100%;
    height: 100%;
  }

  &:fullscreen {
    padding: 20px;
    background: var(--el-bg-color);

    .diagram-header {
      background: var(--el-bg-color);
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
    }

    .flow-diagram {
      height: calc(100vh - 40px);
    }
  }
}

.info-card {
  .el-descriptions {
    margin: 18px;

    :deep(.el-descriptions__title) {
      font-size: 14px;
      font-weight: 500;
    }

    :deep(.el-descriptions__label) {
      width: 72px;
      color: var(--el-text-color-regular);
    }

    :deep(.el-descriptions__content) {
      color: var(--el-text-color-primary);
    }
  }

  .feature-list {
    padding: 0 18px 18px;

    h3 {
      color: var(--el-text-color-primary);
      margin-bottom: 14px;
      font-size: 15px;
      font-weight: 600;
    }
  }

  :deep(.el-timeline) {
    padding-left: 10px;

    .el-timeline-item__node {
      background-color: var(--el-color-primary);
      border: 2px solid var(--el-bg-color);
    }

    .el-timeline-item__wrapper {
      padding-left: 14px;
    }

    .el-timeline-item__timestamp {
      color: var(--el-text-color-primary);
      font-size: 13px;
      font-weight: 500;
      margin-bottom: 5px;
    }

    .el-timeline-item__content {
      color: var(--el-text-color-regular);
      font-size: 12px;
      line-height: 1.4;
    }
  }
}

// 暗色模式适配
@media (prefers-color-scheme: dark) {
  .about-container {
    background-color: var(--el-bg-color-page);
  }

  .content-card, .info-card {
    background-color: var(--el-bg-color-overlay);
    border-color: var(--el-border-color-darker);
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);

    &:hover {
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.25);
    }
  }

  .architecture-diagram {
    border-color: var(--el-border-color-darker);
    background-color: var(--el-bg-color);

    .diagram-header {
      background: rgba(0, 0, 0, 0.6);
    }

    &:fullscreen {
      .diagram-header {
        background: var(--el-bg-color-overlay);
      }
    }
  }
}

// 响应式布局
@media screen and (max-width: 1200px) {
  .about-container {
    padding: 18px;
  }

  .el-col {
    width: 100% !important;
  }

  .architecture-diagram {
    height: 288px;
  }
}

@media screen and (max-width: 768px) {
  .about-container {
    padding: 14px;
  }

  .content-card, .info-card {
    margin-bottom: 14px;

    .card-header {
      padding: 12px;
      flex-direction: column;
      align-items: flex-start;
      gap: 8px;
    }
  }

  .content-section {
    padding: 12px;

    h3 {
      font-size: 15px;
      margin: 18px 0 12px;
    }
  }

  .architecture-diagram {
    height: 252px;
  }

  .info-card {
    .el-descriptions,
    .feature-list {
      padding: 12px;
    }
  }
}
</style>
