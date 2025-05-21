<template>
  <div class="section">
    <TitleView title="自我评价" iconName="User" />
    <div class="self-evaluation">
      {{ evaluation }}
    </div>
  </div>
</template>

<script lang="ts" setup>
import { defineProps } from 'vue';
import TitleView from '../common/TitleView.vue';
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
