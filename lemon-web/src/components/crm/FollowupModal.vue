<template>
  <a-modal
    :open="open"
    :title="title || '写跟进'"
    :confirm-loading="saving"
    destroy-on-close
    @ok="submit"
    @cancel="emit('update:open', false)"
  >
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="跟进方式" name="method">
            <a-select v-model:value="form.method" :options="toOptions(dict.crm_follow_method)" placeholder="请选择" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="跟进时间" name="followTime">
            <a-date-picker v-model:value="form.followTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="跟进内容" name="content">
            <a-textarea v-model:value="form.content" :rows="4" :maxlength="1000" show-count placeholder="沟通了什么、客户反馈、达成的结论" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="下次跟进时间" name="nextTime">
            <a-date-picker v-model:value="form.nextTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="下次跟进计划" name="nextPlan">
            <a-input v-model:value="form.nextPlan" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref, watch } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { addFollowup } from '@/api/crm'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { now } from './crm'

/**
 * 写跟进：跟进客户、线索或商机，保存后更新对象的最近 / 下次跟进时间
 * - targetType：CUSTOMER / LEAD / OPPORTUNITY；targetId：对象 ID
 */
const props = defineProps({
  open: Boolean,
  targetType: String,
  targetId: Number,
  title: String
})
const emit = defineEmits(['update:open', 'success'])
const dict = useDict('crm_follow_method')

const formRef = ref()
const form = ref({})
const saving = ref(false)

watch(
  () => props.open,
  (open) => {
    if (open) form.value = { method: 'PHONE', followTime: now(), content: '', nextTime: undefined, nextPlan: undefined }
  }
)

const rules = {
  method: [{ required: true, message: '请选择跟进方式' }],
  content: [{ required: true, whitespace: true, message: '请填写跟进内容' }],
  nextTime: [
    {
      validator: (_, v) =>
        !v || !form.value.followTime || dayjs(v).isAfter(dayjs(form.value.followTime))
          ? Promise.resolve()
          : Promise.reject(new Error('下次跟进时间必须晚于本次跟进时间'))
    }
  ]
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    await addFollowup({ targetType: props.targetType, targetId: props.targetId, ...form.value })
    message.success('跟进已记录')
    emit('update:open', false)
    emit('success')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
