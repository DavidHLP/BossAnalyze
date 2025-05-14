<template>
  <div class="section">
    <TitleView title="工作经验" iconName="OfficeBuilding" />
    <div class="timeline" v-for="(exp, index) in workExperience || defaultWorkExperience" :key="'work-'+index">
      <div class="timeline-item">
        <div class="time-range">{{ exp.startDate }} ~ {{ exp.endDate }}</div>
        <div class="institution">{{ exp.company }}</div>
        <div class="position">{{ exp.position }}</div>
        <div class="details">
          <div v-for="(duty, dIndex) in exp.duties" :key="'duty-'+dIndex">
            • {{ duty }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { defineProps, ref } from 'vue'
import TitleView from '../common/TitleView.vue'

// 类型定义
interface WorkExperience {
  startDate: string;
  endDate: string;
  company: string;
  position: string;
  duties: string[];
}

// Props 定义
defineProps<{
  workExperience?: WorkExperience[]
}>()

// 默认工作经验
const defaultWorkExperience = ref<WorkExperience[]>([
  {
    startDate: '2018-09',
    endDate: '至今',
    company: '全民简历科技有限公司',
    position: '行政专员',
    duties: [
      '负责公司行政人事管理和日常工作，以及员工培训、假期管理等工作的统筹',
      '公司规章制度，内部办公制度，保证上报下载下高效运行，负责公司文件归档的事项进行归档',
      '部门相关事，负责公司团队状况的制作',
      '公司各人事管理制度，配合各负责人审批。'
    ]
  },
  {
    startDate: '2016-09',
    endDate: '2018-08',
    company: '上海某学网络科技有限公司',
    position: '行政专员',
    duties: [
      '负责中心服务的对接、部门管控',
      '负责公司团队部门各种会议工作，负责引导新人，积极团队工作推广',
      '负责中心行政事务，公司证照管理，负责部门工作下属管理',
      '负责招聘工作，筛选人才和负责人小组面试及测验',
      '管理公司档案工作，人事档案，劳工档案，生日活动及公司档案管理会的活动的执行',
      '负责招聘工作，筛选公司的人力资源配件材料，编写人才聘用及相关标准及资料'
    ]
  }
])
</script>

<style scoped>
.section {
  margin-bottom: 30px;
}

.timeline-item {
  margin-bottom: 20px;
}

.time-range {
  color: #7f8c8d;
  font-size: 14px;
  margin-bottom: 5px;
}

.institution {
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 5px;
}

.position {
  color: #e74c3c;
  margin-bottom: 10px;
}

.details {
  font-size: 14px;
  line-height: 1.6;
}
</style>
