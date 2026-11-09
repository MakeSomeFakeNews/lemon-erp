<template>
  <!-- 折让记录（SettlementPage 的标签页）：已生效收付款中有折让的核销明细，可导出 -->
  <div class="discounts">
    <div class="toolbar">
      <a-space wrap>
        <PartnerSelect v-model:value="query.partnerId" :type="conf.partner" allow-clear :placeholder="conf.allPartners" class="filter" />
        <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" :allow-empty="[true, true]" @change="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.settlementPage.discountKeywordPh')" allow-clear class="filter" @search="search" />
      </a-space>
      <a-button :loading="exporting" @click="doExport">
        <template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}
      </a-button>
    </div>
    <div class="sub-text tip">{{ $t('fin.settlementPage.discountTip') }}</div>
    <a-table
      row-key="id"
      size="middle"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1180 }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'settleNo'">
          <a @click="emit('open', record.settlementId)">{{ record.settleNo }}</a>
        </template>
        <template v-else-if="column.key === 'bill'">
          {{ record.billNo }}
          <div class="sub-text"><DictTag type="fin_bill_source" :value="record.sourceType" /> {{ record.sourceNo || '' }}</div>
        </template>
        <template v-else-if="column.key === 'discount'">
          ¥{{ formatMoney(record.discount) }}
          <div v-if="isForeign(record.currency)" class="sub-text">{{ record.currency }} {{ formatMoney(record.discountFc) }}</div>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { pageSettlementDiscounts } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { formatMoney } from './fin'
import { isForeign } from './currency'
import { t } from '@/locales'

/** kind = receipt | payment，conf 为页面按收付款区分的文字；点击单号触发 open(收付款单 ID) */
const props = defineProps({
  kind: { type: String, required: true },
  conf: { type: Object, required: true }
})
const emit = defineEmits(['open'])
useDict('fin_bill_source')

const columns = [
  { title: t('fin.settlementPage.discountDate'), dataIndex: 'settleDate', width: 110 },
  { title: t('fin.settlementPage.no'), key: 'settleNo', dataIndex: 'settleNo', width: 170 },
  { title: props.conf.partnerLabel, dataIndex: 'partnerName', width: 220, ellipsis: true },
  { title: t('fin.settlementPage.discountBill'), key: 'bill', dataIndex: 'billNo', width: 220 },
  { title: t('fin.settlementPage.discountAmount'), key: 'discount', dataIndex: 'discount', width: 150, align: 'right' },
  { title: t('fin.settlementPage.handler'), dataIndex: 'createByName', width: 120 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
]
/** 导出的列：原单据拆分来源，外币另列原币 */
const exportColumns = [
  ...columns.slice(0, 4),
  { title: t('fin.settlementPage.sourceNo'), dataIndex: 'sourceNo', width: 160 },
  { title: t('fin.settlementPage.discountAmount'), dataIndex: 'discount', width: 120 },
  { title: t('fin.settlementPage.fcRate'), width: 140, exportValue: (r) => (isForeign(r.currency) ? `${r.currency} ${formatMoney(r.discountFc)}` : '') },
  ...columns.slice(5)
]

const query = reactive({ partnerId: undefined, keyword: undefined, range: undefined })
const { list, loading, pagination, load, search, onTableChange, exportAll } = useTable((p) => pageSettlementDiscounts(props.kind, p), query, {
  params: ({ range, ...q }) => ({ ...q, beginDate: range?.[0] || undefined, endDate: range?.[1] || undefined })
})
watch(() => query.partnerId, () => search())
onMounted(load)

const exporting = ref(false)
async function doExport() {
  exporting.value = true
  try {
    await exportAll(exportColumns, props.conf.discountExport)
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
}
.filter {
  width: 240px;
}
.tip {
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
