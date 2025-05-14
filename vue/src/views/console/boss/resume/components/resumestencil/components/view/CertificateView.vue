<template>
  <div class="section">
    <TitleView title="个人证书" iconName="Document" />

    <!-- 证书列表 -->
    <div v-if="certificates && certificates.length > 0">
      <div class="certificate-item" v-for="(cert, index) in certificates" :key="'cert-'+index">
        <div class="certificate-header">
          <span class="certificate-name">{{ cert.name }}</span>
          <span class="certificate-date">{{ cert.date }}</span>
        </div>
        <div class="certificate-description">{{ cert.description }}</div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import TitleView from '../common/TitleView.vue'

interface Certificate {
  name: string;
  date: string;
  description: string;
}

defineProps<{
  certificates: Certificate[]
}>()
</script>

<style scoped>
.section {
  margin-bottom: 30px;
}

.certificate-item {
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #5d6d7e;
}

.certificate-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}

.certificate-name {
  font-weight: bold;
}

.certificate-date {
  color: #444444;
  font-size: 12px;
}

.certificate-description {
  font-size: 13px;
  color: #000000;
}
</style>
