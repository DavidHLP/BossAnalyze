<template>
  <div class="tags-container">
    <div class="tags">
      <span class="tag" v-for="(tag, index) in interestTags" :key="index">{{ tag }}</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
defineProps<{
  interestTags: string[]
}>()
</script>

<style scoped>
.tags-container {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  min-height: 100px; /* 最小高度 */
}

.tags {
  display: flex;
  flex-wrap: wrap;
}

.tag {
  background-color: #4a6785;
  border-radius: 15px;
  padding: 5px 10px;
  margin: 5px;
  font-size: 12px;
}
</style>
