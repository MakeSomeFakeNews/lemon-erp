<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-input-search v-model:value="query.keyword" :placeholder="$t('scm.promotion.name')" allow-clear class="filter-wide" @search="search" />
        <a-select v-model:value="query.promoType" :placeholder="$t('common.field.type')" allow-clear :options="toOptions(dict.scm_promo_type)" class="filter" @change="search" />
        <a-checkbox v-model:checked="activeOnly" @change="search">{{ $t('scm.promotion.activeOnly') }}</a-checkbox>
        <template #extra>
          <a-button v-perm="'scm:promotion:edit'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('scm.promotion.add') }}
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
            <div class="sub-text">{{ record.customerLevel ? $t('scm.promotion.levelCustomer', { level: findDict(dict.scm_customer_level, record.customerLevel)?.label || record.customerLevel }) : $t('scm.promotion.allCustomers') }}</div>
          </template>
          <template v-else-if="column.key === 'usage'">
            {{ $t('scm.promotion.orderCount', { n: record.orderCount }) }}
            <div class="sub-text">
              <template v-if="record.promoType === 'GIFT'">{{ $t('scm.promotion.giftTotalText', { qty: formatQty(record.giftTotal) }) }}</template>
              <template v-else>{{ $t('scm.promotion.discountTotalText', { amount: formatMoney(record.discountTotal) }) }}</template>
            </div>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag v-if="!record.status">{{ $t('scm.promotion.stDisabled') }}</a-tag>
            <a-tag v-else-if="record.endDate < today()">{{ $t('scm.promotion.stEnded') }}</a-tag>
            <a-tag v-else-if="record.beginDate > today()" color="blue">{{ $t('scm.promotion.stNotStarted') }}</a-tag>
            <a-tag v-else color="green">{{ $t('scm.promotion.stActive') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
              <template v-if="userStore.hasPerm('scm:promotion:edit')">
                <a-button type="link" size="small" @click="drawer.openEdit(record)">{{ $t('common.action.edit') }}</a-button>
                <a-button type="link" size="small" @click="toggle(record)">{{ record.status ? $t('common.action.disable') : $t('common.action.enable') }}</a-button>
              </template>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('scm.promotion.add')" :edit-title="$t('scm.promotion.edit')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('scm.promotion.name')" name="name">
          <a-input v-model:value="form.name" :maxlength="64" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item :label="$t('common.field.type')" name="promoType">
              <a-radio-group v-model:value="form.promoType" button-style="solid">
                <a-radio-button v-for="o in dict.scm_promo_type" :key="o.value" :value="o.value">{{ o.label }}</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.beginDate')" name="beginDate">
              <a-date-picker v-model:value="form.beginDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.endDate')" name="endDate">
              <a-date-picker v-model:value="form.endDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('scm.promotion.customerLevel')" name="customerLevel" :extra="$t('scm.promotion.customerLevelExtra')">
              <a-select v-model:value="form.customerLevel" allow-clear :placeholder="$t('scm.promotion.allCustomers')" :options="toOptions(dict.scm_customer_level)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('scm.promotion.materialCategory')" name="materialCategory" :extra="$t('scm.promotion.materialCategoryExtra')">
              <a-select v-model:value="form.materialCategory" allow-clear :placeholder="$t('scm.promotion.allCategories')" :disabled="!!form.materialId"
                :options="toOptions(dict.wms_material_category)" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('scm.promotion.material')" name="materialId">
              <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" :placeholder="$t('scm.promotion.materialPlaceholder')" />
            </a-form-item>
          </a-col>
          <template v-if="form.promoType === 'DISCOUNT'">
            <a-col :span="12">
              <a-form-item :label="$t('scm.promotion.discountRate')" required :extra="$t('scm.promotion.discountRateExtra')">
                <a-input-number v-model:value="form.discountRate" :min="1" :max="99.99" :precision="2" addon-after="%" class="full" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item :label="$t('scm.promotion.minQty')" :extra="$t('scm.promotion.minQtyExtra')">
                <a-input-number v-model:value="form.minQty" :min="0" class="full" />
              </a-form-item>
            </a-col>
          </template>
          <template v-else-if="form.promoType === 'REDUCE'">
            <a-col :span="12">
              <a-form-item :label="$t('scm.promotion.threshold')" required :extra="$t('scm.promotion.thresholdExtra')">
                <a-input-number v-model:value="form.thresholdAmount" :min="0" :precision="2" prefix="¥" class="full" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item :label="$t('scm.promotion.reduce')" required :extra="$t('scm.promotion.reduceExtra')">
                <a-input-number v-model:value="form.reduceAmount" :min="0" :precision="2" prefix="¥" class="full" />
              </a-form-item>
            </a-col>
          </template>
          <template v-else>
            <a-col :span="8">
              <a-form-item :label="$t('scm.promotion.buyQty')" required>
                <a-input-number v-model:value="form.buyQty" :min="0" class="full" />
              </a-form-item>
            </a-col>
            <a-col :span="16">
              <a-form-item :label="$t('scm.promotion.gift')" required>
                <MaterialSelect v-model:value="form.giftMaterialId" :label="form.giftLabel" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item :label="$t('scm.promotion.giftQty')" required>
                <a-input-number v-model:value="form.giftQty" :min="0" class="full" />
              </a-form-item>
            </a-col>
          </template>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.description')" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <a-alert type="info" show-icon :message="$t('scm.promotion.applyTip')" />
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? $t('scm.promotion.detailTitle', { name: detail.name }) : ''" width="min(900px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space v-if="userStore.hasPerm('scm:promotion:edit')">
          <a-popconfirm :title="$t('scm.promotion.confirmDelete')" @confirm="remove"><a-button danger>{{ $t('common.action.delete') }}</a-button></a-popconfirm>
          <a-button @click="drawer.openEdit(detail)">{{ $t('common.action.edit') }}</a-button>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('common.field.type')"><DictTag type="scm_promo_type" :value="detail.promoType" /></a-descriptions-item>
          <a-descriptions-item :label="$t('scm.promotion.period')">{{ detail.beginDate }} ~ {{ detail.endDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')">{{ detail.status ? $t('common.status.enabled') : $t('common.status.disabled') }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.promotion.rule')">{{ ruleText(detail) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.promotion.scope')" :span="2">{{ $t('scm.promotion.scopeJoin', { scope: scopeText(detail), customers: detail.customerLevel ? $t('scm.promotion.levelCustomer', { level: findDict(dict.scm_customer_level, detail.customerLevel)?.label }) : $t('scm.promotion.allCustomers') }) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.promotion.usage')">{{ $t('scm.promotion.orderCount', { n: detail.orderCount }) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.promotion.discountTotal')">¥{{ formatMoney(detail.discountTotal) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.promotion.giftTotal')">{{ formatQty(detail.giftTotal) }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.description')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title">{{ $t('scm.promotion.usageRecords') }}</div>
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
import { useI18n } from 'vue-i18n'
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
const { t } = useI18n()

const plain = (n) => (n == null ? '' : Number(n).toLocaleString('zh-CN', { maximumFractionDigits: 4 }))
function ruleText(p) {
  if (p.promoType === 'DISCOUNT') {
    const rate = t('scm.promotion.ruleDiscount', { rate: Number(p.discountRate) / 10, off: plain(100 - Number(p.discountRate)) })
    return p.minQty ? t('scm.promotion.ruleDiscountMin', { rule: rate, n: plain(p.minQty) }) : rate
  }
  if (p.promoType === 'REDUCE') return t('scm.promotion.ruleReduce', { threshold: plain(p.thresholdAmount), reduce: plain(p.reduceAmount) })
  return t('scm.promotion.ruleGift', { buy: plain(p.buyQty), gift: p.giftMaterialName || '', qty: plain(p.giftQty) })
}
function scopeText(p) {
  if (p.materialId) return `${p.materialCode} ${p.materialName}`
  if (p.materialCategory) return t('scm.promotion.categoryMaterials', { category: findDict(dict.wms_material_category, p.materialCategory)?.label || p.materialCategory })
  return t('scm.promotion.allMaterials')
}

const columns = [
  { title: t('scm.promotion.promotion'), key: 'name', width: 220 },
  { title: t('common.field.type'), key: 'type', width: 100 },
  { title: t('scm.promotion.rule'), key: 'rule', width: 220 },
  { title: t('scm.promotion.scope'), key: 'scope', width: 220 },
  { title: t('scm.promotion.usage'), key: 'usage', width: 150 },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('common.action.operation'), key: 'action', width: 180, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-promotion')
const activeOnly = ref(false)
const query = reactive({ keyword: undefined, promoType: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pagePromotion, query, {
  params: (q) => ({ ...q, activeOn: activeOnly.value ? today() : undefined })
})
onMounted(load)

const rules = {
  name: [{ required: true, whitespace: true, message: t('scm.promotion.ruleName') }],
  promoType: [{ required: true, message: t('common.rule.requiredSelect', { name: t('common.field.type') }) }],
  beginDate: [{ required: true, message: t('common.rule.requiredSelect', { name: t('common.field.beginDate') }) }],
  endDate: [{ required: true, message: t('common.rule.requiredSelect', { name: t('common.field.endDate') }) }]
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
  message.success(record.status ? t('scm.promotion.disabledMsg') : t('scm.promotion.enabledMsg'))
  load()
}

const usageColumns = [
  { title: t('scm.promotion.orderNo'), dataIndex: 'orderNo', width: 170 },
  { title: t('scm.promotion.customer'), dataIndex: 'customerName', ellipsis: true },
  { title: t('scm.promotion.orderDate'), dataIndex: 'orderDate', width: 110 },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('scm.promotion.discount'), key: 'discount', width: 110, align: 'right' },
  { title: t('scm.promotion.gift'), key: 'gift', width: 90, align: 'right' }
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
  message.success(t('common.msg.deleted'))
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
