<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-input-search v-model:value="query.keyword" placeholder="促销名称" allow-clear class="filter-wide" @search="search" />
        <a-select v-model:value="query.promoType" placeholder="类型" allow-clear :options="toOptions(dict.scm_promo_type)" class="filter" @change="search" />
        <a-checkbox v-model:checked="activeOnly" @change="search">只看今天有效的</a-checkbox>
        <template #extra>
          <a-button v-perm="'scm:promotion:edit'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>新增促销
          </a-button>
        </template>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1200, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="openDetail(record.id)">{{ record.name }}</a>
            <div class="sub-text">{{ record.beginDate }} ~ {{ record.endDate }}</div>
          </template>
          <template v-else-if="column.key === 'type'"><DictTag type="scm_promo_type" :value="record.promoType" /></template>
          <template v-else-if="column.key === 'rule'">{{ ruleText(record) }}</template>
          <template v-else-if="column.key === 'scope'">
            {{ scopeText(record) }}
            <div class="sub-text">{{ record.customerLevel ? `${findDict(dict.scm_customer_level, record.customerLevel)?.label || record.customerLevel} 客户` : '全部客户' }}</div>
          </template>
          <template v-else-if="column.key === 'usage'">
            {{ record.orderCount }} 单
            <div class="sub-text">
              <template v-if="record.promoType === 'GIFT'">赠品 {{ formatQty(record.giftTotal) }}</template>
              <template v-else>优惠 ¥{{ formatMoney(record.discountTotal) }}</template>
            </div>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag v-if="!record.status">已停用</a-tag>
            <a-tag v-else-if="record.endDate < today()">已结束</a-tag>
            <a-tag v-else-if="record.beginDate > today()" color="blue">未开始</a-tag>
            <a-tag v-else color="green">进行中</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record.id)">查看</a-button>
              <template v-if="userStore.hasPerm('scm:promotion:edit')">
                <a-button type="link" size="small" @click="drawer.openEdit(record)">编辑</a-button>
                <a-button type="link" size="small" @click="toggle(record)">{{ record.status ? '停用' : '启用' }}</a-button>
              </template>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增促销" edit-title="编辑促销">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="促销名称" name="name">
          <a-input v-model:value="form.name" :maxlength="64" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="类型" name="promoType">
              <a-radio-group v-model:value="form.promoType" button-style="solid">
                <a-radio-button v-for="o in dict.scm_promo_type" :key="o.value" :value="o.value">{{ o.label }}</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="开始日期" name="beginDate">
              <a-date-picker v-model:value="form.beginDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结束日期" name="endDate">
              <a-date-picker v-model:value="form.endDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="客户等级" name="customerLevel" extra="不选适用全部客户">
              <a-select v-model:value="form.customerLevel" allow-clear placeholder="全部客户" :options="toOptions(dict.scm_customer_level)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="物料分类" name="materialCategory" extra="选了物料时按物料">
              <a-select v-model:value="form.materialCategory" allow-clear placeholder="全部分类" :disabled="!!form.materialId"
                :options="toOptions(dict.wms_material_category)" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="适用物料" name="materialId">
              <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" placeholder="不选按分类" />
            </a-form-item>
          </a-col>
          <template v-if="form.promoType === 'DISCOUNT'">
            <a-col :span="12">
              <a-form-item label="折扣率" required extra="成交价 = 原价 × 折扣率，如 90 为九折">
                <a-input-number v-model:value="form.discountRate" :min="1" :max="99.99" :precision="2" addon-after="%" class="full" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="起订数量" extra="明细数量达到时才打折，不填不限">
                <a-input-number v-model:value="form.minQty" :min="0" class="full" />
              </a-form-item>
            </a-col>
          </template>
          <template v-else-if="form.promoType === 'REDUCE'">
            <a-col :span="12">
              <a-form-item label="满" required extra="适用物料的金额（折扣后）">
                <a-input-number v-model:value="form.thresholdAmount" :min="0" :precision="2" prefix="¥" class="full" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="减" required extra="按行金额分摊到单价">
                <a-input-number v-model:value="form.reduceAmount" :min="0" :precision="2" prefix="¥" class="full" />
              </a-form-item>
            </a-col>
          </template>
          <template v-else>
            <a-col :span="8">
              <a-form-item label="每购买" required>
                <a-input-number v-model:value="form.buyQty" :min="0" class="full" />
              </a-form-item>
            </a-col>
            <a-col :span="16">
              <a-form-item label="赠品" required>
                <MaterialSelect v-model:value="form.giftMaterialId" :label="form.giftLabel" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="赠送数量" required>
                <a-input-number v-model:value="form.giftQty" :min="0" class="full" />
              </a-form-item>
            </a-col>
          </template>
          <a-col :span="24">
            <a-form-item label="说明" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <a-alert type="info" show-icon message="手工新建、修改销售订单时按订单日期自动应用：每行取最低折扣，再取减免最多的一个满减，买赠按适用物料数量生成赠品行" />
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? `促销 ${detail.name}` : ''" width="min(900px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space v-if="userStore.hasPerm('scm:promotion:edit')">
          <a-popconfirm title="删除该促销？" @confirm="remove"><a-button danger>删除</a-button></a-popconfirm>
          <a-button @click="drawer.openEdit(detail)">编辑</a-button>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="类型"><DictTag type="scm_promo_type" :value="detail.promoType" /></a-descriptions-item>
          <a-descriptions-item label="期间">{{ detail.beginDate }} ~ {{ detail.endDate }}</a-descriptions-item>
          <a-descriptions-item label="状态">{{ detail.status ? '启用' : '停用' }}</a-descriptions-item>
          <a-descriptions-item label="规则">{{ ruleText(detail) }}</a-descriptions-item>
          <a-descriptions-item label="适用" :span="2">{{ scopeText(detail) }}，{{ detail.customerLevel ? `${findDict(dict.scm_customer_level, detail.customerLevel)?.label} 客户` : '全部客户' }}</a-descriptions-item>
          <a-descriptions-item label="应用">{{ detail.orderCount }} 单</a-descriptions-item>
          <a-descriptions-item label="优惠合计">¥{{ formatMoney(detail.discountTotal) }}</a-descriptions-item>
          <a-descriptions-item label="赠品合计">{{ formatQty(detail.giftTotal) }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="说明" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title">应用记录</div>
        <a-table row-key="orderId" size="small" :columns="usageColumns" :data-source="usages" :pagination="{ pageSize: 20 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'"><DictTag type="scm_order_status" :value="record.status" /></template>
            <template v-else-if="column.key === 'discount'">¥{{ formatMoney(record.discountAmount) }}</template>
            <template v-else-if="column.key === 'gift'">{{ Number(record.giftQty) ? formatQty(record.giftQty) : '-' }}</template>
          </template>
        </a-table>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { addPromotion, changePromotionStatus, deletePromotion, getPromotion, pagePromotion, promotionUsages, updatePromotion } from '@/api/scm/sourcing'
import { formatMoney } from '@/components/scm/scm'
import { formatQty, today } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { findDict, toOptions } from '@/utils/dict'

/** 促销活动：折扣、满减、买赠，手工新建 / 修改销售订单时按订单日期和客户等级自动应用 */
const dict = useDict('scm_promo_type', 'scm_customer_level', 'wms_material_category', 'scm_order_status')
const userStore = useUserStore()

const plain = (n) => (n == null ? '' : Number(n).toLocaleString('zh-CN', { maximumFractionDigits: 4 }))
function ruleText(p) {
  if (p.promoType === 'DISCOUNT') return `${Number(p.discountRate) / 10} 折${p.minQty ? `（满 ${plain(p.minQty)} 个）` : ''}`
  if (p.promoType === 'REDUCE') return `满 ¥${plain(p.thresholdAmount)} 减 ¥${plain(p.reduceAmount)}`
  return `每买 ${plain(p.buyQty)} 赠 ${p.giftMaterialName || ''} × ${plain(p.giftQty)}`
}
function scopeText(p) {
  if (p.materialId) return `${p.materialCode} ${p.materialName}`
  if (p.materialCategory) return `${findDict(dict.wms_material_category, p.materialCategory)?.label || p.materialCategory}类物料`
  return '全部物料'
}

const columns = [
  { title: '促销', key: 'name', width: 220 },
  { title: '类型', key: 'type', width: 90 },
  { title: '规则', key: 'rule', width: 220 },
  { title: '适用', key: 'scope', width: 220 },
  { title: '应用', key: 'usage', width: 140 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-promotion')
const activeOnly = ref(false)
const query = reactive({ keyword: undefined, promoType: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pagePromotion, query, {
  params: (q) => ({ ...q, activeOn: activeOnly.value ? today() : undefined })
})
onMounted(load)

const rules = {
  name: [{ required: true, whitespace: true, message: '请填写促销名称' }],
  promoType: [{ required: true, message: '请选择类型' }],
  beginDate: [{ required: true, message: '请选择开始日期' }],
  endDate: [{ required: true, message: '请选择结束日期' }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', promoType: 'DISCOUNT', beginDate: today(), endDate: undefined, customerLevel: undefined,
    materialId: undefined, materialCategory: undefined, minQty: undefined, discountRate: 90, thresholdAmount: undefined, reduceAmount: undefined,
    buyQty: undefined, giftMaterialId: undefined, giftQty: 1, remark: '' }),
  fetch: async (record) => {
    const p = await getPromotion(record.id)
    const n = (v) => (v == null ? undefined : Number(v))
    return { ...p, minQty: n(p.minQty), discountRate: n(p.discountRate), thresholdAmount: n(p.thresholdAmount), reduceAmount: n(p.reduceAmount),
      buyQty: n(p.buyQty), giftQty: n(p.giftQty),
      materialLabel: p.materialId ? `${p.materialCode} ${p.materialName}` : undefined,
      giftLabel: p.giftMaterialId ? `${p.giftMaterialCode} ${p.giftMaterialName}` : undefined }
  },
  create: addPromotion,
  update: updatePromotion,
  toPayload: (f) => ({ name: f.name, promoType: f.promoType, beginDate: f.beginDate, endDate: f.endDate, customerLevel: f.customerLevel ?? null,
    materialId: f.materialId ?? null, materialCategory: f.materialId ? null : f.materialCategory ?? null, minQty: f.minQty ?? null,
    discountRate: f.discountRate ?? null, thresholdAmount: f.thresholdAmount ?? null, reduceAmount: f.reduceAmount ?? null,
    buyQty: f.buyQty ?? null, giftMaterialId: f.giftMaterialId ?? null, giftQty: f.giftQty ?? null, remark: f.remark || null }),
  onSuccess: () => {
    load()
    if (detailOpen.value) openDetail(detail.value.id)
  }
})
const { formRef, form } = drawer

async function toggle(record) {
  await changePromotionStatus(record.id, record.status ? 0 : 1)
  message.success(record.status ? '已停用' : '已启用')
  load()
}

const usageColumns = [
  { title: '订单号', dataIndex: 'orderNo', width: 170 },
  { title: '客户', dataIndex: 'customerName', ellipsis: true },
  { title: '订单日期', dataIndex: 'orderDate', width: 110 },
  { title: '状态', key: 'status', width: 100 },
  { title: '优惠', key: 'discount', width: 110, align: 'right' },
  { title: '赠品', key: 'gift', width: 90, align: 'right' }
]
const detailOpen = ref(false)
const detail = ref()
const usages = ref([])
async function openDetail(id) {
  const [p, u] = await Promise.all([getPromotion(id), promotionUsages(id)])
  detail.value = p
  usages.value = u
  detailOpen.value = true
}
async function remove() {
  await deletePromotion(detail.value.id)
  message.success('已删除')
  detailOpen.value = false
  load()
}
</script>

<style scoped>
.filter {
  width: 120px;
}
.filter-wide {
  width: 200px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
