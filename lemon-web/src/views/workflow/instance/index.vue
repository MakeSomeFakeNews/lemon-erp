<template>
  <InstanceListPage :fetcher="pageInstance" storage-key="workflow-instance" manage />
</template>

<script setup>
import InstanceListPage from '@/components/workflow/InstanceListPage.vue'
import { pageInstance } from '@/api/workflow/instance'
</script>
