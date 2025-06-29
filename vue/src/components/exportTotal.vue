<script setup lang="ts">
import { getExportCount } from '@/api/modules/resume'
import { onActivated, ref } from 'vue'

const total = ref(0)
onActivated(() => {
  ;(async () => {
    try {
      const count = await getExportCount()
      // 处理可能的非JSON数据
      if (typeof count === 'string') {
        const numericValue = parseInt(count, 10)
        total.value = isNaN(numericValue) ? 0 : numericValue
      } else if (typeof count === 'number') {
        total.value = count
      } else {
        total.value = 0
      }
    } catch (error) {
      console.warn('获取导出统计失败:', error)
      total.value = 0
    }
  })()
})
</script>

<template>
  <div class="export-total" v-show="total">
    累计导出 <strong>{{ total }}</strong> 份
  </div>
</template>

<style lang="scss" scoped>
.export-total {
  font-size: 0.7rem;
  margin: 0 20px;

  strong {
    color: var(--strong-color);
  }
}
</style>
