<template>
  <a-modal
    :open="open"
    :title="material ? $t('wms.tracking.title', { name: `${material.code} ${material.name}` }) : ''"
    :confirm-loading="saving"
    :ok-button-props="{ disabled: !view || blocked || !changed }"
    width="min(620px, 100vw)"
    destroy-on-close
    @ok="submit"
    @cancel="open = false"
  >
    <a-spin :spinning="loading">
      <template v-if="view">
        <a-alert v-if="blocked" type="warning" show-icon class="block" :message="$t('wms.tracking.blocked')">
          <template #description>
            <div v-for="d in view.openDocs" :key="d">{{ d }}</div>
          </template>
        </a-alert>
        <a-descriptions :column="1" size="small" class="block">
          <a-descriptions-item :label="$t('wms.tracking.stockQty')">{{ formatQty(view.stockQty) }} {{ material.unit }}</a-descriptions-item>
          <a-descriptions-item v-if="view.batchEnabled === 1" :label="$t('wms.tracking.batchCount')">{{ view.batchCount }}</a-descriptions-item>
          <a-descriptions-item v-else :label="$t('wms.tracking.noBatchQty')">{{ formatQty(view.noBatchQty) }} {{ material.unit }}</a-descriptions-item>
        </a-descriptions>
        <a-form layout="vertical">
          <a-form-item>
            <a-checkbox :checked="form.batchEnabled === 1" @change="(e) => (form.batchEnabled = e.target.checked ? 1 : 0)">
              {{ $t('wms.material.batchManaged') }}
            </a-checkbox>
          </a-form-item>
          <!-- 启用：已有库存归入填写的批次 -->
          <template v-if="enabling">
            <a-alert type="info" show-icon class="block" :message="$t('wms.tracking.enableTip')" />
            <a-row :gutter="16">
              <a-col :xs="24" :sm="8">
                <a-form-item :label="$t('wms.tracking.batchNo')" :required="hasNoBatchStock">
                  <a-input v-model:value="form.batchNo" :maxlength="64" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="8">
                <a-form-item :label="$t('wms.common.productionDate')">
                  <a-date-picker v-model:value="form.productionDate" value-format="YYYY-MM-DD" class="full" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="8">
                <a-form-item :label="$t('wms.common.expiryDate')">
                  <a-date-picker v-model:value="form.expiryDate" value-format="YYYY-MM-DD" class="full" />
                </a-form-item>
              </a-col>
            </a-row>
          </template>
          <a-alert v-if="disabling" type="info" show-icon class="block" :message="$t('wms.tracking.disableTip')" />
          <a-form-item :extra="serialLocked ? $t('wms.tracking.serialLocked') : $t('wms.tracking.serialTip')">
            <a-checkbox :checked="form.serialEnabled === 1" :disabled="serialLocked" @change="(e) => (form.serialEnabled = e.target.checked ? 1 : 0)">
              {{ $t('wms.material.serialManaged') }}
            </a-checkbox>
          </a-form-item>
        </a-form>
        <div class="sub-text">{{ $t('wms.tracking.draftTip') }}</div>
      </template>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { changeMaterialTracking, materialTracking } from '@/api/wms/material'
import { formatQty } from '@/components/wms/wms'

/**
 * 批次 / 序列号管理切换（物料发生过库存业务后）：
 * 启用批次时已有库存归入填写的批次（如「期初」，可填生产日期、有效期）；停用时各批次按库位合并为无批次；
 * 序列号管理只能在库存为 0 时切换；有进行中的单据（在途调拨、盘点、待检冻结、未出库的发货通知等）时不能切换
 */
const emit = defineEmits(['saved'])
const { t } = useI18n()

const open = ref(false)
const loading = ref(false)
const saving = ref(false)
const material = ref()
const view = ref()
const form = ref({})

const blocked = computed(() => !!view.value?.openDocs?.length)
const enabling = computed(() => view.value?.batchEnabled !== 1 && form.value.batchEnabled === 1)
const disabling = computed(() => view.value?.batchEnabled === 1 && form.value.batchEnabled !== 1)
const hasNoBatchStock = computed(() => Number(view.value?.noBatchQty || 0) !== 0)
const serialLocked = computed(() => Number(view.value?.stockQty || 0) !== 0)
const changed = computed(() => !!view.value && (form.value.batchEnabled !== view.value.batchEnabled || form.value.serialEnabled !== view.value.serialEnabled))

async function show(record) {
  material.value = record
  view.value = undefined
  open.value = true
  loading.value = true
  try {
    view.value = await materialTracking(record.id)
    form.value = {
      batchEnabled: view.value.batchEnabled,
      serialEnabled: view.value.serialEnabled,
      batchNo: t('wms.tracking.defaultBatch'),
      productionDate: null,
      expiryDate: null
    }
  } catch (e) {
    open.value = false
    throw e
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (enabling.value && hasNoBatchStock.value && !form.value.batchNo?.trim()) {
    message.warning(t('wms.tracking.batchNoRequired'))
    return
  }
  saving.value = true
  try {
    await changeMaterialTracking(material.value.id, {
      ...form.value,
      batchNo: enabling.value ? form.value.batchNo?.trim() || null : null
    })
    message.success(t('common.msg.saveSuccess'))
    open.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
