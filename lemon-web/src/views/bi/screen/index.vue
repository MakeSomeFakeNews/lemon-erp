<template>
  <div class="screen-admin">
    <a-card :bordered="false" class="page-card">
      <template #title>{{ $t('bi.screen.title') }}</template>
      <template #extra>
        <span class="sub-text">{{ $t('bi.screen.hint') }}</span>
      </template>
      <div class="cards">
        <div v-for="s in SCREENS" :key="s.key" class="screen-card">
          <div :class="['thumb', s.key.toLowerCase()]">
            <span class="thumb-title">{{ s.label }}</span>
            <i v-for="n in 6" :key="n" class="bar" :style="{ height: `${20 + ((n * 37 + s.key.length * 11) % 50)}%` }" />
          </div>
          <div class="card-body">
            <div class="card-title">
              <a-checkbox :checked="rotate.includes(s.key)" @change="toggleRotate(s.key)">{{ s.label }}</a-checkbox>
            </div>
            <div class="card-desc">{{ s.desc }}</div>
            <a-space class="card-actions" wrap>
              <a-select
                v-if="s.key === 'FACTORY'"
                v-model:value="workCenterId"
                :options="workCenters"
                allow-clear
                :placeholder="$t('bi.screen.allWorkCenters')"
                size="small"
                class="wc-select"
              />
              <a-button type="primary" size="small" @click="open([s.key])"><template #icon><ExpandOutlined /></template>{{ $t('bi.screen.open') }}</a-button>
            </a-space>
          </div>
        </div>
      </div>
      <div class="rotate-bar">
        <span>{{ $t('bi.screen.rotatePrefix', { n: rotate.length }) }}</span>
        <a-input-number v-model:value="interval" :min="10" :max="3600" size="small" class="interval" />
        <span>{{ $t('bi.screen.rotateSuffix') }}</span>
        <a-button size="small" :disabled="rotate.length < 2" @click="open(rotate)">{{ $t('bi.screen.openRotate') }}</a-button>
        <span class="sub-text">{{ $t('bi.screen.rotateHint') }}</span>
      </div>
    </a-card>

    <a-card v-if="canShare" :bordered="false" class="page-card">
      <template #title>{{ $t('bi.screen.shareLinks') }}</template>
      <template #extra>
        <a-button type="primary" @click="drawer.openCreate()"><template #icon><PlusOutlined /></template>{{ $t('bi.screen.newLink') }}</a-button>
      </template>
      <a-alert type="info" show-icon class="tip"
        :message="$t('bi.screen.shareTip')" />
      <a-table row-key="id" size="middle" :columns="columns" :data-source="shares" :loading="loading" :pagination="false" :scroll="{ x: 1100 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'screens'">
            <a-tag v-for="k in record.screens.split(',')" :key="k" :bordered="false" color="blue">{{ screenLabel(k) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'workCenter'">{{ workCenterName(record.workCenterId) }}</template>
          <template v-else-if="column.key === 'interval'">{{ record.screens.includes(',') ? $t('bi.screen.seconds', { n: record.intervalSeconds }) : '-' }}</template>
          <template v-else-if="column.key === 'status'">
            <a-tag v-if="expired(record)" color="default" :bordered="false">{{ $t('bi.screen.expired') }}</a-tag>
            <a-tag v-else :color="record.status === 1 ? 'green' : 'default'" :bordered="false">{{ record.status === 1 ? $t('common.status.enabled') : $t('common.status.disabled') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'expireTime'">{{ record.expireTime || $t('bi.screen.permanent') }}</template>
          <template v-else-if="column.key === 'link'">
            <a-space>
              <a @click="copy(record)"><CopyOutlined /> {{ $t('bi.screen.copyLink') }}</a>
              <a :href="linkOf(record)" target="_blank" rel="noopener">{{ $t('bi.screen.open') }}</a>
            </a-space>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="drawer.openEdit(record)">{{ $t('common.action.edit') }}</a>
              <a-popconfirm :title="$t('bi.screen.confirmReset')" @confirm="reset(record)"><a>{{ $t('bi.screen.regenerate') }}</a></a-popconfirm>
              <a-popconfirm :title="$t('bi.screen.confirmDelete')" @confirm="remove(record)"><a class="danger">{{ $t('common.action.delete') }}</a></a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('bi.screen.createTitle')" :edit-title="$t('bi.screen.editTitle')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('common.field.name')" name="name">
          <a-input v-model:value="form.name" :maxlength="64" :placeholder="$t('bi.screen.namePlaceholder')" />
        </a-form-item>
        <a-form-item :label="$t('bi.screen.screens')" name="screens" :extra="$t('bi.screen.screensExtra')">
          <a-checkbox-group v-model:value="form.screens" :options="SCREENS.map((s) => ({ value: s.key, label: s.label }))" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('bi.screen.intervalSeconds')" name="intervalSeconds">
              <a-input-number v-model:value="form.intervalSeconds" :min="10" :max="3600" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('bi.screen.workCenter')" name="workCenterId" :extra="$t('bi.screen.workCenterExtra')">
              <a-select v-model:value="form.workCenterId" :options="workCenters" allow-clear :placeholder="$t('common.status.all')" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('bi.screen.expireTime')" name="expireTime">
              <a-date-picker v-model:value="form.expireTime" show-time value-format="YYYY-MM-DDTHH:mm:ss" :placeholder="$t('bi.screen.permanent')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.status')" name="status">
              <a-radio-group v-model:value="form.status">
                <a-radio :value="1">{{ $t('common.status.enabled') }}</a-radio>
                <a-radio :value="0">{{ $t('common.status.disabled') }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { CopyOutlined, ExpandOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { createShare, deleteShare, listShares, resetShare, updateShare } from '@/api/bi/screen'
import { workCenterOptions } from '@/api/mes/base'
import { SCREENS, screenLabel } from '@/components/screen/screen'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useUserStore } from '@/store/user'
import { useI18n } from 'vue-i18n'

/** 数据大屏：打开 / 轮播大屏，管理投屏链接（电视免登录查看） */
const { t } = useI18n()
const userStore = useUserStore()
const canShare = computed(() => userStore.hasPerm('bi:screen:share'))

const workCenters = ref([])
const workCenterId = ref()
const workCenterName = (id) => (id ? workCenters.value.find((w) => w.value === id)?.label || id : t('common.status.all'))
const rotate = ref(SCREENS.map((s) => s.key))
const interval = ref(60)
function toggleRotate(key) {
  rotate.value = rotate.value.includes(key) ? rotate.value.filter((k) => k !== key) : SCREENS.map((s) => s.key).filter((k) => k === key || rotate.value.includes(k))
}

/** 新窗口打开大屏 */
function open(keys) {
  const params = new URLSearchParams()
  if (keys.length > 1) {
    params.set('rotate', keys.join(',').toLowerCase())
    params.set('interval', String(interval.value || 60))
  }
  if (keys.includes('FACTORY') && workCenterId.value) params.set('workCenterId', String(workCenterId.value))
  const query = params.toString()
  window.open(`/screen/${keys[0].toLowerCase()}${query ? `?${query}` : ''}`, '_blank')
}

// ---------------------------------------------------------------- 投屏链接
const shares = ref([])
const loading = ref(false)
const columns = [
  { title: t('common.field.name'), dataIndex: 'name', width: 160 },
  { title: t('bi.screen.screens'), key: 'screens', width: 260 },
  { title: t('bi.screen.interval'), key: 'interval', width: 110 },
  { title: t('bi.screen.workCenter'), key: 'workCenter', width: 130 },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('bi.screen.expireTime'), key: 'expireTime', width: 170 },
  { title: t('bi.screen.lastAccess'), dataIndex: 'lastAccessTime', width: 170 },
  { title: t('bi.screen.link'), key: 'link', width: 160 },
  { title: t('common.action.operation'), key: 'action', width: 210, fixed: 'right' }
]
async function load() {
  loading.value = true
  try {
    shares.value = await listShares()
  } finally {
    loading.value = false
  }
}
const expired = (r) => !!r.expireTime && new Date(r.expireTime.replace(' ', 'T')) < new Date()
const linkOf = (r) => `${location.origin}/screen-share/${r.token}`
async function copy(r) {
  try {
    await navigator.clipboard.writeText(linkOf(r))
    message.success(t('bi.screen.linkCopied'))
  } catch {
    message.info(linkOf(r))
  }
}
async function reset(r) {
  await resetShare(r.id)
  message.success(t('bi.screen.regenerated'))
  load()
}
async function remove(r) {
  await deleteShare(r.id)
  message.success(t('common.msg.deleteSuccess'))
  load()
}

const rules = {
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('common.field.name') }) }],
  screens: [{ required: true, type: 'array', min: 1, message: t('bi.screen.selectScreen') }],
  intervalSeconds: [{ required: true, message: t('common.rule.required', { name: t('bi.screen.interval') }) }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', screens: ['FACTORY'], intervalSeconds: 60, workCenterId: undefined, expireTime: undefined, status: 1 }),
  fetch: (r) => ({ ...r, screens: r.screens.split(','), expireTime: r.expireTime ? r.expireTime.replace(' ', 'T') : undefined }),
  create: createShare,
  update: updateShare,
  toPayload: ({ name, screens, intervalSeconds, workCenterId: wc, expireTime, status }) => ({
    name, screens, intervalSeconds, workCenterId: wc ?? null, expireTime: expireTime || null, status
  }),
  onSuccess: load
})
const { formRef, form } = drawer

onMounted(async () => {
  workCenters.value = (await workCenterOptions()).map((w) => ({ value: w.id, label: w.name }))
  if (canShare.value) load()
})
</script>

<style scoped>
.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}
.screen-card {
  border: 1px solid var(--app-border);
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.thumb {
  position: relative;
  height: 120px;
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 0 22px 16px;
  background: radial-gradient(ellipse at 50% 0%, #0b2a5a 0%, #050f24 70%);
}
.thumb-title {
  position: absolute;
  top: 14px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 4px;
  color: #cfe8ff;
}
.bar {
  flex: 1;
  background: linear-gradient(180deg, #00e4ff, rgba(0, 228, 255, 0.15));
}
.thumb.warehouse .bar { background: linear-gradient(180deg, #2fe6a5, rgba(47, 230, 165, 0.15)); }
.thumb.sales .bar { background: linear-gradient(180deg, #ffb03a, rgba(255, 176, 58, 0.15)); }
.thumb.quality .bar { background: linear-gradient(180deg, #a77bff, rgba(167, 123, 255, 0.15)); }
.card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 12px 14px;
}
.card-title {
  font-weight: 500;
}
.card-desc {
  flex: 1;
  margin: 6px 0 10px;
  font-size: 12px;
  color: var(--app-text-3);
  line-height: 1.6;
}
.card-actions {
  justify-content: flex-end;
}
.wc-select {
  width: 140px;
}
.rotate-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
}
.interval {
  width: 80px;
}
.tip {
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
.danger {
  color: var(--status-critical);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
