<template>
  <InstanceListPage :fetcher="pageMyInstance" storage-key="workflow-mine" />
</template>

<script setup>
import InstanceListPage from '@/components/workflow/InstanceListPage.vue'
import { pageMyInstance } from '@/api/workflow/instance'
</script>
