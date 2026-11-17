<template>
  <!-- 在途调拨的差异处理（退回调出仓 / 确认损耗 / 让步接收），或处理未到量（退回 / 确认损耗） -->
  <a-modal
    v-model:open="visible"
    :title="title"
    width="min(560px, 100vw)"
    :confirm-loading="saving"
    destroy-on-close
    @ok="submit"
  >
    <template v-if="ctx">
      <a-descriptions v-if="ctx.diff" :column="1" size="small" class="block">
        <a-descriptions-item :label="$t('wms.common.material')">
          {{ ctx.diff.materialCode }} {{ ctx.diff.materialName }}
          <span v-if="ctx.diff.batchNo" class="sub-text">{{ $t('wms.transit.batchN', { no: ctx.diff.batchNo }) }}</span>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('wms.transit.diffType')">
          <DictTag type="wms_transfer_diff_type" :value="ctx.diff.diffType" />
          {{ formatQty(ctx.diff.qty) }} {{ ctx.diff.unit }}
          <span v-if="ctx.diff.remark" class="sub-text">{{ ctx.diff.remark }}</span>
        </a-descriptions-item>
      </a-descriptions>
      <div v-else class="sub-text block">{{ $t('wms.transit.closeTip', { qty: formatQty(ctx.remaining) }) }}</div>

      <a-form layout="vertical">
        <a-form-item :label="$t('wms.transit.resolution')" required>
          <a-radio-group v-model:value="form.action">
            <a-radio v-for="a in actions" :key="a" :value="a">{{ actionLabel(a) }}</a-radio>
          </a-radio-group>
          <div class="sub-text hint">{{ actionHint }}</div>
        </a-form-item>
        <a-form-item v-if="ctx.diff" :label="$t('wms.transit.resolveQty')">
          <a-select
            v-if="diffSerials.length"
            v-model:value="form.serials"
            mode="multiple"
            :options="diffSerials.map((sn) => ({ value: sn, label: sn }))"
            class="full"
          />
          <a-input-number v-else v-model:value="form.qty" :min="0" :max="Number(ctx.diff.qty)" :precision="4" class="full" />
          <div class="sub-text hint">{{ $t('wms.transit.partialTip') }}</div>
        </a-form-item>
        <a-form-item :label="$t('wms.transit.resolveDate')" required>
          <a-date-picker v-model:value="form.bizDate" value-format="YYYY-MM-DD" :allow-clear="false" class="full" />
        </a-form-item>
        <a-form-item :label="$t('wms.transit.resolveRemark')">
          <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('wms.common.optional')" />
        </a-form-item>
      </a-form>
    </template>
  </a-modal>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import { closeTransfer, resolveTransferDiff } from '@/api/wms/doc'
import { formatQty } from './wms'

import { TransferDiffType, TransferResolution } from '@/constants/enums'
/**
 * openDiff(diff) 处理一条差异；openClose(doc, remaining) 处理调拨单剩余的未到量。完成后触发 done（生成的单据 ID）
 */
const emit = defineEmits(['done'])
const { t } = useI18n()

const visible = ref(false)
const saving = ref(false)
/** { diff } 或 { doc, remaining } */
const ctx = ref()
const form = reactive({ action: undefined, qty: undefined, serials: [], bizDate: undefined, remark: '' })

const over = computed(() => ctx.value?.diff?.diffType === TransferDiffType.OVER)
const diffSerials = computed(() => (ctx.value?.diff?.serialNos ? ctx.value.diff.serialNos.split(',').filter(Boolean) : []))
const title = computed(() => (ctx.value?.diff ? t('wms.transit.resolveTitle') : t('wms.transit.closeTitle', { no: ctx.value?.doc?.docNo })))
/** 处理未到量只能退回或确认损耗；多发只能退回（实物退回）或让步接收（生成调拨单草稿） */
const actions = computed(() => {
  if (!ctx.value?.diff) return [TransferResolution.RETURN, TransferResolution.LOSS]
  if (over.value) return [TransferResolution.RETURN, TransferResolution.ACCEPT]
  return [TransferResolution.RETURN, TransferResolution.LOSS, TransferResolution.ACCEPT]
})
const actionLabel = (a) => t(`wms.transit.${over.value ? 'overActions' : 'actions'}.${a}`)
const actionHint = computed(() => (form.action ? t(`wms.transit.${over.value ? 'overHints' : 'hints'}.${form.action}`) : ''))

function reset(next) {
  ctx.value = next
  Object.assign(form, { action: undefined, qty: next.diff ? Number(next.diff.qty) : undefined, serials: [], bizDate: dayjs().format('YYYY-MM-DD'), remark: '' })
  form.serials = [...diffSerials.value]
  visible.value = true
}

const openDiff = (diff) => reset({ diff })
const openClose = (doc, remaining) => reset({ doc, remaining })

async function submit() {
  if (!form.action) {
    message.warning(t('wms.transit.selectResolution'))
    return
  }
  const data = { action: form.action, bizDate: form.bizDate, remark: form.remark || null }
  if (ctx.value.diff) {
    const serial = diffSerials.value.length > 0
    const qty = serial ? form.serials.length : Number(form.qty) || 0
    if (qty <= 0) {
      message.warning(t('wms.transit.enterResolveQty'))
      return
    }
    data.qty = qty
    data.serialNos = serial ? form.serials.join(',') : null
  }
  saving.value = true
  try {
    const refId = ctx.value.diff ? await resolveTransferDiff(ctx.value.diff.id, data) : await closeTransfer(ctx.value.doc.id, data)
    message.success(t('wms.transit.resolveSuccess'))
    visible.value = false
    emit('done', refId)
  } finally {
    saving.value = false
  }
}

defineExpose({ openDiff, openClose })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
.hint {
  margin-top: 4px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  margin-left: 6px;
}
.hint.sub-text,
.block.sub-text {
  margin-left: 0;
}
</style>
