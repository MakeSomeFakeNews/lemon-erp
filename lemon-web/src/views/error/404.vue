<template>
  <div class="page-card not-found">
    <a-result status="404" title="页面不存在" sub-title="你访问的页面不存在，或者你没有访问权限。">
      <template #extra>
        <a-space>
          <a-button @click="$router.back()">返回上一页</a-button>
          <a-button type="primary" @click="$router.replace('/')">回到首页</a-button>
        </a-space>
      </template>
    </a-result>
  </div>
</template>

<style scoped>
.not-found {
  background: var(--app-card);
  padding: 48px 16px;
}
</style>
