<template>
  <div class="page-card not-found">
    <a-result status="404" :title="$t('login.notFound.title')" :sub-title="$t('login.notFound.subTitle')">
      <template #extra>
        <a-space>
          <a-button @click="$router.back()">{{ $t('login.notFound.back') }}</a-button>
          <a-button type="primary" @click="$router.replace('/')">{{ $t('login.notFound.home') }}</a-button>
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
