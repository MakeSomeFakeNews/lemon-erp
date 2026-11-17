<template>
  <!-- 在途调拨的调拨入库（验收）：引用调拨单分批录入本次验收入库的数量，可登记差异；每次生成一张验收单 -->
  <a-modal
    v-model:open="visible"
    :title="doc ? $t('wms.transit.receiveTitle', { no: doc.docNo }) : $t('wms.transit.receive')"
    width="min(1200px, 100vw)"
    :confirm-loading="saving"
    :ok-text="$t('wms.transit.receiveOk')"
    :ok-button-props="{ disabled: !doc }"
    destroy-on-close
    @ok="submit"
  >
    <a-spin :spinning="loading">
      <template v-if="doc">
        <div class="sub-text tip">{{ $t('wms.transit.receiveTip', { from: doc.warehouseName, to: doc.toWarehouseName }) }}</div>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12" :lg="6">
            <a-form-item :label="$t('wms.transit.receiveDate')" required>
              <a-date-picker v-model:value="form.receiveDate" value-format="YYYY-MM-DD" :allow-clear="false" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="6">
            <a-form-item :label="$t('wms.transit.logisticsNo')">
              <a-input v-model:value="form.logisticsNo" :maxlength="64" :placeholder="$t('wms.common.optional')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="6">
            <a-form-item :label="$t('wms.transit.plateNo')">
              <a-input v-model:value="form.plateNo" :maxlength="32" :placeholder="$t('wms.common.optional')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="6">
            <a-form-item :label="$t('common.field.remark')">
              <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('wms.common.optional')" />
            </a-form-item>
          </a-col>
        </a-row>

        <div class="items-head">
          <span class="section-title">{{ $t('wms.transit.receiveLines', { qty: formatQty(totalReceive), diff: formatQty(totalDiff) }) }}</span>
          <a-space>
            <a-button size="small" @click="fillAll">{{ $t('wms.transit.allArrived') }}</a-button>
            <a-button size="small" @click="clearAll">{{ $t('wms.transit.clearAll') }}</a-button>
          </a-space>
        </div>
        <a-table row-key="id" size="small" :columns="columns" :data-source="rows" :pagination="false" :scroll="{ x: 1300 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              <div>{{ record.materialCode }} {{ record.materialName }}</div>
              <div class="sub-text">{{ [record.spec, record.batchNo && $t('wms.transit.batchN', { no: record.batchNo })].filter(Boolean).join(' · ') }}</div>
            </template>
            <template v-else-if="column.key === 'progress'">
              <div>{{ formatQty(record.remainingQty) }} / {{ formatQty(record.qty) }} {{ record.unit }}</div>
              <div v-if="over(record)" class="error-text">{{ $t('wms.transit.exceedsRemaining') }}</div>
            </template>
            <template v-else-if="column.key === 'receive'">
              <a-select
                v-if="record.serial"
                v-model:value="record.recvSerials"
                mode="multiple"
                :options="serialOptions(record, record.diffSerials)"
                :placeholder="$t('wms.transit.selectSerials')"
                :max-tag-count="3"
                class="full"
              />
              <a-input-number v-else v-model:value="record.recvQty" :min="0" :max="Number(record.remainingQty)" :precision="4" class="full" />
            </template>
            <template v-else-if="column.key === 'diffType'">
              <a-select
                v-model:value="record.diffType"
                :options="diffTypeOptions"
                allow-clear
                :placeholder="$t('wms.transit.noDiff')"
                class="full"
                @change="() => onDiffTypeChange(record)"
              />
            </template>
            <template v-else-if="column.key === 'diff'">
              <template v-if="record.diffType">
                <a-select
                  v-if="record.serial && !isOver(record)"
                  v-model:value="record.diffSerials"
                  mode="multiple"
                  :options="serialOptions(record, record.recvSerials)"
                  :placeholder="$t('wms.transit.selectSerials')"
                  :max-tag-count="3"
                  class="full"
                />
                <template v-else>
                  <a-input-number v-model:value="record.diffQty" :min="0" :precision="record.serial ? 0 : 4" class="full" />
                  <a-select
                    v-if="record.serial"
                    v-model:value="record.diffSerials"
                    mode="tags"
                    :placeholder="$t('wms.transit.overSerials')"
                    :token-separators="[',', ' ']"
                    class="full over-serials"
                  />
                </template>
              </template>
              <span v-else class="sub-text">-</span>
            </template>
            <template v-else-if="column.key === 'diffRemark'">
              <a-input v-if="record.diffType" v-model:value="record.diffRemark" :maxlength="255" :placeholder="$t('wms.common.optional')" />
              <span v-else class="sub-text">-</span>
            </template>
          </template>
        </a-table>
      </template>
    </a-spin>
  </a-modal>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { getDoc, getTransferTransit, receiveTransfer } from '@/api/wms/doc'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { round4 } from '@/utils/number'
import { formatQty } from './wms'

import { TransferDiffType } from '@/constants/enums'
/**
 * 调拨入库（验收）弹窗：open(docId) 加载调拨单和未到的序列号，默认全部未到量验收入库，可修改、登记差异；
 * 保存后触发 success（验收单 ID）
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('wms_transfer_diff_type')
// 「在途丢失」由处理未到量生成，验收时不可选
const diffTypeOptions = computed(() => toOptions(dict.wms_transfer_diff_type).filter((o) => o.value !== TransferDiffType.LOST))

const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const doc = ref()
const rows = ref([])
const form = reactive({ receiveDate: undefined, logisticsNo: '', plateNo: '', remark: '' })

const columns = computed(() => [
  { title: t('wms.common.material'), key: 'material', width: 240 },
  { title: t('wms.transit.remainingOfShipped'), key: 'progress', width: 150, align: 'right' },
  { title: t('wms.transit.receiveQty'), key: 'receive', width: 220 },
  { title: t('wms.transit.diffType'), key: 'diffType', width: 130 },
  { title: t('wms.transit.diffQty'), key: 'diff', width: 220 },
  { title: t('wms.transit.diffRemark'), key: 'diffRemark', width: 200 }
])

const isOver = (r) => r.diffType === TransferDiffType.OVER
const recvQty = (r) => (r.serial ? r.recvSerials.length : Number(r.recvQty) || 0)
const diffQty = (r) => {
  if (!r.diffType) return 0
  return r.serial && !isOver(r) ? r.diffSerials.length : Number(r.diffQty) || 0
}
/** 验收 + 差异（多发除外）超过未到量 */
const over = (r) => round4(recvQty(r) + (isOver(r) ? 0 : diffQty(r))) > Number(r.remainingQty)
const totalReceive = computed(() => round4(rows.value.reduce((s, r) => s + recvQty(r), 0)))
const totalDiff = computed(() => round4(rows.value.reduce((s, r) => s + diffQty(r), 0)))

/** 序列号选项：本行未到的序列号，去掉另一栏已选的 */
const serialOptions = (r, exclude) => r.available.filter((sn) => !exclude.includes(sn)).map((sn) => ({ value: sn, label: sn }))

async function open(docId) {
  visible.value = true
  loading.value = true
  doc.value = undefined
  rows.value = []
  Object.assign(form, { receiveDate: dayjs().format('YYYY-MM-DD'), logisticsNo: '', plateNo: '', remark: '' })
  try {
    const [d, transit] = await Promise.all([getDoc('transfer', docId), getTransferTransit(docId)])
    doc.value = d
    const serials = transit.availableSerials || {}
    rows.value = d.items
      .filter((i) => Number(i.remainingQty) > 0)
      .map((i) => {
        const serial = i.serialEnabled === 1
        const available = serials[i.id] || []
        return {
          ...i,
          serial,
          available,
          recvQty: Number(i.remainingQty),
          recvSerials: serial ? [...available] : [],
          diffType: undefined,
          diffQty: undefined,
          diffSerials: [],
          diffRemark: ''
        }
      })
  } catch (e) {
    visible.value = false
    throw e
  } finally {
    loading.value = false
  }
}

function fillAll() {
  rows.value.forEach((r) => Object.assign(r, { recvQty: Number(r.remainingQty), recvSerials: [...r.available], diffType: undefined, diffQty: undefined, diffSerials: [] }))
}

function clearAll() {
  rows.value.forEach((r) => Object.assign(r, { recvQty: 0, recvSerials: [] }))
}

/** 选择差异类型时：默认差异数量为未到量减去本次验收；清空时去掉差异 */
function onDiffTypeChange(r) {
  r.diffSerials = []
  if (!r.diffType) {
    r.diffQty = undefined
    return
  }
  if (!r.serial && !isOver(r) && r.diffQty == null) {
    r.diffQty = Math.max(0, round4(Number(r.remainingQty) - recvQty(r)))
  }
}

async function submit() {
  const bad = rows.value.find(over)
  if (bad) {
    message.warning(t('wms.transit.lineExceeds', { name: `${bad.materialCode} ${bad.materialName}` }))
    return
  }
  const missingType = rows.value.find((r) => !r.diffType && Number(r.diffQty) > 0)
  if (missingType) {
    message.warning(t('wms.transit.selectDiffType'))
    return
  }
  const lines = rows.value
    .filter((r) => recvQty(r) > 0 || diffQty(r) > 0)
    .map((r) => ({
      itemId: r.id,
      qty: recvQty(r),
      serialNos: r.serial ? r.recvSerials.join(',') || null : null,
      diffType: diffQty(r) > 0 ? r.diffType : null,
      diffQty: diffQty(r),
      diffSerialNos: r.serial && diffQty(r) > 0 ? r.diffSerials.join(',') || null : null,
      diffRemark: diffQty(r) > 0 ? r.diffRemark || null : null
    }))
  if (!lines.length) {
    message.warning(t('wms.transit.enterReceive'))
    return
  }
  saving.value = true
  try {
    const id = await receiveTransfer(doc.value.id, {
      receiveDate: form.receiveDate,
      logisticsNo: form.logisticsNo || null,
      plateNo: form.plateNo || null,
      remark: form.remark || null,
      lines
    })
    message.success(t('wms.transit.receiveSuccess'))
    visible.value = false
    emit('success', id)
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
.over-serials {
  margin-top: 4px;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.section-title {
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.error-text {
  color: var(--status-critical);
  font-size: 12px;
}
</style>
