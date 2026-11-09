<template>
  <div>
    <div class="items-head">
      <span class="section-title">
        {{ $t('scm.trade.itemsSummary', { rows: form.items.length, qty: formatQty(totalQty), amount: formatMoney(totalAmount) }) }}
      </span>
      <a-space>
        <a-button v-if="inbound" size="small" :disabled="!form.warehouseId || !form.items.length" @click="putaway">{{ $t('scm.trade.suggestLocation') }}</a-button>
        <a-button v-if="direct" size="small" type="primary" ghost @click="addRow()">
          <template #icon><PlusOutlined /></template>{{ $t('scm.trade.addLine') }}
        </a-button>
      </a-space>
    </div>

    <LineItemsTable
      class="items-table"
      :items="form.items"
      :columns="columns"
      :empty-text="direct ? $t('scm.trade.emptyDirect') : form.orderId ? $t('scm.trade.noItems', { action: conf.action }) : $t('scm.trade.selectOrderFirst')"
    >
      <template #cell="{ column, record }">
        <template v-if="column.key === 'material'">
          <template v-if="record.gift">
            <div>{{ record.materialName }}</div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <a-form-item v-else-if="direct" :name="lines.itemName(record, 'materialId')" :rules="requiredRule($t('scm.trade.selectMaterial'))" class="cell-item">
            <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => applyMaterial(record, m)" />
          </a-form-item>
          <template v-else>
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <div class="row-tags">
            <a-tag v-if="record.gift" color="green" :bordered="false">{{ $t('scm.trade.giftTag') }}</a-tag>
            <a-tag v-if="record.batchEnabled === 1" color="blue" :bordered="false">{{ $t('scm.trade.batch') }}</a-tag>
            <a-tag v-if="record.serialEnabled === 1" color="purple" :bordered="false">{{ $t('scm.trade.serialNo') }}</a-tag>
          </div>
        </template>
        <template v-else-if="column.key === 'limit'">
          <div>{{ formatQty(record.limit) }} {{ record.unit }}</div>
          <div class="sub-text">¥{{ formatMoney(record.price) }} / {{ record.unit || $t('common.field.unit') }}</div>
        </template>
        <template v-else-if="column.key === 'warehouse'">
          <WarehouseSelect
            :value="record.warehouseId"
            :placeholder="$t('scm.trade.sameAsHeader')"
            @update:value="(v) => changeRowWarehouse(record, v)"
          />
        </template>
        <template v-else-if="column.key === 'slot'">
          <a-form-item :name="lines.itemName(record, 'slot')" :rules="requiredRule($t('scm.trade.selectSlot'))" class="cell-item">
            <StockSlotSelect
              v-model:value="record.slot"
              :label="record.slotLabel"
              :warehouse-id="rowWarehouse(record)"
              :material-id="record.materialId"
              :include-frozen="kind === 'purchaseReturn'"
              @select="(s) => applySlot(record, s)"
            />
          </a-form-item>
          <div v-if="record.available != null" class="sub-text">{{ $t('scm.trade.availableN', { qty: formatQty(record.available) }) }}</div>
        </template>
        <template v-else-if="column.key === 'location'">
          <LocationSelect v-model:value="record.locationId" :warehouse-id="form.warehouseId" />
        </template>
        <template v-else-if="column.key === 'batchNo'">
          <a-form-item
            :name="lines.itemName(record, 'batchNo')"
            :rules="record.batchEnabled === 1 ? requiredRule($t('scm.trade.inputBatchNo')) : []"
            class="cell-item"
          >
            <a-input v-model:value="record.batchNo" :maxlength="64" :disabled="record.batchEnabled !== 1" :placeholder="$t('scm.trade.batchNo')" />
          </a-form-item>
          <template v-if="kind === 'purchaseIn' && record.batchEnabled === 1">
            <a-date-picker v-model:value="record.productionDate" value-format="YYYY-MM-DD" :placeholder="$t('scm.trade.productionDate')" class="full date-row" />
            <a-date-picker v-model:value="record.expiryDate" value-format="YYYY-MM-DD" :placeholder="$t('scm.trade.expiryDate')" class="full date-row" />
          </template>
        </template>
        <template v-else-if="column.key === 'qty'">
          <a-form-item :name="lines.itemName(record, 'qty')" :rules="requiredRule($t('scm.trade.inputQty'))" class="cell-item">
            <a-input-number
              v-model:value="record.qty"
              :min="0"
              :precision="record.serialEnabled === 1 ? 0 : 4"
              :addon-after="record.unit || undefined"
              class="full"
              @change="direct && !record.gift && fillPrices([record])"
            />
          </a-form-item>
          <div v-if="record.gift" class="sub-text">{{ $t('scm.trade.giftExpected', { qty: formatQty(giftExpected(promo?.gifts, record.materialId)) }) }}</div>
        </template>
        <template v-else-if="column.key === 'price' && record.gift">
          <div class="promo-text">{{ $t('scm.trade.giftPrice') }}<span v-if="record.promotionName">（{{ record.promotionName }}）</span></div>
        </template>
        <template v-else-if="column.key === 'price'">
          <a-form-item :name="lines.itemName(record, 'price')" :rules="requiredRule($t('scm.trade.inputPrice'))" class="cell-item">
            <a-input-number v-model:value="record.price" :min="0" :precision="4" prefix="¥" class="full" @change="record.priceAuto = false" />
          </a-form-item>
          <div class="sub-text price-links">
            <span v-if="record.priceSource && record.priceAuto">{{ priceSourceText(record.priceSource) }}</span>
            <a v-if="record.materialId" @click="openReference(record)">{{ $t('scm.trade.priceReference') }}</a>
          </div>
          <div v-if="promoLine(record)" class="promo-text">
            {{ $t('scm.trade.promoPrice', { price: formatMoney(promoLine(record).price) }) }}
            <span v-if="promoLine(record).promotionName">（{{ promoLine(record).promotionName }}）</span>
          </div>
        </template>
        <template v-else-if="column.key === 'taxRate'">
          <a-input-number v-model:value="record.taxRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" :disabled="record.gift" />
        </template>
        <template v-else-if="column.key === 'amount'">
          <div class="amount">¥{{ formatMoney(lineAmount(record.qty, record.price)) }}</div>
          <div class="sub-text">{{ $t('scm.trade.taxRateN', { rate: Number(record.taxRate || 0) }) }}</div>
        </template>
        <template v-else-if="column.key === 'serialNos'">
          <SerialInput
            v-if="record.serialEnabled === 1"
            v-model:value="record.serialNos"
            :expected="record.qty ?? 0"
            :source="conf.fromStock ? serialSource(record) : undefined"
            :placeholder="conf.fromStock ? $t('scm.trade.selectOrScan') : $t('scm.trade.scanOrInputSerial')"
          />
          <span v-else class="sub-text">-</span>
        </template>
      </template>
      <template #action="{ record }">
        <a-tooltip :title="$t('scm.trade.splitTip')">
          <a-button type="link" size="small" @click="splitRow(record)">
            <template #icon><SplitCellsOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-button v-if="!record.gift || giftRowCount(record.materialId) > 1" type="link" danger size="small" @click="lines.removeRow(record)">
          <template #icon><DeleteOutlined /></template>
        </a-button>
      </template>
    </LineItemsTable>
    <div v-if="direct && promo?.discountAmount > 0" class="promo-text promo-total">
      {{ $t('scm.trade.promoTotal', { amount: formatMoney(promo.discountAmount) }) }}
    </div>
    <WholeDiscountBar v-if="direct && form.items.length" v-model:value="form.wholeDiscount" :total="discountBase" />
    <PriceReference ref="referenceRef" pickable @pick="pickPrice" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, PlusOutlined, SplitCellsOutlined } from '@ant-design/icons-vue'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import LocationSelect from '@/components/wms/LocationSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import SerialInput from '@/components/wms/SerialInput.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import StockSlotSelect from './StockSlotSelect.vue'
import PriceReference from './PriceReference.vue'
import WholeDiscountBar from './WholeDiscountBar.vue'
import { defaultPrices } from '@/api/scm/pricing'
import { previewPromotion } from '@/api/scm/sourcing'
import { giftExpected, syncGifts } from './giftRows'
import { useLineItems } from '@/composables/useLineItems'
import { fillPutaway, formatQty } from '@/components/wms/wms'
import { TRADE_KINDS, formatMoney, lineAmount, newTradeRow } from './scm'
import { round2 } from '@/utils/number'

/**
 * 出入库单的明细：引用订单时按订单明细录入数量（可拆分库位、批次）；直接开单时选择商品、数量、单价、税率。
 * 销售出库在参数开启时每行可另选仓库（为空取表头仓库）。直接修改 form.items
 */
const props = defineProps({
  kind: { type: String, required: true },
  /** 表单：partnerId、warehouseId、bizDate、items */
  form: { type: Object, required: true },
  /** 直接开单（不引用订单） */
  direct: Boolean,
  /** 明细可选仓库（仅销售出库） */
  lineWarehouse: Boolean
})
const { t } = useI18n()

const conf = computed(() => TRADE_KINDS[props.kind])
const priceKind = computed(() => (conf.value.orderKind === 'sales' ? 'SALES' : 'PURCHASE'))
/** 入库类单据（采购入库、销售退货）可按上架策略推荐库位 */
const inbound = computed(() => props.kind === 'purchaseIn' || props.kind === 'salesReturn')
const requiredRule = (msg) => [{ required: true, message: msg }]

const lines = useLineItems(() => props.form.items, { newRow: newTradeRow })
const addRow = (extra) => lines.addRow(extra)

const columns = computed(() => {
  const c = conf.value
  return [
    { title: t('scm.trade.material'), key: 'material', width: 240 },
    ...(props.direct ? [] : [{ title: c.returns ? t('scm.trade.returnableQty') : t('scm.trade.openQty'), key: 'limit', width: 140 }]),
    ...(props.lineWarehouse ? [{ title: t('scm.trade.warehouse'), key: 'warehouse', width: 170 }] : []),
    ...(c.fromStock
      ? [{ title: t('scm.trade.locationBatch'), key: 'slot', width: 230 }]
      : [
          { title: t('scm.trade.location'), key: 'location', width: 180 },
          { title: t('scm.trade.batchNo'), key: 'batchNo', width: 160 }
        ]),
    { title: t('common.field.quantity'), key: 'qty', width: 150 },
    ...(props.direct
      ? [
          { title: t('scm.trade.taxPrice'), key: 'price', width: 160 },
          { title: t('scm.trade.taxRate'), key: 'taxRate', width: 110 },
          { title: t('scm.trade.totalAmount'), key: 'amount', width: 120, align: 'right' }
        ]
      : []),
    { title: t('scm.trade.serialNo'), key: 'serialNos', width: 240 },
    { title: '', key: 'action', width: 80, fixed: 'right' }
  ]
})

const totalQty = computed(() => props.form.items.reduce((sum, i) => sum + (Number(i.qty) || 0), 0))
const totalAmount = computed(() => round2(props.form.items.reduce((sum, i) => sum + lineAmount(i.qty, i.price), 0)))

async function putaway() {
  const n = await fillPutaway(props.form.warehouseId, props.form.items)
  message.info(n ? t('scm.trade.suggestedN', { n }) : t('scm.trade.noSuggestion'))
}

// ---------------------------------------------------------------- 仓库、库位
const rowWarehouse = (row) => (props.lineWarehouse && row.warehouseId) || props.form.warehouseId

function clearSlot(row) {
  Object.assign(row, { locationId: null, slot: undefined, slotLabel: undefined, available: undefined, serialNos: undefined })
}

function changeRowWarehouse(row, id) {
  if (id === row.warehouseId) return
  row.warehouseId = id
  clearSlot(row)
}

/** 表头仓库变化：沿用表头仓库的行清空库位，赠品行按新仓库重新先到期先出 */
watch(
  () => props.form.warehouseId,
  async () => {
    props.form.items.forEach((i) => !(props.lineWarehouse && i.warehouseId) && clearSlot(i))
    if (promo.value?.gifts?.length) {
      for (let i = props.form.items.length - 1; i >= 0; i--) if (props.form.items[i].gift) props.form.items.splice(i, 1)
      await syncGifts(props.form.items, promo.value.gifts, props.form.warehouseId)
    }
  }
)

function applySlot(row, slot) {
  Object.assign(row, { locationId: slot.locationId, batchNo: slot.batchNo, slotLabel: slot.label, available: Number(slot.qty), serialNos: undefined })
}

const serialSource = (row) =>
  row.slot
    ? { warehouseId: rowWarehouse(row), locationId: row.locationId || undefined, materialId: row.materialId, batchNo: row.batchNo || undefined }
    : undefined

function splitRow(row) {
  const i = props.form.items.indexOf(row)
  lines.addRow({ ...row, key: undefined, qty: undefined, slot: undefined, slotLabel: undefined, locationId: null, batchNo: undefined, available: undefined, serialNos: undefined })
  // addRow 加在末尾，移到原行之后
  const added = props.form.items.pop()
  props.form.items.splice(i + 1, 0, added)
}

// ---------------------------------------------------------------- 直接开单：商品、默认价、价格参考
function applyMaterial(row, m) {
  Object.assign(row, {
    materialLabel: m.label,
    materialCode: m.code,
    materialName: m.name,
    spec: m.spec,
    unit: m.unit,
    batchEnabled: m.batchEnabled,
    serialEnabled: m.serialEnabled,
    priceAuto: true,
    batchNo: undefined
  })
  clearSlot(row)
  fillPrices([row])
}

/** 按往来单位、日期带出默认价（只覆盖没有手工改过的单价） */
async function fillPrices(rows) {
  const targets = rows.filter((r) => r.materialId && r.priceAuto)
  if (!targets.length || !props.form.partnerId) return
  const list = await defaultPrices({
    kind: priceKind.value,
    partnerId: props.form.partnerId,
    materialIds: targets.map((r) => r.materialId),
    qtys: targets.map((r) => r.qty),
    date: props.form.bizDate
  }).catch(() => [])
  targets.forEach((r, i) => {
    // 默认价按行返回（与请求的物料一一对应，同一物料多行按各行数量匹配起订量）
    const p = list?.[i]?.materialId === r.materialId ? list[i] : undefined
    if (!p || !r.priceAuto) return
    if (p.price == null) return
    r.price = Number(p.price)
    if (p.taxRate != null) r.taxRate = Number(p.taxRate)
    r.priceSource = p.source
  })
}

/** 往来单位或日期变化后重新带出默认价 */
watch(
  () => (props.direct ? `${props.form.partnerId}|${props.form.bizDate}` : null),
  (v, old) => v && v !== old && fillPrices(props.form.items)
)

const priceSourceText = (source) => t(`pricing.source.${source}`)

const referenceRef = ref()
let referenceRow
function openReference(row) {
  referenceRow = row
  referenceRef.value?.open({ materialId: row.materialId, materialName: row.materialLabel, partnerId: props.form.partnerId, kind: priceKind.value })
}
/** 价格参考中「使用此价格」 */
function pickPrice(price) {
  if (!referenceRow) return
  Object.assign(referenceRow, { price, priceAuto: false, priceSource: undefined })
}

// ---------------------------------------------------------------- 销售出库：促销预览，买赠的赠品行随单出库
const promo = ref()
let promoTimer
let promoSeq = 0
const goodsRows = () => props.form.items.filter((i) => !i.gift)
const giftRowCount = (materialId) => props.form.items.filter((i) => i.gift && i.materialId === materialId).length
watch(
  () => props.direct && props.kind === 'salesOut'
    ? JSON.stringify([props.form.partnerId, props.form.bizDate, goodsRows().map((i) => [i.materialId, i.qty, i.price])])
    : null,
  (key) => {
    clearTimeout(promoTimer)
    if (!key) return applyPromo(undefined)
    promoTimer = setTimeout(async () => {
      const f = props.form
      const rows = goodsRows()
      const ready = rows.length && rows.every((i) => i.materialId && Number(i.qty) > 0 && i.price != null)
      if (!f.partnerId || !f.bizDate || !ready) return applyPromo(undefined)
      const seq = ++promoSeq
      const res = await previewPromotion({
        customerId: f.partnerId, orderDate: f.bizDate,
        items: rows.map((i) => ({ materialId: i.materialId, qty: i.qty, price: i.price }))
      }).catch(() => undefined)
      if (seq === promoSeq) applyPromo(res)
    }, 500)
  }
)
/** 预览结果：赠品同步到明细末尾（没有预览结果时保留已有赠品行，保存时后端按促销校验） */
async function applyPromo(res) {
  promo.value = res
  if (res) await syncGifts(props.form.items, res.gifts, props.form.warehouseId)
  else if (!props.direct || props.kind !== 'salesOut') props.form.items.splice(0, props.form.items.length, ...goodsRows())
}
/** 直接开单整单优惠的基数：扣除促销优惠后的合计 */
const discountBase = computed(() => round2(totalAmount.value - (Number(promo.value?.discountAmount) || 0)))
const promoLine = (row) => {
  const line = promo.value?.lines?.[goodsRows().indexOf(row)]
  return line && line.origPrice != null ? line : null
}

defineExpose({ addRow, fillPrices })
</script>

<style scoped>
.full {
  width: 100%;
}
.date-row {
  margin-top: 4px;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
}
.section-title {
  font-weight: 500;
}
.items-table :deep(.ant-table-cell) {
  vertical-align: top;
}
.cell-item {
  margin-bottom: 0;
}
.row-tags {
  margin-top: 4px;
}
.row-tags:empty {
  display: none;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.price-links {
  display: flex;
  gap: 8px;
  margin-top: 2px;
}
.promo-text {
  color: var(--ant-color-success, #52c41a);
  font-size: 12px;
}
.promo-total {
  margin-top: 8px;
  text-align: right;
}
.amount {
  font-weight: 500;
}
</style>
