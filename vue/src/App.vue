<template>
  <div id="app">
    <router-view></router-view>

    <!-- 全局Snackbar消息提示 -->
    <v-snackbar
      v-model="snackbar.visible"
      :color="snackbar.color"
      :timeout="snackbar.timeout"
    >
      {{ snackbar.text }}
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

<style scoped lang="scss">
#app {
   height: 98vh; /* 视口高度 */
   width: 98vw;  /* 视口宽度 */
   display: flex;
   justify-content: center; /* 水平居中 */
   align-items: center;     /* 垂直居中 */
   overflow: hidden; /* 隐藏下拉条 */
 }
</style>
