<template>
  <div id="app" class="app-container">
    <router-view></router-view>
  </div>
</template>

<script setup lang="ts">
// 这里可以放置一些全局的逻辑或引入
import AOS from 'aos'
import { onMounted } from 'vue'
import 'aos/dist/aos.css'

onMounted(() => AOS.init({ once: true }))
</script>

<style lang="scss">
</style>
