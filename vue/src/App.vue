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
   height: 95vh; /* 视口高度 */
   width: 99vw;  /* 视口宽度 */
   display: flex;
   justify-content: center; /* 水平居中 */
   align-items: flex-start; /* 垂直顶部对齐 */
   overflow-y: auto; /* 允许垂直滚动 */
   overflow-x: hidden; /* 隐藏水平滚动条 */
   padding: 10px 0; /* 添加上下内边距 */
 }
</style>
