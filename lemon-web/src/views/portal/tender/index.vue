<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load" />
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 900, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'tenderNo'">
            <a @click="openDetail(record.id)">{{ record.tenderNo }}</a>
            <div class="sub-text">{{ record.title }}</div>
          </template>
          <template v-else-if="column.key === 'deadline'">
            {{ record.deadline }}
            <div v-if="biddable(record)" class="sub-text">{{ remain(record) }}</div>
          </template>
          <template v-else-if="column.key === 'mine'">
            <a-tag v-if="record.bidCount" color="green" :bordered="false">已投标</a-tag>
            <span v-else class="sub-text">未投标</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag v-if="record.status === 'AWARDED'" :color="record.awardSupplierId ? 'green' : 'default'" :bordered="false">
              {{ record.awardSupplierId ? '已中标' : '未中标' }}
            </a-tag>
            <DictTag v-else type="scm_tender_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ biddable(record) ? '投标' : '查看' }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <SubPage :open="detailOpen" :title="detail ? `招标 ${detail.tenderNo} ${detail.title}` : ''" width="min(900px, 100vw)" @close="detailOpen = false">
      <template v-if="detail && biddable(detail)" #extra>
        <a-button v-perm="'portal:tender:bid'" type="primary" :loading="saving" @click="submitBid">{{ myBid ? '修改投标' : '提交投标' }}</a-button>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="投标截止">{{ detail.deadline }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <template v-if="detail.status === 'AWARDED'">{{ detail.awardSupplierId ? '已中标' : '未中标' }}</template>
            <DictTag v-else type="scm_tender_status" :value="detail.status" />
          </a-descriptions-item>
          <a-descriptions-item label="我的投标">{{ myBid ? `已提交 ${myBid.submitTime}` : '未投标' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="招标说明" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <a-alert v-if="biddable(detail)" type="info" show-icon class="block" message="截止前可以修改投标，开标前采购方看不到报价" />
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 760 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}<div class="sub-text">{{ record.spec }} {{ record.remark || '' }}</div></template>
            <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
            <template v-else-if="column.key === 'price'">
              <a-input-number v-if="biddable(detail)" v-model:value="prices[record.id].price" :min="0" :precision="4" prefix="¥" class="full" />
              <span v-else>{{ prices[record.id]?.price != null ? '¥' + prices[record.id].price : '-' }}</span>
            </template>
            <template v-else-if="column.key === 'taxRate'">
              <a-input-number v-if="biddable(detail)" v-model:value="prices[record.id].taxRate" :min="0" :max="100" addon-after="%" class="full" />
              <span v-else>{{ prices[record.id]?.taxRate != null ? prices[record.id].taxRate + '%' : '-' }}</span>
            </template>
            <template v-else-if="column.key === 'amount'">
              ¥{{ formatMoney(Number(prices[record.id]?.price || 0) * Number(record.qty)) }}
            </template>
          </template>
        </a-table>
        <a-row :gutter="16" class="bid-foot">
          <a-col :xs="24" :sm="8">
            <a-form-item label="交货期（天）">
              <a-input-number v-model:value="bidForm.deliveryDays" :min="0" :disabled="!biddable(detail)" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="16">
            <a-form-item label="投标说明">
              <a-input v-model:value="bidForm.remark" :maxlength="500" :disabled="!biddable(detail)" />
            </a-form-item>
          </a-col>
        </a-row>
        <div class="total">投标总价 <b>¥{{ formatMoney(total) }}</b></div>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { bidPortalTender, getPortalTender, pagePortalTender } from '@/api/portal'
import { formatMoney } from '@/components/scm/scm'
import { formatQty } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'

/** 供应商门户 - 招标投标：截止前提交或修改报价，定标后显示是否中标 */
const columns = [
  { title: '招标编号 / 标题', key: 'tenderNo', width: 260 },
  { title: '投标截止', key: 'deadline', width: 180 },
  { title: '我的投标', key: 'mine', width: 100 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'portal-tender')
const { list, loading, size, pagination, load, onTableChange } = useTable(pagePortalTender, reactive({}))
onMounted(load)

const biddable = (t) => t.status === 'BIDDING' && dayjs(t.deadline).isAfter(dayjs())
function remain(t) {
  const mins = dayjs(t.deadline).diff(dayjs(), 'minute')
  return mins >= 1440 ? `剩余 ${Math.floor(mins / 1440)} 天` : mins >= 60 ? `剩余 ${Math.floor(mins / 60)} 小时` : `剩余 ${mins} 分钟`
}
const itemColumns = [
  { title: '物料', key: 'material', width: 220 },
  { title: '数量', key: 'qty', width: 110 },
  { title: '需求日期', dataIndex: 'requiredDate', width: 110 },
  { title: '含税单价', key: 'price', width: 160 },
  { title: '税率', key: 'taxRate', width: 110 },
  { title: '小计', key: 'amount', width: 120, align: 'right' }
]

const detailOpen = ref(false)
const detail = ref()
const prices = ref({})
const bidForm = ref({})
const myBid = computed(() => detail.value?.bids?.[0])
const total = computed(() => (detail.value?.items || []).reduce((s, i) => s + Number(prices.value[i.id]?.price || 0) * Number(i.qty), 0))
async function openDetail(id) {
  const t = await getPortalTender(id)
  const own = t.bids?.[0]
  const byItem = new Map((own?.items || []).map((i) => [i.tenderItemId, i]))
  prices.value = Object.fromEntries(t.items.map((i) => [i.id, {
    price: byItem.get(i.id) ? Number(byItem.get(i.id).price) : undefined,
    taxRate: byItem.get(i.id) ? Number(byItem.get(i.id).taxRate) : 13
  }]))
  bidForm.value = { deliveryDays: own?.deliveryDays, remark: own?.remark || '' }
  detail.value = t
  detailOpen.value = true
}
const saving = ref(false)
async function submitBid() {
  const items = detail.value.items.map((i) => ({ tenderItemId: i.id, price: prices.value[i.id].price, taxRate: prices.value[i.id].taxRate }))
  if (items.some((i) => i.price == null)) return message.warning('请为每个物料报价')
  saving.value = true
  try {
    await bidPortalTender(detail.value.id, { deliveryDays: bidForm.value.deliveryDays ?? null, remark: bidForm.value.remark || null, items })
    message.success('投标已提交')
    await openDetail(detail.value.id)
    load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.bid-foot {
  margin-top: 16px;
}
.total {
  text-align: right;
  font-size: 15px;
}
</style>
