<template>
  <div class="drawer-content b-overlay-wrap">
    <!-- 公司头部信息 -->
    <div class="company-header">
      <h2 class="company-title">{{ companyInfo.companyName }}</h2>
      <el-tag type="success" effect="dark" class="position-tag mb-2">{{ companyInfo.positionName }}</el-tag>
      <div class="location-info d-flex align-items-center">
        <el-icon class="me-1"><Location /></el-icon>
        <span>{{ companyInfo.cityName }}</span>
      </div>
    </div>

    <!-- 信息部分 -->
    <div class="info-sections">
      <!-- 薪资与要求 -->
      <b-card no-body class="info-card mb-3">
        <b-card-header class="card-header d-flex align-items-center">
          <el-icon class="me-2"><Money /></el-icon>
          <span>薪资与要求</span>
        </b-card-header>
        <b-card-body class="p-0">
          <b-list-group flush>
            <b-list-group-item class="d-flex justify-content-between align-items-center">
              <div class="info-label text-muted">薪资</div>
              <div class="info-value salary fw-bold">{{ companyInfo.salary }}</div>
            </b-list-group-item>
            <b-list-group-item class="d-flex justify-content-between align-items-center">
              <div class="info-label text-muted">学历要求</div>
              <div class="info-value">{{ companyInfo.degree }}</div>
            </b-list-group-item>
            <b-list-group-item class="d-flex justify-content-between align-items-center">
              <div class="info-label text-muted">经验要求</div>
              <div class="info-value">{{ companyInfo.experience }}</div>
            </b-list-group-item>
          </b-list-group>
        </b-card-body>
      </b-card>

      <!-- 公司信息 -->
      <b-card no-body class="info-card mb-3">
        <b-card-header class="card-header d-flex align-items-center">
          <el-icon class="me-2"><OfficeBuilding /></el-icon>
          <span>公司信息</span>
        </b-card-header>
        <b-card-body class="p-0">
          <b-list-group flush>
            <b-list-group-item class="d-flex justify-content-between align-items-center">
              <div class="info-label text-muted">公司规模</div>
              <div class="info-value">{{ companyInfo.companySize }}</div>
            </b-list-group-item>
            <b-list-group-item class="d-flex justify-content-between align-items-center">
              <div class="info-label text-muted">融资阶段</div>
              <div class="info-value">{{ companyInfo.financingStage }}</div>
            </b-list-group-item>
            <b-list-group-item class="d-flex justify-content-between align-items-center">
              <div class="info-label text-muted">公司地址</div>
              <div class="info-value location" @click="openGoogleMaps">
                <el-icon class="me-1"><Location /></el-icon>
                <span>{{ companyInfo.address || '暂无地址信息' }}</span>
              </div>
            </b-list-group-item>
          </b-list-group>
        </b-card-body>
      </b-card>

      <!-- 员工福利 -->
      <b-card no-body class="info-card mb-3">
        <b-card-header class="card-header d-flex align-items-center">
          <el-icon class="me-2"><Present /></el-icon>
          <span>员工福利</span>
        </b-card-header>
        <b-card-body>
          <div class="benefits-container">
            <template v-if="companyInfo.employeeBenefits && companyInfo.employeeBenefits.length > 0">
              <b-badge
                v-for="(benefit, index) in companyInfo.employeeBenefits"
                :key="index"
                variant="primary"
                pill
                class="benefit-tag me-2 mb-2"
              >
                {{ benefit }}
              </b-badge>
            </template>
            <template v-else>
              <div class="empty-info text-center text-muted fst-italic">暂无福利信息</div>
            </template>
          </div>
        </b-card-body>
      </b-card>

      <!-- 职位要求 -->
      <b-card no-body class="info-card">
        <b-card-header class="card-header d-flex align-items-center">
          <el-icon class="me-2"><Document /></el-icon>
          <span>职位要求</span>
        </b-card-header>
        <b-card-body>
          <div class="requirements-container">
            <template v-if="companyInfo.jobRequirements && companyInfo.jobRequirements.length > 0">
              <div v-for="(requirement, index) in companyInfo.jobRequirements" :key="index" class="requirement-item d-flex mb-2">
                <div class="requirement-number d-flex align-items-center justify-content-center rounded-circle bg-primary text-white me-2">{{ index + 1 }}</div>
                <div class="requirement-text">{{ requirement }}</div>
              </div>
            </template>
            <template v-else>
              <div class="empty-info text-center text-muted fst-italic">暂无要求信息</div>
            </template>
          </div>
        </b-card-body>
      </b-card>
    </div>

    <!-- 底部按钮 -->
    <div class="drawer-footer d-flex shadow-sm">
      <b-button v-if="companyInfo.companyUrl" variant="primary" class="me-2 flex-grow-1" @click="openCompanyUrl">
        <el-icon class="me-1"><Link /></el-icon> 查看公司详情
      </b-button>
      <b-button v-if="companyInfo.jobUrl" variant="success" class="flex-grow-1" @click="openJobUrl">
        <el-icon class="me-1"><View /></el-icon> 查看职位详情
      </b-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { defineProps, defineEmits } from 'vue';
import type { CompanyInfo } from '@/api/boss/user/user.d';
import {
  ElIcon,
  ElTag
} from 'element-plus';
import {
  Location,
  Money,
  OfficeBuilding,
  Link,
  View,
  Present,
  Document
} from '@element-plus/icons-vue';
// 导入Bootstrap Vue Next组件
import {
  BCard,
  BCardHeader,
  BCardBody,
  BListGroup,
  BListGroupItem,
  BBadge,
  BButton
} from 'bootstrap-vue-next';

defineProps<{
  companyInfo: CompanyInfo;
}>();

const emit = defineEmits<{
  (e: 'openCompanyUrl'): void;
  (e: 'openJobUrl'): void;
  (e: 'openGoogleMaps'): void;
}>();

// 打开公司URL
const openCompanyUrl = () => {
  emit('openCompanyUrl');
};

// 打开职位URL
const openJobUrl = () => {
  emit('openJobUrl');
};

// 添加打开地图的方法
const openGoogleMaps = () => {
  emit('openGoogleMaps');
};
</script>

<style lang="scss" scoped>
.drawer-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0;
  background-color: #f9fafc;
  font-family: system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.company-header {
  padding: 2rem 1.75rem;
  background: linear-gradient(135deg, #4268F6, #5733EA);
  color: #fff;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border-radius: 0 0 16px 16px;

  .company-title {
    margin: 0 0 1rem 0;
    font-size: 1.75rem;
    font-weight: 700;
    line-height: 1.3;
    letter-spacing: -0.01em;
  }

  .position-tag {
    margin-bottom: 0.75rem;
    font-size: 0.875rem;
    border: none;
    padding: 0.35rem 0.75rem;
    border-radius: 6px;
  }

  .location-info {
    font-size: 0.95rem;
    opacity: 0.92;
    display: inline-flex;
    align-items: center;
    padding: 0.25rem 0.5rem;
    background-color: rgba(255, 255, 255, 0.15);
    border-radius: 6px;
    backdrop-filter: blur(4px);
  }
}

.info-sections {
  flex: 1;
  padding: 1.25rem;
  overflow-y: auto;
  scroll-behavior: smooth;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 10px;
  }

  &::-webkit-scrollbar-thumb {
    background: #d1d5db;
    border-radius: 10px;
    transition: background 0.3s;

    &:hover {
      background: #9ca3af;
    }
  }
}

.info-card {
  border: 0;
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  margin-bottom: 1.25rem;
  background-color: #fff;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  }

  .card-header {
    background-color: #f9fafc;
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    font-weight: 600;
    color: #374151;
    padding: 1rem 1.25rem;
    font-size: 1.05rem;
  }
}

.info-label {
  font-size: 0.9rem;
  color: #6b7280;
}

.info-value {
  font-weight: 500;
  color: #111827;

  &.salary {
    color: #4268F6;
    font-size: 1.25rem;
    font-weight: 700;
  }

  &.location {
    display: flex;
    align-items: center;
    color: #4268F6;
    cursor: pointer;
    transition: all 0.2s;
    border-radius: 6px;
    padding: 0.25rem 0.5rem;

    &:hover {
      background-color: rgba(66, 104, 246, 0.08);
      transform: translateX(2px);
    }
  }
}

.benefits-container {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.benefit-tag {
  font-weight: 500;
  font-size: 0.85rem;
  padding: 0.45rem 0.85rem;
  background-color: rgba(66, 104, 246, 0.08);
  color: #4268F6;
  border-radius: 8px;
  border: none;
  transition: all 0.2s;

  &:hover {
    background-color: rgba(66, 104, 246, 0.15);
    transform: translateY(-2px);
  }
}

.requirement-item {
  margin-bottom: 0.85rem;
  background-color: #f9fafc;
  padding: 0.75rem;
  border-radius: 8px;
  transition: all 0.2s;

  &:hover {
    background-color: #f3f4f6;
    transform: translateX(3px);
  }
}

.requirement-number {
  width: 28px;
  height: 28px;
  min-width: 28px;
  background-color: #4268F6;
  font-size: 0.85rem;
  font-weight: 600;
  border-radius: 50%;
  margin-right: 0.75rem;
}

.requirement-text {
  font-size: 0.95rem;
  line-height: 1.6;
  align-self: center;
}

.empty-info {
  padding: 1.5rem;
  color: #9ca3af;
  font-size: 0.95rem;
}

.drawer-footer {
  padding: 1.25rem 1.75rem;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  background-color: #fff;
  gap: 1rem;
}

.b-button {
  border-radius: 8px;
  padding: 0.65rem 1rem;
  font-weight: 500;
  transition: all 0.2s;

  &:active {
    transform: scale(0.98);
  }
}

// 响应式调整
@media (max-width: 768px) {
  .company-header {
    padding: 1.5rem;
    border-radius: 0 0 12px 12px;

    .company-title {
      font-size: 1.35rem;
    }
  }

  .info-sections {
    padding: 1rem;
  }

  .drawer-footer {
    padding: 1rem;
    flex-direction: column;

    .b-button {
      margin: 0.25rem 0;
      width: 100%;
    }
  }

  .info-card {
    margin-bottom: 1rem;
    border-radius: 10px;
  }
}
</style>
