<template>
  <ScreenRunner
    :screens="screens"
    :interval="interval"
    :fetcher="fetch"
    :company="company"
    :title-suffix="workCenterName"
    closable
    @close="close"
  />
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { screenData } from '@/api/bi/screen'
import { SCREENS } from '@/components/screen/screen'
import { useConfigStore } from '@/store/config'
import { useUserStore } from '@/store/user'
import ScreenRunner from './ScreenRunner.vue'

/**
 * 登录后打开的大屏：/screen/factory，多个大屏轮播 /screen/factory?rotate=factory,quality&interval=60，
 * 生产车间可按工作中心 ?workCenterId=1
 */
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const configStore = useConfigStore()

const screens = computed(() => {
  const keys = String(route.query.rotate || route.params.key || '').toUpperCase().split(',')
  const list = keys.map((k) => SCREENS.find((s) => s.key === k)).filter(Boolean)
  return list.length ? list : [SCREENS[0]]
})
const interval = computed(() => Number(route.query.interval) || 60)
const workCenterId = computed(() => (route.query.workCenterId ? Number(route.query.workCenterId) : undefined))
const company = computed(() => userStore.tenantName || configStore.systemName)
const workCenterName = ref('')

async function fetch(key) {
  const data = await screenData(key, key === 'FACTORY' ? { workCenterId: workCenterId.value } : undefined)
  if (key === 'FACTORY') workCenterName.value = data.workCenter || ''
  return data
}

function close() {
  if (window.history.length > 1) router.back()
  else router.push('/bi/screen')
}
</script>
