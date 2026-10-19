<template>
  <view>
    <l-card>
      <l-field :label="t('pm.project')" required>
        <picker :range="projects" range-key="label" :value="projectIndex" :disabled="!projects.length" @change="onProject">
          <view class="l-input" :class="{ 'l-placeholder': projectIndex < 0 }">
            {{ projectIndex >= 0 ? projects[projectIndex].label : projects.length || !projectsLoaded ? t('common.pleaseSelect') : t('pm.noProject') }}
          </view>
        </picker>
        <view class="te__hint">{{ t('pm.projectTip') }}</view>
      </l-field>
      <l-field :label="t('pm.task')">
        <picker :range="taskOptions" range-key="label" :value="taskIndex" :disabled="!form.projectId" @change="(e) => (form.taskId = taskOptions[e.detail.value].value)">
          <view class="l-input" :class="{ 'l-placeholder': !form.taskId }">{{ taskIndex > 0 ? taskOptions[taskIndex].label : t('pm.optional') }}</view>
        </picker>
      </l-field>
      <l-field :label="t('pm.workDate')" required>
        <picker mode="date" :value="form.workDate" :end="todayStr" @change="(e) => (form.workDate = e.detail.value)">
          <view class="l-input">{{ form.workDate }}</view>
        </picker>
      </l-field>
      <l-field :label="t('pm.hours')" required>
        <view class="te__hours">
          <text class="te__step" @click="step(-0.5)">－</text>
          <input v-model="form.hours" class="l-input te__hours-input" type="digit" />
          <text class="te__step" @click="step(0.5)">＋</text>
          <text class="te__unit">{{ t('pm.hourUnit') }}</text>
        </view>
      </l-field>
      <l-field :label="t('pm.content')">
        <textarea v-model="form.content" class="l-textarea" maxlength="255" :placeholder="t('pm.contentPh')" placeholder-class="l-placeholder" />
      </l-field>
    </l-card>
    <view v-if="rejectReason" class="te__reject">{{ t('pm.rejectReasonN', { reason: rejectReason }) }}</view>
    <view class="l-footer-space" />
    <view class="l-footer">
      <button class="l-btn" :loading="busy" :disabled="busy" @click="save">{{ t('pm.saveSubmit') }}</button>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { t } from '@/locales'
import { addTimesheet, myProjects, myTasks, myTimesheet, taskTree, updateTimesheet } from '@/api/pm'
import { today } from '@/utils/format'
import { leafTasks } from '../pm'

const todayStr = today()
const id = ref(null)
const busy = ref(false)
const projects = ref([])
const projectsLoaded = ref(false)
const tasks = ref([])
const rejectReason = ref('')
const form = reactive({ projectId: null, taskId: null, workDate: todayStr, hours: '8', content: '' })

const projectIndex = computed(() => projects.value.findIndex((p) => p.value === form.projectId))
const taskOptions = computed(() => [{ value: null, label: t('pm.noTask') }, ...tasks.value])
const taskIndex = computed(() => Math.max(0, taskOptions.value.findIndex((o) => o.value === form.taskId)))

onLoad(async (q) => {
  id.value = q.id ? Number(q.id) : null
  uni.setNavigationBarTitle({ title: t(id.value ? 'pm.edit' : 'pm.log') })
  if (q.date) form.workDate = q.date > todayStr ? todayStr : q.date
  const list = await myProjects().catch(() => [])
  projects.value = (list || []).map((p) => ({ value: p.id, label: `${p.code} ${p.name}` }))
  projectsLoaded.value = true
  if (id.value) {
    const rows = (await myTimesheet(q.date, q.date).catch(() => [])) || []
    const r = rows.find((x) => x.id === id.value)
    if (r) {
      Object.assign(form, { projectId: r.projectId, taskId: r.taskId || null, workDate: r.workDate, hours: String(Number(r.hours)), content: r.content || '' })
      rejectReason.value = r.rejectReason || ''
      // 项目已不在可选列表中（如已完成）时仍显示原项目
      if (!projects.value.some((p) => p.value === r.projectId)) projects.value.unshift({ value: r.projectId, label: r.projectName })
    }
  } else if (projects.value.length === 1) {
    form.projectId = projects.value[0].value
  }
  if (form.projectId) loadTasks(form.projectId)
})

function onProject(e) {
  form.projectId = projects.value[e.detail.value].value
  form.taskId = null
  loadTasks(form.projectId)
}

/** 末级任务；没有任务查看权限时用分配给我的任务 */
async function loadTasks(projectId) {
  let list
  try {
    list = leafTasks(await taskTree(projectId))
  } catch {
    list = ((await myTasks().catch(() => [])) || []).filter((x) => x.projectId === projectId)
  }
  if (form.projectId !== projectId) return
  tasks.value = list.map((x) => ({ value: x.id, label: x.name }))
  if (form.taskId && !tasks.value.some((o) => o.value === form.taskId)) form.taskId = null
}

function step(n) {
  const v = Math.round(((Number(form.hours) || 0) + n) * 10) / 10
  form.hours = String(Math.min(24, Math.max(0.5, v)))
}

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}

async function save() {
  if (!form.projectId) return toast(t('common.selectRequired', { name: t('pm.project') }))
  const h = Number(form.hours)
  // 与后端校验一致：0.5 ~ 24，最多 1 位小数
  if (!form.hours || !Number.isFinite(h)) return toast(t('common.required', { name: t('pm.hours') }))
  if (h < 0.5 || h > 24) return toast(t('pm.hoursRange'))
  if (Math.round(h * 10) !== h * 10) return toast(t('pm.hoursDecimal'))
  if (form.workDate > todayStr) return toast(t('pm.noFuture'))
  const data = { projectId: form.projectId, taskId: form.taskId || null, workDate: form.workDate, hours: h, content: form.content.trim() || null }
  busy.value = true
  let res
  try {
    res = id.value ? await updateTimesheet(id.value, data) : await addTimesheet(data)
  } catch {
    return
  } finally {
    busy.value = false
  }
  if (res?.warning) {
    // 超过当天出勤时长等：已保存，提示后返回
    uni.showModal({ title: t('common.tip'), content: t('pm.savedWithWarning', { warning: res.warning }), showCancel: false, confirmText: t('common.ok'), complete: () => uni.navigateBack() })
  } else {
    uni.showToast({ title: t('common.saved'), icon: 'success' })
    setTimeout(() => uni.navigateBack(), 800)
  }
}
</script>

<style lang="scss">
.te {
  &__hint {
    margin-top: 8rpx;
    font-size: 22rpx;
    color: $l-text-3;
  }
  &__hours {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }
  &__step {
    width: 64rpx;
    height: 64rpx;
    line-height: 64rpx;
    text-align: center;
    border-radius: 12rpx;
    font-size: 32rpx;
    color: $l-primary;
    background: #e6f4ff;
  }
  &__hours-input {
    width: 160rpx;
    text-align: center;
  }
  &__unit {
    font-size: 26rpx;
    color: $l-text-3;
  }
  &__reject {
    margin: 0 24rpx;
    padding: 20rpx 24rpx;
    border-radius: $l-radius;
    font-size: 26rpx;
    color: #cf1322;
    background: #fff1f0;
  }
}
</style>
