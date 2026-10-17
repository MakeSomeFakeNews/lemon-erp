<template>
  <div class="screen-admin">
    <a-card :bordered="false" class="page-card">
      <template #title>数据大屏</template>
      <template #extra>
        <span class="sub-text">按 1920 × 1080 设计，自动缩放铺满屏幕；数据每 30 秒刷新，按 F11 或右下角按钮全屏</span>
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
                placeholder="全部工作中心"
                size="small"
                class="wc-select"
              />
              <a-button type="primary" size="small" @click="open([s.key])"><template #icon><ExpandOutlined /></template>打开</a-button>
            </a-space>
          </div>
        </div>
      </div>
      <div class="rotate-bar">
        <span>轮播：已选 {{ rotate.length }} 个大屏，每</span>
        <a-input-number v-model:value="interval" :min="10" :max="3600" size="small" class="interval" />
        <span>秒切换</span>
        <a-button size="small" :disabled="rotate.length < 2" @click="open(rotate)">轮播打开</a-button>
        <span class="sub-text">勾选大屏名称前的复选框选择要轮播的大屏</span>
      </div>
    </a-card>

    <a-card v-if="canShare" :bordered="false" class="page-card">
      <template #title>投屏链接</template>
      <template #extra>
        <a-button type="primary" @click="drawer.openCreate()"><template #icon><PlusOutlined /></template>新建链接</a-button>
      </template>
      <a-alert type="info" show-icon class="tip"
        message="车间、仓库的电视用浏览器打开投屏链接即可显示大屏，不需要登录、不会过期退出；链接只能查看所选的大屏。链接泄露时可停用或重新生成。" />
      <a-table row-key="id" size="middle" :columns="columns" :data-source="shares" :loading="loading" :pagination="false" :scroll="{ x: 1100 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'screens'">
            <a-tag v-for="k in record.screens.split(',')" :key="k" :bordered="false" color="blue">{{ screenLabel(k) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'workCenter'">{{ workCenterName(record.workCenterId) }}</template>
          <template v-else-if="column.key === 'interval'">{{ record.screens.includes(',') ? `${record.intervalSeconds} 秒` : '-' }}</template>
          <template v-else-if="column.key === 'status'">
            <a-tag v-if="expired(record)" color="default" :bordered="false">已过期</a-tag>
            <a-tag v-else :color="record.status === 1 ? 'green' : 'default'" :bordered="false">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'expireTime'">{{ record.expireTime || '长期有效' }}</template>
          <template v-else-if="column.key === 'link'">
            <a-space>
              <a @click="copy(record)"><CopyOutlined /> 复制链接</a>
              <a :href="linkOf(record)" target="_blank" rel="noopener">打开</a>
            </a-space>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="drawer.openEdit(record)">编辑</a>
              <a-popconfirm title="重新生成后原链接立即失效，确定吗？" @confirm="reset(record)"><a>重新生成</a></a-popconfirm>
              <a-popconfirm title="删除后链接失效，确定删除吗？" @confirm="remove(record)"><a class="danger">删除</a></a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新建投屏链接" edit-title="编辑投屏链接">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="名称" name="name">
          <a-input v-model:value="form.name" :maxlength="64" placeholder="如 一车间电视、成品仓大屏" />
        </a-form-item>
        <a-form-item label="大屏" name="screens" extra="选择多个时轮播">
          <a-checkbox-group v-model:value="form.screens" :options="SCREENS.map((s) => ({ value: s.key, label: s.label }))" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="轮播间隔（秒）" name="intervalSeconds">
              <a-input-number v-model:value="form.intervalSeconds" :min="10" :max="3600" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="工作中心" name="workCenterId" extra="生产车间大屏只显示该工作中心">
              <a-select v-model:value="form.workCenterId" :options="workCenters" allow-clear placeholder="全部" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="有效期至" name="expireTime">
              <a-date-picker v-model:value="form.expireTime" show-time value-format="YYYY-MM-DDTHH:mm:ss" placeholder="长期有效" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <a-radio-group v-model:value="form.status">
                <a-radio :value="1">启用</a-radio>
                <a-radio :value="0">停用</a-radio>
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

/** 数据大屏：打开 / 轮播大屏，管理投屏链接（电视免登录查看） */
const userStore = useUserStore()
const canShare = computed(() => userStore.hasPerm('bi:screen:share'))

const workCenters = ref([])
const workCenterId = ref()
const workCenterName = (id) => (id ? workCenters.value.find((w) => w.value === id)?.label || id : '全部')
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
  { title: '名称', dataIndex: 'name', width: 160 },
  { title: '大屏', key: 'screens', width: 260 },
  { title: '轮播间隔', key: 'interval', width: 90 },
  { title: '工作中心', key: 'workCenter', width: 120 },
  { title: '状态', key: 'status', width: 80 },
  { title: '有效期至', key: 'expireTime', width: 170 },
  { title: '最近访问', dataIndex: 'lastAccessTime', width: 170 },
  { title: '链接', key: 'link', width: 150 },
  { title: '操作', key: 'action', width: 190, fixed: 'right' }
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
    message.success('已复制链接')
  } catch {
    message.info(linkOf(r))
  }
}
async function reset(r) {
  await resetShare(r.id)
  message.success('已重新生成，原链接已失效')
  load()
}
async function remove(r) {
  await deleteShare(r.id)
  message.success('删除成功')
  load()
}

const rules = {
  name: [{ required: true, whitespace: true, message: '请填写名称' }],
  screens: [{ required: true, type: 'array', min: 1, message: '至少选择一个大屏' }],
  intervalSeconds: [{ required: true, message: '请填写轮播间隔' }]
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
