<template>
  <LoginComponent class="login-page"/>
</template>

<script setup lang="ts" name="LoginView">
import LoginComponent from '@/components/LoginComponent.vue'
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100vw;
}
</style>
