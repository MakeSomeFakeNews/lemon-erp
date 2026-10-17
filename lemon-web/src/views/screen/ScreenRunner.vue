<template>
  <ScreenShell
    :title="title"
    :subtitle="subtitle"
    :company="company"
    :updated-at="updatedAt"
    :error="error"
    :screens="namedScreens"
    :current="index"
    :playing="playing"
    :closable="closable"
    @switch="go"
    @toggle-play="playing = !playing"
    @close="emit('close')"
  >
    <component :is="COMPONENTS[current?.key]" v-if="current && data[current.key]" :data="data[current.key]" />
    <div v-else class="loading">{{ error || $t('screen.runner.loading') }}</div>
  </ScreenShell>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { useI18n } from 'vue-i18n'
import { SCREENS } from '@/components/screen/screen'
import ScreenShell from '@/components/screen/ScreenShell.vue'
import FactoryScreen from './FactoryScreen.vue'
import QualityScreen from './QualityScreen.vue'
import SalesScreen from './SalesScreen.vue'
import WarehouseScreen from './WarehouseScreen.vue'

/**
 * 大屏运行：显示 screens 中的大屏，多个时按 interval 秒轮播；当前大屏每 30 秒刷新一次数据，
 * 请求失败时保留上次的数据并在标题栏提示离线，恢复后自动继续
 * - fetcher(key)：取大屏数据
 * - titleSuffix：标题后缀，如工作中心名称
 */
const props = defineProps({
  screens: { type: Array, required: true },
  interval: { type: Number, default: 60 },
  fetcher: { type: Function, required: true },
  company: String,
  titleSuffix: String,
  closable: Boolean
})
const emit = defineEmits(['close'])
const { t } = useI18n()

const COMPONENTS = { FACTORY: FactoryScreen, WAREHOUSE: WarehouseScreen, SALES: SalesScreen, QUALITY: QualityScreen }
const REFRESH_MS = 30000

const index = ref(0)
const playing = ref(true)
const data = reactive({})
const loadedAt = reactive({})
const error = ref('')
const current = computed(() => props.screens[index.value])
/** 大屏名称优先用前端的译文 */
const screenName = (s) => (s ? SCREENS.find((x) => x.key === s.key)?.label || s.label || '' : '')
const namedScreens = computed(() => props.screens.map((s) => ({ ...s, label: screenName(s) })))
const title = computed(() => `${screenName(current.value)}${current.value?.key === 'FACTORY' && props.titleSuffix ? ` · ${props.titleSuffix}` : ''}`)
const subtitle = computed(() => (current.value?.key === 'FACTORY' && props.titleSuffix ? '' : t('screen.runner.realtime')))
const updatedAt = computed(() => (current.value && loadedAt[current.value.key] ? dayjs(loadedAt[current.value.key]).format('HH:mm:ss') : ''))

async function load(force = false) {
  const key = current.value?.key
  if (!key) return
  if (!force && loadedAt[key] && Date.now() - loadedAt[key] < REFRESH_MS) return
  try {
    data[key] = await props.fetcher(key)
    loadedAt[key] = Date.now()
    error.value = ''
  } catch (e) {
    error.value = loadedAt[key] ? t('screen.runner.offline', { msg: e.message, time: dayjs(loadedAt[key]).format('HH:mm') }) : e.message
  }
}

function go(i) {
  index.value = i
  restartRotation()
}

let refreshTimer
let rotateTimer
function restartRotation() {
  clearInterval(rotateTimer)
  if (props.screens.length > 1) {
    rotateTimer = setInterval(() => {
      if (playing.value) index.value = (index.value + 1) % props.screens.length
    }, Math.max(10, props.interval) * 1000)
  }
}
watch(index, () => load())
watch(() => props.screens, () => {
  index.value = 0
  load(true)
  restartRotation()
})

onMounted(() => {
  load(true)
  refreshTimer = setInterval(() => load(true), REFRESH_MS)
  restartRotation()
})
onBeforeUnmount(() => {
  clearInterval(refreshTimer)
  clearInterval(rotateTimer)
})
</script>

<style scoped>
.loading {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: rgba(160, 190, 230, 0.8);
}
</style>
