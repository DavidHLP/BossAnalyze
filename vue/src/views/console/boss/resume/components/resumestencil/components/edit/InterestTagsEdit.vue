<template>
  <div>
    <el-form-item>
      <el-tag
        :key="tag"
        v-for="tag in interestTags"
        closable
        :disable-transitions="false"
        @close="handleTagClose(tag)">
        {{tag}}
      </el-tag>
      <el-input
        class="input-new-tag"
        v-if="inputVisible"
        v-model="inputValue"
        ref="saveTagInput"
        size="small"
        @keyup.enter="handleInputConfirm"
        @blur="handleInputConfirm"
      >
      </el-input>
      <el-button v-else class="button-new-tag" size="small" @click="showInput">+ 添加兴趣爱好</el-button>
    </el-form-item>
  </div>
</template>

<script lang="ts" setup>
import { ref, nextTick, defineProps, defineEmits } from 'vue'

// 使用v-model方式接收并更新props
const props = defineProps<{
  interestTags: string[]
}>()

// 定义emit来更新父组件的值
const emit = defineEmits<{
  (e: 'update:interestTags', value: string[]): void
}>()

const inputVisible = ref(false)
const inputValue = ref('')
const saveTagInput = ref<HTMLInputElement | null>(null)

// 标签相关方法
const handleTagClose = (tag: string) => {
  const updatedTags = [...props.interestTags]
  updatedTags.splice(updatedTags.indexOf(tag), 1)
  emit('update:interestTags', updatedTags)
}

const showInput = () => {
  inputVisible.value = true
  nextTick(() => {
    saveTagInput.value?.focus()
  })
}

const handleInputConfirm = () => {
  if (inputValue.value) {
    const updatedTags = [...props.interestTags, inputValue.value]
    emit('update:interestTags', updatedTags)
  }
  inputVisible.value = false
  inputValue.value = ''
}
</script>

<style lang="scss" scoped>
@use "../style/edittheme.scss";
</style>
