<template>
  <div class="section">
    <TitleView title="自我评价" iconName="User" />
    <div class="self-evaluation">
      {{ evaluation || defaultEvaluation }}
    </div>
  </div>
</template>

<script lang="ts" setup>
import { defineProps } from 'vue';
import TitleView from '../common/TitleView.vue';

const defaultEvaluation = '工作积极认真，细心负责，熟练运用公众自动化软件，善于在工作中提出创意，发现问题，整理流程，有较强的分析能力，易融于学习，接受新知；好学勤力强，认真负责，有良好的团队合作精神；诚实守信，吃苦耐劳，宽以待人。';

defineProps<{
  evaluation?: string;
}>();
</script>

<style scoped>
.section {
  margin-bottom: 30px;
}

.self-evaluation {
  font-size: 14px;
  line-height: 1.6;
}
</style>
