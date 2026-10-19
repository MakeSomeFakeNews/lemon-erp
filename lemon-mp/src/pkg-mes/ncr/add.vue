<template>
  <view>
    <l-card>
      <l-field :label="t('mes.ncr.source')" required>
        <picker :range="sources" range-key="label" @change="(e) => (form.source = sources[e.detail.value].value)">
          <view class="l-input">{{ dictText(dict.qms_ncr_source, form.source) || t('common.pleaseSelect') }}</view>
        </picker>
      </l-field>
      <l-field :label="t('mes.common.woNo')">
        <view class="na__row">
          <input v-model="form.woNo" class="l-input" maxlength="32" :placeholder="t('mes.ncr.woNoPh')" placeholder-class="l-placeholder" @blur="loadWo" />
          <text class="na__link" @click="scanWo">{{ t('common.scan') }}</text>
        </view>
      </l-field>
      <l-field :label="t('mes.common.material')" required>
        <MaterialPicker v-model="form.material" />
      </l-field>
      <l-field :label="t('mes.common.batchNo')">
        <input v-model="form.batchNo" class="l-input" maxlength="64" :placeholder="t('mes.common.optional')" placeholder-class="l-placeholder" />
      </l-field>
      <l-field :label="t('mes.common.qty')" required>
        <view class="na__row">
          <input v-model="form.qty" class="l-input" type="digit" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
          <text v-if="form.material?.unit" class="l-muted">{{ form.material.unit }}</text>
        </view>
      </l-field>
      <l-field :label="t('mes.ncr.defectType')">
        <picker :range="dict.mes_defect_type" range-key="label" @change="(e) => (form.defectType = dict.mes_defect_type[e.detail.value].value)">
          <view class="l-input" :class="{ 'l-placeholder': !form.defectType }">{{ dictText(dict.mes_defect_type, form.defectType) || t('mes.common.optional') }}</view>
        </picker>
      </l-field>
      <l-field :label="t('mes.ncr.description')" required>
        <textarea v-model="form.description" class="l-textarea" maxlength="500" :placeholder="t('mes.ncr.descriptionPh')" placeholder-class="l-placeholder" />
      </l-field>
    </l-card>
    <view class="na__tip">{{ t('mes.ncr.reviewOnPc') }}</view>
    <view class="l-footer-space" />
    <view class="l-footer">
      <button class="l-btn" :loading="busy" :disabled="busy" @click="submit">{{ t('common.submit') }}</button>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from '@/locales'
import { NcrSource } from '@web/constants/enums'
import { addNcr, getWorkOrder, pageWorkOrder } from '@/api/mes'
import { scanCode } from '@/composables/useAction'
import { useTitle } from '@/composables/useTitle'
import { dictText, useDict } from '@/store/dict'
import MaterialPicker from '../components/MaterialPicker.vue'
import { findWorkOrder } from '../mes'

/** 登记不合格品（生产发现、客诉、其他）：检验不合格由检验单确认时自动生成，这里不能选；填写工单号时带出工单产品 */
const { t } = useI18n()
useTitle('mes.ncr.addTitle')
const dict = useDict('qms_ncr_source', 'mes_defect_type')
const busy = ref(false)
const form = reactive({ source: NcrSource.PRODUCTION, woNo: '', material: null, batchNo: '', qty: '', defectType: null, description: '' })
const sources = computed(() => dict.qms_ncr_source.filter((d) => d.value !== NcrSource.INSPECTION))

async function scanWo() {
  const code = await scanCode()
  if (!code) return
  form.woNo = code
  loadWo()
}

let lastWo = ''
async function loadWo() {
  const no = form.woNo.trim()
  if (!no || no === lastWo) return
  lastWo = no
  const wo = await findWorkOrder(pageWorkOrder, no).catch(() => null)
  if (!wo) return uni.showToast({ title: t('mes.common.woNotFound', { no }), icon: 'none' })
  if (form.material) return
  const d = await getWorkOrder(wo.id).catch(() => null)
  if (d) form.material = { id: d.materialId, code: d.materialCode, name: d.materialName, spec: d.spec, unit: d.unit }
}

async function submit() {
  if (!form.source) return toast(t('common.selectRequired', { name: t('mes.ncr.source') }))
  if (!form.material?.id) return toast(t('common.selectRequired', { name: t('mes.common.material') }))
  if (!(Number(form.qty) > 0)) return toast(t('common.required', { name: t('mes.common.qty') }))
  if (!form.description.trim()) return toast(t('common.required', { name: t('mes.ncr.description') }))
  busy.value = true
  let id
  try {
    id = await addNcr({
      source: form.source,
      materialId: form.material.id,
      batchNo: form.batchNo.trim() || null,
      qty: Number(form.qty),
      woNo: form.woNo.trim() || null,
      defectType: form.defectType || null,
      description: form.description.trim()
    })
  } catch {
    return
  } finally {
    busy.value = false
  }
  uni.showToast({ title: t('mes.ncr.created'), icon: 'success' })
  setTimeout(() => uni.redirectTo({ url: `/pkg-mes/ncr/detail?id=${id}` }), 800)
}

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}
</script>

<style lang="scss">
.na {
  &__row {
    display: flex;
    align-items: center;
    gap: 12rpx;
  }
  &__link {
    padding-left: 16rpx;
    font-size: 26rpx;
    color: $l-primary;
    white-space: nowrap;
  }
  &__tip {
    margin: 0 32rpx;
    font-size: 24rpx;
    color: $l-text-3;
  }
}
</style>
