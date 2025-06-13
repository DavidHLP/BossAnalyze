<template>
  <div class="home-container">
    <div class="home-header">
      <h1>首页</h1>
      <p class="subtitle">欢迎使用我们的系统</p>
    </div>
  </div>
</template>

<script setup lang="ts">
// 首页组件逻辑可在此处添加
</script>

<style lang="scss">

.home-container {
  padding: 2rem;
  background-color: var(--background-color);
  min-height: 100vh;
}

.home-header {
  text-align: center;
  margin-bottom: 3rem;

  h1 {
    color: var(--primary-color);
    font-size: 2.5rem;
    margin-bottom: 0.5rem;
  }

  .subtitle {
    color: var(--text-secondary);
    font-size: 1.2rem;
  }
}

.card-section {
  display: flex;
  flex-wrap: wrap;
  gap: 2rem;
  justify-content: center;
}

.feature-card {
  padding: 1.5rem;
  width: 300px;

  h3 {
    color: var(--primary-dark);
    margin-bottom: 1rem;
  }

  p {
    color: var(--text-primary);
    margin-bottom: 1.5rem;
    line-height: 1.5;
  }

  button {
    padding: 0.5rem 1rem;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-weight: 500;
  }
}
</style>
