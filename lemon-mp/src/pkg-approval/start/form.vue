<template>
  <view v-if="model">
    <l-card :title="model.name">
      <view v-if="!fields.length" class="l-muted sf__none">{{ t('approval.start.noForm') }}</view>
      <l-field v-for="f in fields" :key="f.field" :label="f.label" :required="!!f.required">
        <textarea
          v-if="f.type === FormFieldType.TEXTAREA"
          class="l-textarea"
          :value="form[f.field]"
          :placeholder="f.placeholder || t('common.pleaseInput')"
          placeholder-class="l-placeholder"
          :maxlength="TEXTAREA_MAX"
          @input="(e) => set(f, e.detail.value)"
        />
        <input
          v-else-if="f.type === FormFieldType.NUMBER"
          class="l-input"
          type="digit"
          :value="form[f.field]"
          :placeholder="f.placeholder || t('approval.start.inputNumber')"
          placeholder-class="l-placeholder"
          @input="(e) => set(f, e.detail.value)"
        />
        <view v-else-if="f.type === FormFieldType.DATE" class="sf__picker">
          <picker mode="date" :value="form[f.field] || today()" class="sf__picker-main" @change="(e) => set(f, e.detail.value)">
            <view class="l-input" :class="{ 'l-placeholder': !form[f.field] }">{{ form[f.field] || f.placeholder || t('approval.start.selectDate') }}</view>
          </picker>
          <text v-if="form[f.field]" class="sf__clear" @click="set(f, '')">{{ t('approval.start.clear') }}</text>
        </view>
        <view v-else-if="f.type === FormFieldType.SELECT" class="sf__picker">
          <picker mode="selector" :range="f.options || []" :value="Math.max(0, (f.options || []).indexOf(form[f.field]))" class="sf__picker-main" @change="(e) => set(f, (f.options || [])[e.detail.value])">
            <view class="l-input" :class="{ 'l-placeholder': !form[f.field] }">{{ form[f.field] || f.placeholder || t('common.pleaseSelect') }}</view>
          </picker>
          <text v-if="form[f.field]" class="sf__clear" @click="set(f, '')">{{ t('approval.start.clear') }}</text>
        </view>
        <input
          v-else
          class="l-input"
          :value="form[f.field]"
          :placeholder="f.placeholder || t('common.pleaseInput')"
          placeholder-class="l-placeholder"
          :maxlength="INPUT_MAX"
          @input="(e) => set(f, e.detail.value)"
        />
      </l-field>
    </l-card>
    <view class="l-footer-space" />
    <view class="l-footer">
      <button class="l-btn" :loading="busy" :disabled="busy" @click="submit">{{ t('common.submit') }}</button>
    </view>
  </view>
  <l-empty v-else-if="failed" />
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { FormFieldType } from '@web/constants/enums'
import { getStartModel, startInstance } from '@/api/workflow'
import { runAction } from '@/composables/useAction'
import { useTitle } from '@/composables/useTitle'
import { requestSubscribe } from '@/utils/subscribe'
import { openEntry } from '@/config/entries'
import { ensureLogin } from '@/utils/session'
import { today } from '@/utils/format'

/** 与后端 FormFieldType 的校验一致：单行文本 200 字、多行 2000 字，数字绝对值不超过 1 万亿 */
const INPUT_MAX = 200
const TEXTAREA_MAX = 2000
const MAX_NUMBER = 1e12
const NUMBER_RE = /^[+-]?(\d+(\.\d*)?|\.\d+)$/

const { t } = useI18n()
useTitle('approval.start.title')
const model = ref(null)
const failed = ref(false)
const busy = ref(false)
const form = reactive({})
const fields = computed(() => model.value?.form || [])

onLoad(async (q) => {
  if (!(await ensureLogin())) return
  try {
    model.value = await getStartModel(decodeURIComponent(q.key || ''))
    uni.setNavigationBarTitle({ title: t('approval.start.formTitle', { name: model.value.name }) })
  } catch {
    failed.value = true
  }
})

function set(f, value) {
  form[f.field] = value
}

const isBlank = (v) => v === undefined || v === null || String(v).trim() === ''

/** 校验并整理提交的值：未填写的不提交，数字转为数值 */
function collect() {
  const data = {}
  for (const f of fields.value) {
    const raw = form[f.field]
    if (isBlank(raw)) {
      if (f.required) {
        const select = f.type === FormFieldType.SELECT || f.type === FormFieldType.DATE
        throw new Error(t(select ? 'common.selectRequired' : 'common.required', { name: f.label }))
      }
      continue
    }
    const value = String(raw).trim()
    if (f.type === FormFieldType.NUMBER) {
      if (!NUMBER_RE.test(value)) throw new Error(t('approval.start.mustNumber', { name: f.label }))
      const n = Number(value)
      if (Math.abs(n) > MAX_NUMBER) throw new Error(t('approval.start.outRange', { name: f.label }))
      data[f.field] = n
    } else if (f.type === FormFieldType.INPUT || f.type === FormFieldType.TEXTAREA || !f.type) {
      const max = f.type === FormFieldType.TEXTAREA ? TEXTAREA_MAX : INPUT_MAX
      if (value.length > max) throw new Error(t('approval.start.tooLong', { name: f.label, n: max }))
      data[f.field] = value
    } else {
      data[f.field] = value
    }
  }
  return data
}

async function submit() {
  // 流程结束时通知发起人，在点击时申请订阅授权
  requestSubscribe()
  let formData
  try {
    formData = collect()
  } catch (e) {
    return uni.showToast({ title: e.message, icon: 'none' })
  }
  busy.value = true
  try {
    await runAction(() => startInstance({ modelKey: model.value.modelKey, formData }), {
      confirm: t('approval.start.confirm'),
      success: t('approval.start.submitted'),
      // 提交后进入审批页的「我发起的」
      then: () => new Promise((resolve) => setTimeout(() => resolve(openEntry({ tab: true, url: '/pages/approval/index' }, '/workflow/mine')), 800))
    })
  } finally {
    busy.value = false
  }
}
</script>

<style lang="scss">
.sf {
  &__none {
    padding: 16rpx 0;
    font-size: 26rpx;
  }
  &__picker {
    display: flex;
    align-items: center;
  }
  &__picker-main {
    flex: 1;
  }
  &__clear {
    padding-left: 24rpx;
    font-size: 24rpx;
    color: $l-primary;
  }
}
</style>
