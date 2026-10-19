<template>
  <view>
    <!-- 选择工单 -->
    <block v-if="!wo">
      <l-scan-input :placeholder="t('wms.report.scanWo')" @scan="searchWo" />
      <view v-for="w in workOrders" :key="w.id" class="m-card" @click="selectWo(w.id)">
        <view class="m-row">
          <text class="m-title">{{ w.woNo }}</text>
          <l-tag :text="dictText(dict.mes_wo_status, w.status)" :color="dictColor(dict.mes_wo_status, w.status)" />
        </view>
        <view class="m-sub">{{ w.materialCode }} {{ w.materialName }}{{ w.spec ? ` · ${w.spec}` : '' }}</view>
        <view class="m-row m-sub">
          <text>{{ t('wms.report.planQty', { n: qty(w.qty), unit: w.unit || '' }) }}</text>
          <text>{{ w.planStart || '' }}{{ w.planEnd ? ` ~ ${w.planEnd}` : '' }}</text>
        </view>
      </view>
      <l-empty v-if="loaded && !workOrders.length" :text="t('wms.report.noWo')" />
    </block>

    <!-- 工序 -->
    <block v-else-if="!op">
      <view class="m-card">
        <view class="m-row">
          <text class="m-title">{{ wo.woNo }}</text>
          <text class="m-link" @click="backToList">{{ t('wms.report.changeWo') }}</text>
        </view>
        <view class="m-sub">{{ wo.materialCode }} {{ wo.materialName }}{{ wo.spec ? ` · ${wo.spec}` : '' }}</view>
        <view class="m-sub">{{ t('wms.report.planQty', { n: qty(wo.qty), unit: wo.unit || '' }) }}</view>
      </view>
      <view class="report__hint">{{ t('wms.report.selectOp') }}</view>
      <view v-for="o in wo.ops || []" :key="o.id" class="m-card" @click="openOp(o)">
        <view class="m-row">
          <text class="m-title">{{ o.seq }} {{ o.name }}</text>
          <l-tag v-if="o.firstArticle === 1" :text="t('wms.report.firstArticle')" color="orange" />
        </view>
        <view class="m-sub">{{ o.workCenterName || '' }}</view>
        <view class="m-row m-sub">
          <text :class="Number(o.goodQty) >= Number(wo.qty) ? 'qty-done' : ''">
            {{ t('wms.report.opProgress', { good: qty(o.goodQty), scrap: qty(o.scrapQty), total: qty(wo.qty) }) }}
          </text>
          <text>{{ t('wms.report.opHours', { n: qty(o.hours) }) }}</text>
        </view>
      </view>
      <l-empty v-if="!(wo.ops || []).length" :text="t('wms.report.noOps')" />
    </block>

    <!-- 报工 -->
    <block v-else>
      <view class="m-card">
        <view class="m-row">
          <text class="m-title">{{ wo.woNo }} · {{ op.seq }} {{ op.name }}</text>
          <text class="m-link" @click="op = null">{{ t('wms.report.changeOp') }}</text>
        </view>
        <view class="m-sub">{{ wo.materialCode }} {{ wo.materialName }}</view>
        <view class="m-sub">{{ t('wms.report.opProgress', { good: qty(op.goodQty), scrap: qty(op.scrapQty), total: qty(wo.qty) }) }}</view>
      </view>
      <l-card>
        <l-field :label="t('wms.report.reportDate')" required>
          <picker mode="date" :value="form.reportDate" @change="(e) => (form.reportDate = e.detail.value)">
            <view class="l-input">{{ form.reportDate }}</view>
          </picker>
        </l-field>
        <l-field :label="t('wms.report.goodQty')" required>
          <input v-model="form.goodQty" class="l-input" type="digit" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
        </l-field>
        <l-field :label="t('wms.report.scrapQty')">
          <input v-model="form.scrapQty" class="l-input" type="digit" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
        </l-field>
        <l-field :label="t('wms.report.hours')" required>
          <input v-model="form.hours" class="l-input" type="digit" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
        </l-field>
        <l-field :label="t('wms.report.equipment')">
          <picker :range="equipmentRange" range-key="label" :value="equipmentIndex" @change="(e) => (form.equipmentId = equipmentRange[e.detail.value].id)">
            <view class="l-input" :class="{ 'l-placeholder': !form.equipmentId }">{{ equipmentRange[equipmentIndex].label }}</view>
          </picker>
        </l-field>
        <l-field v-if="molds.length" :label="t('wms.report.mold')">
          <picker :range="moldRange" range-key="label" :value="moldIndex" @change="(e) => (form.moldId = moldRange[e.detail.value].id)">
            <view class="l-input" :class="{ 'l-placeholder': !form.moldId }">{{ moldRange[moldIndex].label }}</view>
          </picker>
          <view v-if="shotsHint" class="report__extra">{{ shotsHint }}</view>
        </l-field>
        <l-field :label="t('common.remark')">
          <input v-model="form.remark" class="l-input" maxlength="255" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
        </l-field>
      </l-card>
      <view class="report__extra report__self">{{ t('wms.report.operatorSelf') }}</view>
      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn" :loading="saving" :disabled="saving" @click="submit">{{ t('wms.report.submit') }}</button>
      </view>
    </block>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { WorkOrderStatus } from '@web/constants/enums'
import { equipmentOptions, getWorkOrder, moldsOfWo, pageWorkOrder, reportWorkOrder } from '@/api/mes'
import { useTitle } from '@/composables/useTitle'
import { runAction } from '@/composables/useAction'
import { dictColor, dictText, useDict } from '@/store/dict'
import { qty, today } from '@/utils/format'

/**
 * 生产报工：扫描或输入工单号（或在已下达、生产中的工单中选择）→ 选择工序 → 填写合格数、报废数、工时（默认按剩余数量和标准工时），
 * 可选设备、模具。首件检验、超报等由后端校验并提示
 */
const { t } = useI18n()
useTitle('wms.entry.report')
const dict = useDict('mes_wo_status')
const REPORTABLE = [WorkOrderStatus.RELEASED, WorkOrderStatus.STARTED]
const loaded = ref(false)
const workOrders = ref([])
const wo = ref(null)
const op = ref(null)
const equipments = ref([])
const molds = ref([])
const saving = ref(false)
const form = reactive({ reportDate: today(), goodQty: '', scrapQty: '', hours: '', equipmentId: null, moldId: null, remark: '' })

/** 工序所在工作中心的设备排在前面 */
const equipmentRange = computed(() => [
  { id: null, label: t('wms.report.notSpecified') },
  ...[...equipments.value]
    .sort((a, b) => Number(b.workCenterId === op.value?.workCenterId) - Number(a.workCenterId === op.value?.workCenterId))
    .map((e) => ({ id: e.id, label: `${e.code} ${e.name}` }))
])
const equipmentIndex = computed(() => Math.max(0, equipmentRange.value.findIndex((e) => e.id === (form.equipmentId || null))))
const moldRange = computed(() => [
  { id: null, label: t('wms.report.notUsed') },
  ...molds.value.map((m) => ({ id: m.id, label: t('wms.report.moldOption', { code: m.code, name: m.name, n: m.cavities }) }))
])
const moldIndex = computed(() => Math.max(0, moldRange.value.findIndex((m) => m.id === (form.moldId || null))))
const shotsHint = computed(() => {
  const m = molds.value.find((x) => x.id === form.moldId)
  if (!m || !m.cavities) return ''
  const shots = Math.ceil((Number(form.goodQty || 0) + Number(form.scrapQty || 0)) / m.cavities)
  const used = Number(m.usedCount || 0) + shots
  return m.designLife ? t('wms.report.shotsHintLife', { shots, used, life: m.designLife }) : t('wms.report.shotsHint', { shots, used })
})

onLoad(() => searchWo())

/** 已下达、生产中的工单；扫描工单号时只有一个结果直接进入 */
async function searchWo(woNo) {
  try {
    const lists = await Promise.all(REPORTABLE.map((status) => pageWorkOrder({ woNo: woNo || undefined, status, current: 1, size: 50 })))
    workOrders.value = lists.flatMap((r) => r.records || []).sort((a, b) => String(a.planStart || '').localeCompare(String(b.planStart || '')))
    if (woNo && workOrders.value.length === 1) selectWo(workOrders.value[0].id)
    else if (woNo && !workOrders.value.length) uni.showToast({ title: t('wms.report.woNotFound', { no: woNo }), icon: 'none' })
  } finally {
    loaded.value = true
  }
}

async function selectWo(id) {
  const [detail, eqs, ms] = await Promise.all([getWorkOrder(id), equipmentOptions().catch(() => []), moldsOfWo(id).catch(() => [])])
  wo.value = detail
  equipments.value = eqs || []
  molds.value = ms || []
  op.value = null
}

function backToList() {
  wo.value = null
  searchWo()
}

function openOp(o) {
  op.value = o
  const remaining = Math.max(Number(wo.value.qty) - Number(o.goodQty || 0) - Number(o.scrapQty || 0), 0)
  const hours = Math.round(Number(o.stdHours || 0) * remaining * 100) / 100
  const sameCenter = equipments.value.filter((e) => e.workCenterId === o.workCenterId)
  Object.assign(form, {
    reportDate: today(), goodQty: String(remaining), scrapQty: '0', hours: String(hours), remark: '',
    equipmentId: molds.value[0]?.equipmentId ?? (sameCenter.length === 1 ? sameCenter[0].id : null),
    moldId: molds.value.length === 1 ? molds.value[0].id : null
  })
}

const num = (v) => (v === '' || v === null || v === undefined ? null : Number(v))

async function submit() {
  const goodQty = num(form.goodQty)
  const scrapQty = num(form.scrapQty) ?? 0
  const hours = num(form.hours)
  if (goodQty === null || !Number.isFinite(goodQty) || goodQty < 0) return toast(t('wms.report.goodQtyRequired'))
  if (!Number.isFinite(scrapQty) || scrapQty < 0) return toast(t('wms.report.scrapQtyInvalid'))
  if (hours === null || !Number.isFinite(hours) || hours < 0) return toast(t('wms.report.hoursRequired'))
  saving.value = true
  try {
    await runAction(
      () =>
        reportWorkOrder(wo.value.id, {
          opId: op.value.id, reportDate: form.reportDate, goodQty, scrapQty, hours, remark: form.remark || null,
          operatorId: null, equipmentId: form.equipmentId || null, moldId: form.moldId || null
        }),
      { success: t('wms.report.done'), then: () => selectWo(wo.value.id) }
    )
  } finally {
    saving.value = false
  }
}

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}
</script>

<style lang="scss">
@import '../mobile.scss';

.report {
  &__hint {
    margin: 24rpx 24rpx 0;
    font-size: 26rpx;
    color: $l-text-3;
  }
  &__extra {
    margin-top: 8rpx;
    font-size: 24rpx;
    color: $l-text-3;
  }
  &__self {
    margin: 0 32rpx;
  }
}
</style>
