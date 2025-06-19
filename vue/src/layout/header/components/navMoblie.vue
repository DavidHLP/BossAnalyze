<script setup lang="ts">
import outNav from '@/common/nav/outNav'
</script>

<template>
  <div class="header-moblie">
    <el-dropdown>
      <i class="iconfont icon-home ml-10 bold"></i>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item
            :key="idx"
            v-for="(navItem, idx) in outNav"
            @click="$router.push(navItem.path)"
            >{{ navItem.name }}</el-dropdown-item
          >
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<style lang="scss" scoped>
.header-moblie {
  height: 60px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  .icon-home {
    font-size: 1.8rem;
    outline: none;
  }
}
</style>
