<template>
  <view>
    <l-card>
      <view v-if="saved" class="ie__no">
        <text>{{ saved.inspNo }}</text>
        <l-tag v-if="readonly && saved.status === InspectionStatus.CONFIRMED" :text="dictText(dict.mes_qc_result, saved.result)" :color="dictColor(dict.mes_qc_result, saved.result)" />
        <l-tag v-else :text="t('mes.inspection.draft')" color="orange" />
      </view>
      <l-field :label="t('mes.inspection.type')" required>
        <picker v-if="!readonly" :range="dict.mes_qc_type" range-key="label" @change="changeType">
          <view class="l-input">{{ dictText(dict.mes_qc_type, form.type) || t('common.pleaseSelect') }}</view>
        </picker>
        <view v-else class="l-input">{{ dictText(dict.mes_qc_type, form.type) }}</view>
      </l-field>
      <l-field :label="form.type === QcType.IQC ? t('mes.inspection.receiptNo') : t('mes.common.woNo')" :required="form.type === QcType.FAI">
        <view class="ie__row">
          <input
            v-model="form.sourceNo"
            class="l-input"
            maxlength="32"
            :disabled="readonly"
            :placeholder="sourcePh"
            placeholder-class="l-placeholder"
            confirm-type="search"
            @confirm="loadWo"
            @blur="loadWo"
          />
          <text v-if="!readonly" class="ie__link" @click="scanSource">{{ t('common.scan') }}</text>
        </view>
      </l-field>
      <l-field v-if="form.type === QcType.FAI" :label="t('mes.inspection.op')" required>
        <picker v-if="!readonly && ops.length" :range="ops" range-key="label" @change="(e) => (form.woOpId = ops[e.detail.value].value)">
          <view class="l-input" :class="{ 'l-placeholder': !form.woOpId }">{{ opLabel || t('mes.inspection.selectOp') }}</view>
        </picker>
        <view v-else class="l-input" :class="{ 'l-placeholder': !opLabel }">{{ opLabel || saved?.opName || t('mes.inspection.woFirst') }}</view>
      </l-field>
      <l-field :label="t('mes.common.material')" required>
        <MaterialPicker v-model="form.material" :disabled="readonly" />
      </l-field>
      <l-field :label="t('mes.common.batchNo')">
        <input v-model="form.batchNo" class="l-input" maxlength="64" :disabled="readonly" :placeholder="t('mes.common.optional')" placeholder-class="l-placeholder" />
      </l-field>
      <l-field :label="t('mes.inspection.inspectQty')" required>
        <input v-model="form.inspectQty" class="l-input" type="digit" :disabled="readonly" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
      </l-field>
      <l-field :label="t('mes.inspection.inspectDate')" required>
        <picker v-if="!readonly" mode="date" :value="form.inspectDate" @change="(e) => (form.inspectDate = e.detail.value)">
          <view class="l-input">{{ form.inspectDate }}</view>
        </picker>
        <view v-else class="l-input">{{ form.inspectDate }}</view>
      </l-field>
    </l-card>

    <!-- 按检验方案逐项录入 -->
    <template v-if="usePlan">
      <view class="ie__plan">
        <text class="ie__plan-name">{{ t('mes.inspection.planHead', { name: planName }) }}</text>
        <text v-if="match">{{ match.sampleCode ? t('mes.inspection.samplingCode', { code: match.sampleCode }) : '' }}{{ t('mes.inspection.sampling', { n: qty(match.sampleQty), ac: match.acceptQty ?? 0, re: match.rejectQty ?? 1 }) }}</text>
      </view>
      <view v-for="item in form.items" :key="item.planItemId" class="ie__item">
        <view class="ie__item-head">
          <text class="ie__item-name">{{ item.name }}</text>
          <l-tag v-if="item.critical" :text="t('mes.inspection.critical')" color="red" />
          <l-tag :text="defectsOf(item) > 0 ? t('mes.inspection.fail') : t('mes.inspection.pass')" :color="defectsOf(item) > 0 ? 'red' : 'green'" />
        </view>
        <view class="ie__item-spec">{{ t('mes.inspection.spec') }} {{ specText(item) }}</view>
        <template v-if="item.method === ItemMethod.QUANTITATIVE">
          <textarea
            v-model="item.measuredText"
            class="ie__measured"
            auto-height
            :disabled="readonly"
            :placeholder="t('mes.inspection.measuredPh', { n: qty(match?.sampleQty ?? '') })"
            placeholder-class="l-placeholder"
          />
          <view class="ie__item-foot">
            <text>{{ t('mes.inspection.measuredCount', { n: parseMeasured(item.measuredText).length }) }}</text>
            <text :class="{ 'ie__danger': defectsOf(item) > 0 }">{{ t('mes.inspection.outOfSpec', { n: defectsOf(item) }) }}</text>
          </view>
        </template>
        <view v-else class="ie__count">
          <text>{{ t('mes.inspection.defectCount') }}</text>
          <view class="ie__stepper">
            <text v-if="!readonly" class="ie__step" @click="step(item, -1)">-</text>
            <input v-model="item.defectCount" class="ie__step-input" type="number" :disabled="readonly" />
            <text v-if="!readonly" class="ie__step" @click="step(item, 1)">+</text>
          </view>
        </view>
      </view>
      <view class="ie__judge">
        <text>{{ t('mes.inspection.defectTotal', { n: planDefects }) }}</text>
        <text>{{ t('mes.inspection.judge') }}</text>
        <l-tag :text="planResult === QcResult.FAIL ? t('mes.inspection.fail') : t('mes.inspection.pass')" :color="planResult === QcResult.FAIL ? 'red' : 'green'" />
      </view>
      <view v-if="planResult === QcResult.FAIL && !readonly" class="ie__tip">{{ form.type === QcType.IQC ? t('mes.inspection.failTipIqc') : t('mes.inspection.failTip') }}</view>
    </template>

    <!-- 没有方案：手工判定 -->
    <l-card v-else>
      <view v-if="!readonly" class="ie__tip ie__tip--card">{{ form.material && Number(form.inspectQty) > 0 ? t('mes.inspection.noPlan') : t('mes.inspection.matchTip') }}</view>
      <l-field :label="t('mes.inspection.sampleQty')" required>
        <input v-model="form.sampleQty" class="l-input" type="digit" :disabled="readonly" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
      </l-field>
      <l-field :label="t('mes.inspection.defectQty')">
        <input v-model="form.defectQty" class="l-input" type="digit" :disabled="readonly" placeholder-class="l-placeholder" />
      </l-field>
      <l-field :label="t('mes.inspection.result')" required>
        <picker v-if="!readonly" :range="dict.mes_qc_result" range-key="label" @change="(e) => (form.result = dict.mes_qc_result[e.detail.value].value)">
          <view class="l-input">{{ dictText(dict.mes_qc_result, form.result) || t('common.pleaseSelect') }}</view>
        </picker>
        <view v-else class="l-input">{{ dictText(dict.mes_qc_result, form.result) }}</view>
      </l-field>
    </l-card>

    <l-card>
      <l-field :label="t('mes.inspection.defectType')">
        <picker v-if="!readonly" :range="defectTypes" range-key="label" @change="(e) => (form.defectType = defectTypes[e.detail.value].value)">
          <view class="l-input" :class="{ 'l-placeholder': !form.defectType }">{{ dictText(dict.mes_defect_type, form.defectType) || t('mes.common.optional') }}</view>
        </picker>
        <view v-else class="l-input">{{ dictText(dict.mes_defect_type, form.defectType) || '-' }}</view>
      </l-field>
      <l-field :label="t('mes.inspection.defectDesc')">
        <textarea v-model="form.defectDesc" class="l-textarea" maxlength="255" :disabled="readonly" :placeholder="readonly ? '' : t('mes.common.optional')" placeholder-class="l-placeholder" />
      </l-field>
    </l-card>

    <template v-if="!readonly">
      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn l-btn--ghost" :disabled="busy" @click="save(false)">{{ t('mes.common.saveDraft') }}</button>
        <button v-if="canConfirm" class="l-btn" :disabled="busy" @click="save(true)">{{ t('mes.inspection.confirm') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { InspectionStatus, ItemMethod, QcResult, QcType } from '@web/constants/enums'
import { addInspection, confirmInspection, getInspection, getWorkOrder, matchPlan, pageInspection, pageWorkOrder, updateInspection } from '@/api/mes'
import { runAction, scanCode } from '@/composables/useAction'
import { dictColor, dictText, useDict } from '@/store/dict'
import { hasPerm } from '@/store/user'
import { qty, today } from '@/utils/format'
import MaterialPicker from '../components/MaterialPicker.vue'
import { findWorkOrder, outOfSpec, parseMeasured, specText } from '../mes'

/**
 * 检验录入：选择类型、来源单号（来料为采购入库单号，过程 / 成品 / 首件为工单号，首件还要选工序）、物料、送检数量后匹配检验方案；
 * 有方案时按方案逐项录入（定性填不合格数，定量填实测值），页面实时提示判定，最终以后端按方案抽样计算的结果为准；
 * 没有方案时手工填写抽检数、不良数和结论。已确认或没有修改权限时只读
 */
const { t } = useI18n()
const dict = useDict('mes_qc_type', 'mes_qc_result', 'mes_defect_type')
const id = ref(null)
const saved = ref(null)
const busy = ref(false)
const ops = ref([])
const match = ref(null)
const form = reactive({
  type: QcType.IQC,
  sourceNo: '',
  woOpId: null,
  material: null,
  batchNo: '',
  inspectQty: '',
  inspectDate: today(),
  sampleQty: '',
  defectQty: '0',
  result: QcResult.PASS,
  defectType: null,
  defectDesc: '',
  remark: '',
  items: []
})

const readonly = computed(() => saved.value?.status === InspectionStatus.CONFIRMED || (id.value ? !hasPerm('mes:qc:edit') : !hasPerm('mes:qc:add')))
const canConfirm = computed(() => hasPerm('mes:qc:edit'))
const defectTypes = computed(() => dict.mes_defect_type)
const sourcePh = computed(() =>
  form.type === QcType.IQC ? t('mes.inspection.receiptNoPh') : form.type === QcType.FAI ? t('mes.inspection.woNoPh') : t('mes.inspection.woNoOptional')
)
const opLabel = computed(() => ops.value.find((o) => o.value === form.woOpId)?.label || '')
/** 已确认的检验单显示保存的方案；草稿按当前条件匹配 */
const usePlan = computed(() => (readonly.value ? !!saved.value?.planId : !!match.value?.plan))
const planName = computed(() => (readonly.value ? saved.value?.planName : match.value?.plan?.name) || '')

onLoad(async (q) => {
  if (q.type && Object.values(QcType).includes(q.type)) form.type = q.type
  if (q.id) {
    id.value = Number(q.id)
    await load()
  }
  uni.setNavigationBarTitle({ title: t(id.value ? (readonly.value ? 'mes.inspection.viewTitle' : 'mes.inspection.editTitle') : 'mes.inspection.addTitle') })
})

/** 详情没有物料、工单等名称，名称取列表中的同一条记录 */
async function load() {
  const d = await getInspection(id.value).catch(() => null)
  if (!d) return
  const page = await pageInspection({ keyword: d.inspNo, current: 1, size: 5 }).catch(() => null)
  const row = (page?.records || []).find((r) => r.id === d.id) || {}
  saved.value = { ...row, ...d, materialCode: row.materialCode, materialName: row.materialName, opName: row.opName, planName: row.planName }
  Object.assign(form, {
    type: d.type,
    sourceNo: d.sourceNo || '',
    woOpId: d.woOpId,
    material: { id: d.materialId, code: row.materialCode, name: row.materialName, spec: row.spec },
    batchNo: d.batchNo || '',
    inspectQty: d.inspectQty != null ? String(Number(d.inspectQty)) : '',
    inspectDate: d.inspectDate,
    sampleQty: d.sampleQty != null ? String(Number(d.sampleQty)) : '',
    defectQty: d.defectQty != null ? String(Number(d.defectQty)) : '0',
    result: d.result,
    defectType: d.defectType,
    defectDesc: d.defectDesc || '',
    remark: d.remark || ''
  })
  const items = (d.items || []).map((i) => ({
    planItemId: i.planItemId,
    name: i.name,
    method: i.method,
    standard: i.standard,
    lowerLimit: i.lowerLimit,
    upperLimit: i.upperLimit,
    unit: i.unit,
    critical: i.critical === 1,
    measuredText: (i.measured || '').split(',').filter(Boolean).join(' '),
    defectCount: i.defectCount ?? 0
  }))
  if (readonly.value) {
    form.items = items
    match.value = d.rejectQty != null ? { sampleQty: d.sampleQty, sampleCode: d.sampleCode, acceptQty: d.acceptQty, rejectQty: d.rejectQty } : null
  } else {
    pendingItems = Object.fromEntries(items.map((i) => [i.planItemId, i]))
    if (form.type === QcType.FAI) loadWo()
  }
}

function changeType(e) {
  form.type = dict.mes_qc_type[e.detail.value].value
  ops.value = []
  form.woOpId = null
  loadWo()
}

async function scanSource() {
  const code = await scanCode()
  if (!code) return
  form.sourceNo = code
  loadWo()
}

/** 过程 / 成品 / 首件检验：按工单号带出物料，首件检验加载工序 */
let lastWo = ''
async function loadWo() {
  const no = form.sourceNo.trim()
  if (readonly.value || form.type === QcType.IQC || !no || no === lastWo) return
  lastWo = no
  const wo = await findWorkOrder(pageWorkOrder, no).catch(() => null)
  if (!wo) {
    ops.value = []
    if (form.type === QcType.FAI) uni.showToast({ title: t('mes.common.woNotFound', { no }), icon: 'none' })
    return
  }
  const d = await getWorkOrder(wo.id).catch(() => null)
  if (!d) return
  if (!form.material) form.material = { id: d.materialId, code: d.materialCode, name: d.materialName, spec: d.spec, unit: d.unit }
  ops.value = (d.ops || []).map((o) => ({
    value: o.id,
    label: o.firstArticle === 1 ? t('mes.inspection.opFai', { seq: o.seq, name: o.name }) : `${o.seq} ${o.name}`
  }))
}

// ---------------------------------------------------------------- 检验方案匹配
let pendingItems = null
let matchSeq = 0
let timer = null
async function rematch() {
  if (readonly.value) return
  if (!form.type || !form.material?.id || !(Number(form.inspectQty) > 0)) {
    match.value = null
    form.items = []
    return
  }
  const seq = ++matchSeq
  const m = await matchPlan({ type: form.type, materialId: form.material.id, lotQty: form.inspectQty }).catch(() => null)
  if (seq !== matchSeq) return
  const prev = Object.fromEntries(form.items.map((i) => [i.planItemId, i]))
  const keep = pendingItems || {}
  pendingItems = null
  match.value = m
  form.items = (m?.plan?.items || []).map((p) => {
    const old = prev[p.id] || keep[p.id]
    return {
      planItemId: p.id,
      name: p.name,
      method: p.method,
      standard: p.standard,
      lowerLimit: p.lowerLimit,
      upperLimit: p.upperLimit,
      unit: p.unit,
      critical: p.critical === 1,
      measuredText: old?.measuredText ?? '',
      defectCount: old?.defectCount ?? 0
    }
  })
}
watch(
  () => [form.type, form.material?.id, form.inspectQty],
  () => {
    clearTimeout(timer)
    timer = setTimeout(rematch, 400)
  }
)

function defectsOf(item) {
  if (item.method === ItemMethod.QUANTITATIVE) return parseMeasured(item.measuredText).filter((v) => outOfSpec(v, item)).length
  return Number(item.defectCount || 0)
}
const planDefects = computed(() => form.items.reduce((s, i) => s + defectsOf(i), 0))
/** 页面提示：关键项目有不合格或不合格合计达到 Re 判不合格（与 Web 端一致，保存后以后端为准） */
const planResult = computed(() => {
  if (readonly.value && saved.value?.result) return saved.value.result === QcResult.FAIL ? QcResult.FAIL : QcResult.PASS
  const criticalFail = form.items.some((i) => i.critical && defectsOf(i) > 0)
  return criticalFail || planDefects.value >= Number(match.value?.rejectQty ?? 1) ? QcResult.FAIL : QcResult.PASS
})

function step(item, d) {
  const max = Number(match.value?.sampleQty) || Infinity
  item.defectCount = Math.min(max, Math.max(0, Number(item.defectCount || 0) + d))
}

// ---------------------------------------------------------------- 保存、确认
function validate() {
  if (!form.type) return t('common.selectRequired', { name: t('mes.inspection.typeRequired') })
  if (form.type === QcType.FAI && !form.sourceNo.trim()) return t('common.required', { name: t('mes.common.woNo') })
  if (form.type === QcType.FAI && !form.woOpId) return t('common.selectRequired', { name: t('mes.inspection.opRequired') })
  if (!form.material?.id) return t('common.selectRequired', { name: t('mes.common.material') })
  if (!(Number(form.inspectQty) > 0)) return t('common.required', { name: t('mes.inspection.inspectQty') })
  if (!usePlan.value && !(Number(form.sampleQty) > 0)) return t('common.required', { name: t('mes.inspection.sampleQty') })
  return null
}

function payload() {
  const base = {
    type: form.type,
    sourceNo: form.sourceNo.trim() || null,
    woOpId: form.type === QcType.FAI ? form.woOpId : null,
    materialId: form.material.id,
    batchNo: form.batchNo.trim() || null,
    inspectQty: Number(form.inspectQty),
    defectType: form.defectType || null,
    defectDesc: form.defectDesc.trim() || null,
    inspectDate: form.inspectDate,
    remark: form.remark || null
  }
  if (usePlan.value) {
    return {
      ...base,
      planId: match.value.plan.id,
      items: form.items.map((i) => ({
        planItemId: i.planItemId,
        measured: i.method === ItemMethod.QUANTITATIVE ? parseMeasured(i.measuredText) : null,
        defectCount: i.method === ItemMethod.QUALITATIVE ? Number(i.defectCount || 0) : null
      }))
    }
  }
  return { ...base, sampleQty: Number(form.sampleQty), defectQty: Number(form.defectQty || 0), result: form.result }
}

async function save(andConfirm) {
  const err = validate()
  if (err) return uni.showToast({ title: err, icon: 'none' })
  if (andConfirm) {
    const fail = usePlan.value ? planResult.value === QcResult.FAIL : form.result === QcResult.FAIL
    const ok = await new Promise((resolve) =>
      uni.showModal({
        title: t('common.tip'),
        content: fail ? t('mes.inspection.confirmFail') : t('mes.inspection.confirmPass'),
        confirmText: t('common.ok'),
        cancelText: t('common.cancel'),
        success: (r) => resolve(r.confirm)
      })
    )
    if (!ok) return
  }
  busy.value = true
  try {
    await runAction(
      async () => {
        if (id.value) await updateInspection(id.value, payload())
        else id.value = await addInspection(payload())
        if (andConfirm) await confirmInspection(id.value)
      },
      { success: andConfirm ? t('mes.inspection.confirmedMsg') : t('mes.inspection.saved'), then: load }
    )
  } finally {
    busy.value = false
  }
}
</script>

<style lang="scss">
.ie {
  &__no {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 12rpx;
    font-size: 28rpx;
    font-weight: 600;
  }
  &__row {
    display: flex;
    align-items: center;
  }
  &__link {
    padding-left: 16rpx;
    font-size: 26rpx;
    color: $l-primary;
    white-space: nowrap;
  }
  &__plan {
    margin: 24rpx 24rpx 0;
    padding: 20rpx 24rpx;
    border-radius: $l-radius;
    background: #e6f4ff;
    font-size: 24rpx;
    color: $l-text-2;
    line-height: 1.6;
  }
  &__plan-name {
    display: block;
    font-size: 28rpx;
    font-weight: 600;
    color: $l-text;
  }
  &__item {
    margin: 20rpx 24rpx;
    padding: 24rpx 28rpx;
    background: #fff;
    border-radius: $l-radius;
  }
  &__item-head {
    display: flex;
    align-items: center;
    gap: 12rpx;
  }
  &__item-name {
    flex: 1;
    font-size: 30rpx;
    font-weight: 600;
  }
  &__item-spec {
    margin: 8rpx 0 16rpx;
    font-size: 24rpx;
    color: $l-text-3;
  }
  &__measured {
    width: 100%;
    min-height: 72rpx;
    padding: 16rpx 20rpx;
    box-sizing: border-box;
    border-radius: 12rpx;
    background: $l-bg;
    font-size: 28rpx;
  }
  &__item-foot {
    display: flex;
    justify-content: space-between;
    margin-top: 12rpx;
    font-size: 24rpx;
    color: $l-text-3;
  }
  &__danger {
    color: $l-danger;
  }
  &__count {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 28rpx;
    color: $l-text-2;
  }
  &__stepper {
    display: flex;
    align-items: center;
    border: 1rpx solid #d9d9d9;
    border-radius: 12rpx;
    overflow: hidden;
  }
  &__step {
    width: 72rpx;
    line-height: 64rpx;
    text-align: center;
    font-size: 32rpx;
    background: $l-bg;
  }
  &__step-input {
    width: 120rpx;
    height: 64rpx;
    text-align: center;
    font-size: 28rpx;
  }
  &__judge {
    display: flex;
    align-items: center;
    gap: 16rpx;
    margin: 8rpx 32rpx 0;
    font-size: 28rpx;
  }
  &__tip {
    margin: 12rpx 32rpx 0;
    font-size: 24rpx;
    color: #d46b08;
    &--card {
      margin: 0 0 8rpx;
      color: $l-text-3;
    }
  }
}
</style>
