<template>
  <!-- MRP 结果表：只列决策需要的信息，净需求的计算过程悬浮查看；待下达的建议可改数量、供应商 -->
  <a-table
    row-key="id"
    size="small"
    :columns="columns"
    :data-source="results"
    :loading="loading"
    :pagination="false"
    :row-selection="rowSelection"
    :scroll="{ y: '100%' }"
    :locale="{ emptyText }"
  >
    <template #headerCell="{ column }">
      <template v-if="column.help">
        {{ column.title }}
        <a-tooltip :title="column.help">
          <QuestionCircleOutlined class="help" />
        </a-tooltip>
      </template>
    </template>
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'material'">
        <div class="material">
          <a-tooltip :title="$t('mes.mrp.level', { n: record.levelNo })">
            <span class="level">L{{ record.levelNo }}</span>
          </a-tooltip>
          <div class="material-text">
            <div class="ellipsis" :title="`${record.materialCode} ${record.materialName}`">
              <span class="code">{{ record.materialCode }}</span>{{ record.materialName }}
            </div>
            <div v-if="record.spec" class="sub-text ellipsis" :title="record.spec">{{ record.spec }}</div>
          </div>
        </div>
      </template>
      <template v-else-if="column.key === 'supplyType'">
        <DictTag type="mes_supply_type" :value="record.supplyType" />
      </template>
      <template v-else-if="column.key === 'netQty'">
        <a-popover placement="left" :title="$t('mes.mrp.net')">
          <template #content>
            <div v-if="record.supplyType === SupplyType.PHANTOM" class="calc-note">{{ $t('mes.mrp.phantomNet') }}</div>
            <table v-else class="calc">
              <tbody>
                <tr v-for="line in calcLines(record)" :key="line.key" :class="{ result: line.result }">
                  <td class="op">{{ line.op }}</td>
                  <td>{{ line.label }}</td>
                  <td class="num">{{ formatQty(line.value) }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="record.supplyType !== SupplyType.PHANTOM && !(Number(record.netQty) > 0)" class="calc-note">
              {{ $t('mes.mrp.netNone') }}
            </div>
          </template>
          <span class="net" :class="{ positive: Number(record.netQty) > 0 }">{{ formatQty(record.netQty) }}</span>
        </a-popover>
      </template>
      <template v-else-if="column.key === 'suggestQty'">
        <span class="suggest">
          <a-input-number
            v-if="editable(record)"
            :value="drafts[record.id] ?? Number(record.suggestQty)"
            :min="0"
            size="small"
            class="qty-input"
            @change="(v) => (drafts[record.id] = v)"
            @blur="saveQty(record)"
            @press-enter="saveQty(record)"
          />
          <b v-else>{{ formatQty(record.suggestQty) }}</b>
          <span class="sub-text">{{ record.unit }}</span>
        </span>
      </template>
      <template v-else-if="column.key === 'dates'">
        <div>{{ $t('mes.mrp.required', { d: record.requiredDate }) }}</div>
        <a-tooltip :title="overdue(record) ? $t('mes.mrp.overdue') : undefined">
          <div class="sub-text" :class="{ overdue: overdue(record) }">{{ releaseLabel(record) }} {{ record.releaseDate }}</div>
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'supplier'">
        <template v-if="record.supplyType === SupplyType.BUY || record.supplyType === SupplyType.OUTSOURCE">
          <PartnerSelect
            v-if="editable(record)"
            :value="record.supplierId"
            :label="record.supplierName"
            type="supplier"
            :placeholder="$t('mes.mrp.selectSupplier')"
            size="small"
            class="supplier-select"
            @update:value="(v) => saveSupplier(record, v)"
          />
          <div v-else class="ellipsis" :title="record.supplierName">{{ record.supplierName || '-' }}</div>
          <div v-if="record.price != null" class="sub-text">¥{{ formatMoney(record.price) }}</div>
        </template>
        <span v-else class="sub-text">-</span>
      </template>
      <template v-else-if="column.key === 'status'">
        <a-tag v-if="record.status === MrpResultStatus.NEW" color="orange" :bordered="false">{{ $t('mes.mrp.status.NEW') }}</a-tag>
        <template v-else-if="record.status === MrpResultStatus.RELEASED">
          <a-tag color="green" :bordered="false">{{ $t('mes.mrp.status.RELEASED') }}</a-tag>
          <div v-if="record.releaseNo">
            <a v-if="docTarget(record)" class="doc-no" :title="$t('mes.mrp.openDoc')" @click="openDoc(record)">{{ record.releaseNo }}</a>
            <span v-else class="sub-text doc-no">{{ record.releaseNo }}</span>
          </div>
        </template>
        <a-tooltip v-else-if="record.status === MrpResultStatus.EXPIRED" :title="$t('mes.mrp.expiredTip')">
          <a-tag :bordered="false">{{ $t('mes.mrp.status.EXPIRED') }}</a-tag>
        </a-tooltip>
        <a-tag v-else :bordered="false">{{ $t('mes.mrp.status.NONE') }}</a-tag>
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { updateMrpResult } from '@/api/mes/plan'
import { pageWo } from '@/api/mes/wo'
import { pageOrder } from '@/api/scm/order'
import { useUserStore } from '@/store/user'
import { formatMoney } from '@/components/scm/scm'
import { formatQty } from '@/components/wms/wms'
import { MrpResultStatus, SupplyType } from '@/constants/enums'

const props = defineProps({
  results: { type: Array, default: () => [] },
  loading: Boolean,
  emptyText: String,
  canRelease: Boolean,
  selected: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:selected', 'reload'])
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const columns = [
  { title: t('mes.mrp.materialCol'), key: 'material' },
  { title: t('mes.mrp.supplyType'), key: 'supplyType', width: 84 },
  { title: t('mes.mrp.net'), key: 'netQty', width: 110, align: 'right', help: t('mes.mrp.netTip') },
  { title: t('mes.mrp.suggest'), key: 'suggestQty', width: 150, help: t('mes.mrp.suggestTip') },
  { title: t('mes.mrp.dates'), key: 'dates', width: 136 },
  { title: t('mes.mrp.supplier'), key: 'supplier', width: 210 },
  { title: t('common.field.status'), key: 'status', width: 116 }
]

const rowSelection = computed(() =>
  props.canRelease
    ? {
        selectedRowKeys: props.selected,
        preserveSelectedRowKeys: true,
        onChange: (keys) => emit('update:selected', keys),
        getCheckboxProps: (r) => ({ disabled: r.status !== MrpResultStatus.NEW })
      }
    : undefined
)

const editable = (r) => r.status === MrpResultStatus.NEW && props.canRelease

/** 计算过程：净需求 = 毛需求 - (库存 + 在途 - 已分配 - 安全库存)，小于 0 时为 0 */
function calcLines(r) {
  return [
    { key: 'gross', op: '', label: t('mes.mrp.gross'), value: r.grossQty },
    { key: 'onHand', op: '−', label: t('mes.mrp.onHand'), value: r.onHandQty },
    { key: 'scheduled', op: '−', label: t('mes.mrp.scheduled'), value: r.scheduledQty },
    { key: 'allocated', op: '+', label: t('mes.mrp.allocated'), value: r.allocatedQty },
    { key: 'safety', op: '+', label: t('mes.mrp.safety'), value: r.safetyQty },
    { key: 'net', op: '=', label: t('mes.mrp.net'), value: r.netQty, result: true }
  ]
}

const releaseLabel = (r) =>
  r.supplyType === SupplyType.MAKE ? t('mes.mrp.start') : r.supplyType === SupplyType.PHANTOM ? t('mes.mrp.phantom') : t('mes.mrp.order')
const today = dayjs().format('YYYY-MM-DD')
const overdue = (r) => r.status === MrpResultStatus.NEW && !!r.releaseDate && r.releaseDate < today

/** 修改建议数量：输入框失焦或回车时保存，没有变化不提交 */
const drafts = reactive({})
async function saveQty(record) {
  const v = drafts[record.id]
  delete drafts[record.id]
  if (v == null || v === Number(record.suggestQty)) return
  await updateMrpResult(record.id, { suggestQty: v, supplierId: record.supplierId, price: record.price })
  record.suggestQty = v
}
/** 更换供应商：单价改取该供应商的供货价格，保存后重新加载 */
async function saveSupplier(record, supplierId) {
  await updateMrpResult(record.id, { suggestQty: Number(record.suggestQty), supplierId, price: null })
  emit('reload')
}

/** 下达生成的单据：自制件为工单，采购、委外件为采购订单；按单号查到 ID 后跳转到对应页面打开详情 */
function docTarget(r) {
  const make = r.supplyType === SupplyType.MAKE
  const perm = make ? 'mes:wo:list' : 'scm:purchase:list'
  return userStore.hasPerm(perm) ? { make, path: make ? '/mes/wo' : '/scm/purchase' } : null
}
async function openDoc(r) {
  const target = docTarget(r)
  const page = target.make
    ? await pageWo({ current: 1, size: 10, woNo: r.releaseNo })
    : await pageOrder('purchase', { current: 1, size: 10, orderNo: r.releaseNo })
  const doc = page.records.find((d) => (target.make ? d.woNo : d.orderNo) === r.releaseNo)
  if (!doc) {
    message.warning(t('mes.mrp.docNotFound', { no: r.releaseNo }))
    return
  }
  router.push({ path: target.path, query: { id: doc.id } })
}
</script>

<style scoped>
.material {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}
.material-text {
  min-width: 0;
  flex: 1;
}
.level {
  flex: none;
  margin-top: 2px;
  padding: 0 5px;
  border-radius: 4px;
  font-size: 11px;
  line-height: 18px;
  color: var(--app-text-2);
  background: var(--app-hover);
  cursor: default;
}
.code {
  font-weight: 500;
  margin-right: 6px;
}
.net {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  border-bottom: 1px dashed var(--app-text-3);
  cursor: help;
}
.net.positive {
  color: #fa8c16;
}
.suggest {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.qty-input {
  width: 100px;
}
.supplier-select {
  width: 100%;
}
.overdue {
  color: #ff4d4f;
}
.doc-no {
  font-size: 12px;
}
.help {
  color: var(--app-text-3);
  margin-left: 2px;
  cursor: help;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.calc {
  min-width: 180px;
  font-variant-numeric: tabular-nums;
}
.calc td {
  padding: 2px 0;
}
.calc .op {
  width: 16px;
  color: var(--app-text-3);
}
.calc .num {
  text-align: right;
  padding-left: 24px;
}
.calc .result td {
  border-top: 1px solid var(--app-border);
  padding-top: 4px;
  font-weight: 600;
}
.calc-note {
  max-width: 240px;
  margin-top: 4px;
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
