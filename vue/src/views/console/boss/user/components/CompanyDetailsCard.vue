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
  background-color: var(--bs-light, #f8f9fa);
}

.company-header {
  padding: 1.5rem;
  background: linear-gradient(135deg, var(--bs-primary, #0d6efd), var(--bs-primary-dark, #0a58ca));
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

  .company-title {
    margin: 0 0 0.75rem 0;
    font-size: 1.5rem;
    font-weight: 600;
    line-height: 1.3;
  }

  .position-tag {
    margin-bottom: 0.75rem;
    font-size: 0.875rem;
    border: none;
  }

  .location-info {
    font-size: 0.875rem;
    opacity: 0.9;
  }
}

.info-sections {
  flex: 1;
  padding: 1rem;
  overflow-y: auto;
}

.info-card {
  border: 0;
  border-radius: 0.5rem;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  }

  .card-header {
    background-color: rgba(var(--bs-primary-rgb, 13, 110, 253), 0.05);
    border-bottom: 1px solid rgba(var(--bs-primary-rgb, 13, 110, 253), 0.1);
    font-weight: 600;
    color: var(--bs-gray-700);
    padding: 0.75rem 1rem;
  }
}

.info-value {
  font-weight: 500;

  &.salary {
    color: var(--bs-primary, #0d6efd);
    font-size: 1.125rem;
  }

  &.location {
    display: flex;
    align-items: center;
    color: var(--bs-primary, #0d6efd);
    cursor: pointer;
    transition: opacity 0.2s;

    &:hover {
      opacity: 0.8;
    }
  }
}

.benefits-container {
  display: flex;
  flex-wrap: wrap;
}

.benefit-tag {
  font-weight: normal;
  font-size: 0.8125rem;
  padding: 0.35rem 0.75rem;
  background-color: rgba(var(--bs-primary-rgb, 13, 110, 253), 0.1);
}

.requirement-number {
  width: 24px;
  height: 24px;
  min-width: 24px;
  background-color: var(--bs-primary, #0d6efd);
  font-size: 0.75rem;
}

.requirement-text {
  font-size: 0.9375rem;
  line-height: 1.6;
}

.drawer-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  background-color: #fff;
}

// 响应式调整
@media (max-width: 768px) {
  .company-header {
    padding: 1rem;

    .company-title {
      font-size: 1.25rem;
    }
  }

  .info-sections {
    padding: 0.75rem;
  }

  .drawer-footer {
    padding: 0.75rem 1rem;
    flex-direction: column;

    .b-button {
      margin: 0.25rem 0;
    }
  }
}
</style>
