<template>
  <a-spin :spinning="loading">
    <a-empty v-if="!loading && !list.length" description="没有变更记录" />
    <div v-for="c in list" :key="c.id" class="change">
      <div class="change-head">
        <a-space wrap>
          <b>{{ c.changeNo }}</b>
          <a-tag v-if="c.version" color="blue" :bordered="false">V{{ c.version }}</a-tag>
          <DictTag type="scm_change_status" :value="c.status" />
          <span>
            价税合计 {{ symbol }}{{ formatMoney(c.oldAmount) }}
            <ArrowRightOutlined class="arrow" />
            <b>{{ symbol }}{{ formatMoney(c.newAmount) }}</b>
          </span>
        </a-space>
        <span class="sub-text">
          {{ c.createByName || '-' }} {{ c.createTime }}
          <template v-if="c.approveTime">，{{ c.status === 'REJECTED' ? '驳回' : '生效' }} {{ c.approveTime }}</template>
        </span>
      </div>
      <div class="reason">变更原因：{{ c.reason || '-' }}</div>
      <a-table row-key="key" size="small" :columns="columns" :data-source="rows(c)" :pagination="false" :scroll="{ x: 860 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-tag :color="ACTIONS[record.action]?.color" :bordered="false">{{ ACTIONS[record.action]?.label || record.action }}</a-tag>
          </template>
          <template v-else-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">
            <OldNew :old="record.oldQty" :new="record.newQty" :fmt="(v) => `${formatQty(v)} ${record.unit || ''}`" />
          </template>
          <template v-else-if="column.key === 'price'">
            <OldNew :old="record.oldPrice" :new="record.newPrice" :fmt="(v) => `${symbol}${formatMoney(v)}`" />
          </template>
          <template v-else-if="column.key === 'deliveryDate'">
            <OldNew :old="record.oldDeliveryDate" :new="record.newDeliveryDate" :fmt="(v) => v" />
          </template>
        </template>
      </a-table>
    </div>
  </a-spin>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { ArrowRightOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { orderChanges } from '@/api/scm/order'
import { formatQty } from '@/components/wms/wms'
import { currencySymbol } from '@/components/fin/currency'
import { formatMoney } from './scm'

/** 订单变更记录：每次变更的原因、状态、金额及明细的新旧值 */
const props = defineProps({
  kind: { type: String, required: true },
  orderId: { type: Number, required: true },
  currency: String
})

const ACTIONS = {
  MODIFY: { label: '修改', color: 'orange' },
  REMOVE: { label: '删除', color: 'red' },
  ADD: { label: '新增', color: 'green' }
}
const columns = [
  { title: '动作', key: 'action', width: 70 },
  { title: '物料', key: 'material', width: 220 },
  { title: '数量', key: 'qty', width: 200 },
  { title: '含税单价', key: 'price', width: 200 },
  { title: '交货日期', key: 'deliveryDate', width: 200 }
]

/** 旧值 → 新值；相同时只显示一个，空值显示 - */
const OldNew = (p) => {
  const empty = (v) => v === null || v === undefined || v === ''
  const text = (v) => (empty(v) ? '-' : p.fmt(v))
  const same = String(p.old ?? '') === String(p.new ?? '') || (!empty(p.old) && !empty(p.new) && Number(p.old) === Number(p.new))
  if (same) return h('span', text(p.new))
  return h('span', [h('span', { class: 'old' }, text(p.old)), h(ArrowRightOutlined, { class: 'arrow' }), h('b', text(p.new))])
}
OldNew.props = ['old', 'new', 'fmt']

const symbol = computed(() => currencySymbol(props.currency))
const list = ref([])
const loading = ref(false)
const rows = (c) => (c.items || []).map((i, n) => ({ ...i, key: n }))

async function load() {
  loading.value = true
  try {
    list.value = await orderChanges(props.kind, props.orderId)
  } finally {
    loading.value = false
  }
}
onMounted(load)
defineExpose({ reload: load })
</script>

<style scoped>
.change {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px dashed var(--app-border, #f0f0f0);
}
.change:last-child {
  border-bottom: none;
}
.change-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}
.reason {
  margin-bottom: 8px;
  color: var(--app-text-2);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
:deep(.arrow),
.arrow {
  color: var(--app-text-3);
  font-size: 12px;
  margin: 0 4px;
}
:deep(.old) {
  color: var(--app-text-3);
  text-decoration: line-through;
}
</style>
