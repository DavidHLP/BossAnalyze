<template>
  <div>
    <MdPreview :id="id" :modelValue="content" :theme="theme" :codeTheme="codeTheme" :showCodeRowNumber="showCodeRowNumber" />
    <MdCatalog v-if="showCatalog" :editorId="id" :scrollElement="scrollElement" :scrollElementOffsetTop="0" :level="catalogLevel" />
  </div>
</template>

<script setup lang="ts">
import { MdPreview, MdCatalog } from 'md-editor-v3';
import type { Themes } from 'md-editor-v3';
// 只导入预览相关的样式
import 'md-editor-v3/lib/preview.css';
import { onMounted } from 'vue';

// 组件属性定义
const props = defineProps({
  // markdown 内容
  content: {
    type: String,
    default: ''
  },
  // 是否显示目录
  showCatalog: {
    type: Boolean,
    default: true
  },
  // 主题
  theme: {
    type: String as () => Themes,
    default: 'dark' as Themes
  },
  // 代码主题
  codeTheme: {
    type: String,
    default: 'atom'
  },
  // 是否显示代码行号
  showCodeRowNumber: {
    type: Boolean,
    default: true
  },
  // 目录层级限制
  catalogLevel: {
    type: Number,
    default: 5
  }
});

// 为预览组件生成唯一ID
const id = `markdown-preview-${Date.now()}`;
// 滚动元素
const scrollElement = document.documentElement;

// 挂载时进行初始化
onMounted(() => {
  // 在这里可以添加组件挂载时的处理逻辑
  // 例如自动滚动到顶部或处理目录定位
  if (props.content && scrollElement) {
    scrollElement.scrollTop = 0;
  }
});
</script>

<style scoped>
</style>
