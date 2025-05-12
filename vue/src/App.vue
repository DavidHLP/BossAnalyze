<template>
  <div id="app" class="app-container">
    <router-view></router-view>

    <!-- 全局Snackbar消息提示 -->
    <v-snackbar
      v-model="snackbar.visible"
      :color="snackbar.color"
      :timeout="snackbar.timeout"
    >
      <span class="snackbar-text">{{ snackbar.text }}</span>
      <template v-slot:actions>
        <v-btn
          variant="text"
          @click="hideSnackbar"
        >
          关闭
        </v-btn>
      </template>
    </v-snackbar>
  </div>
</template>

<script setup lang="ts">
// 这里可以放置一些全局的逻辑或引入
// 引入Snackbar组件
import { useSnackbar } from '@/composables/useSnackbar'
const { snackbar, hideSnackbar } = useSnackbar()
</script>

<style lang="scss">
</style>
