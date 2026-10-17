<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" :placeholder="$t('mes.doc.allStatus')" allow-clear :options="toOptions(dict.qms_ncr_status)" class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('qms.ncr.keywordPh')" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'qms:ncr:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('qms.ncr.add') }}
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
        :scroll="{ x: 1110, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'ncrNo'">
            <a @click="openDetail(record.id)">{{ record.ncrNo }}</a>
            <div class="sub-text"><DictTag type="qms_ncr_source" :value="record.source" /></div>
          </template>
          <template v-else-if="column.key === 'source'">
            <div>{{ record.sourceNo || '-' }}</div>
            <div class="sub-text">{{ record.supplierName || (record.woNo ? $t('mes.wo.detailTitle', { no: record.woNo }) : '') }}</div>
          </template>
          <template v-else-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ formatQty(record.qty) }} {{ record.unit }}{{ record.batchNo ? ' · ' + $t('mes.inspection.batchNo', { no: record.batchNo }) : '' }}</div>
          </template>
          <template v-else-if="column.key === 'description'">
            <DictTag v-if="record.defectType" type="mes_defect_type" :value="record.defectType" />
            <span class="desc">{{ record.description }}</span>
          </template>
          <template v-else-if="column.key === 'disposition'">
            <template v-if="record.disposition">
              <DictTag type="qms_disposition" :value="record.disposition" />
              <div class="sub-text"><DictTag type="qms_responsibility" :value="record.responsibility" /></div>
            </template>
            <span v-else class="sub-text">{{ $t('qms.ncr.pendingReview') }}</span>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="qms_ncr_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
              <a-button v-if="record.status === 'OPEN'" v-perm="'qms:ncr:edit'" type="link" size="small" @click="openReview(record)">{{ $t('qms.ncr.review') }}</a-button>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 手工登记 -->
    <FormDrawer :drawer="drawer" :create-title="$t('qms.ncr.addTitle')" :edit-title="$t('qms.ncr.addTitle')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('qms.capa.source')" name="source">
          <a-radio-group v-model:value="form.source" :options="sourceOptions" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="16">
            <a-form-item :label="$t('mes.wo.material')" name="materialId">
              <MaterialSelect v-model:value="form.materialId" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="$t('common.field.quantity')" name="qty">
              <a-input-number v-model:value="form.qty" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.inspection.woNo')" name="woNo" :extra="$t('qms.ncr.woExtra')">
              <a-input v-model:value="form.woNo" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.woDocForm.batchNo')" name="batchNo">
              <a-input v-model:value="form.batchNo" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.mrp.supplier')" name="supplierId" :extra="$t('qms.ncr.supplierExtra')">
              <PartnerSelect v-model:value="form.supplierId" type="supplier" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('qms.ncr.defectType')" name="defectType">
              <a-select v-model:value="form.defectType" allow-clear :options="toOptions(dict.mes_defect_type)" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('qms.ncr.description')" name="description">
          <a-textarea v-model:value="form.description" :rows="3" :maxlength="500" show-count />
        </a-form-item>
      </a-form>
    </FormDrawer>

    <!-- 评审处置 -->
    <a-modal v-model:open="reviewOpen" :title="$t('qms.ncr.reviewTitle', { no: current?.ncrNo || '' })" :confirm-loading="saving" width="600px" @ok="submitReview">
      <a-alert type="info" show-icon class="block" :message="DISPOSITION_TIPS[review.disposition] || $t('qms.ncr.reviewTip')" />
      <a-form layout="vertical">
        <a-form-item :label="$t('qms.ncr.disposition')" required>
          <a-radio-group v-model:value="review.disposition">
            <a-radio-button v-for="d in dispositionOptions" :key="d.value" :value="d.value" :disabled="d.disabled">{{ d.label }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="review.disposition === 'REWORK'" :label="$t('qms.ncr.reworkWarehouse')" required>
          <WarehouseSelect v-model:value="review.reworkWarehouseId" />
        </a-form-item>
        <a-form-item :label="$t('qms.ncr.responsibility')" required>
          <a-radio-group v-model:value="review.responsibility" :options="toOptions(dict.qms_responsibility)" />
        </a-form-item>
        <a-form-item :label="$t('qms.ncr.opinion')">
          <a-textarea v-model:value="review.reviewOpinion" :rows="3" :maxlength="500" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? $t('qms.ncr.detailTitle', { no: detail.ncrNo }) : ''" width="min(900px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space>
          <a-button v-if="detail.flowInstanceId" @click="flowOpen = true">{{ $t('mes.ecn.flowRecord') }}</a-button>
          <a-button v-if="detail.status === 'DONE'" v-perm="'qms:capa:add'" @click="router.push({ path: '/qms/capa', query: { ncrId: detail.id } })">{{ $t('qms.ncr.start8d') }}</a-button>
          <a-button v-if="detail.status === 'DONE' && detail.supplierId" v-perm="'qms:claim:add'" @click="router.push({ path: '/qms/claim', query: { ncrId: detail.id } })">
            {{ $t('qms.ncr.claim') }}
          </a-button>
          <template v-if="detail.status === 'OPEN'">
            <a-button v-perm="'qms:ncr:edit'" type="primary" @click="openReview(detail)">{{ $t('qms.ncr.review') }}</a-button>
            <span v-perm="'qms:ncr:edit'">
              <a-popconfirm v-if="detail.source === 'INSPECTION'" :title="$t('qms.ncr.cancelConfirm')" @confirm="cancel">
                <a-button>{{ $t('common.action.cancel') }}</a-button>
              </a-popconfirm>
            </span>
          </template>
          <span v-if="detail.source !== 'INSPECTION' && ['OPEN', 'CANCELLED'].includes(detail.status)" v-perm="'qms:ncr:remove'">
            <a-popconfirm :title="$t('qms.ncr.deleteConfirm')" @confirm="remove">
              <a-button danger>{{ $t('common.action.delete') }}</a-button>
            </a-popconfirm>
          </span>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('qms.capa.source')"><DictTag type="qms_ncr_source" :value="detail.source" /> {{ detail.inspNo ? $t('mes.inspection.detailTitle', { no: detail.inspNo }) : '' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="qms_ncr_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('mes.wo.material')">{{ detail.materialCode }} {{ detail.materialName }} {{ detail.spec || '' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.quantity')">{{ formatQty(detail.qty) }} {{ detail.unit }}{{ detail.batchNo ? ' · ' + $t('mes.inspection.batchNo', { no: detail.batchNo }) : '' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('qms.ncr.sourceDoc')">{{ detail.sourceNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('qms.ncr.supplierWo')">{{ detail.supplierName || detail.woNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('qms.ncr.description')" :span="2">
            <DictTag v-if="detail.defectType" type="mes_defect_type" :value="detail.defectType" /> {{ detail.description }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('qms.capa.registered')">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item :label="$t('qms.ncr.doneTime')">{{ detail.doneTime || '-' }}</a-descriptions-item>
        </a-descriptions>
        <template v-if="detail.disposition">
          <div class="section-title">{{ $t('qms.ncr.review') }}</div>
          <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" bordered>
            <a-descriptions-item :label="$t('qms.ncr.disposition')"><DictTag type="qms_disposition" :value="detail.disposition" /></a-descriptions-item>
            <a-descriptions-item :label="$t('qms.ncr.responsibility')"><DictTag type="qms_responsibility" :value="detail.responsibility" /></a-descriptions-item>
            <a-descriptions-item :label="$t('qms.ncr.opinion')" :span="2">{{ detail.reviewOpinion || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('qms.ncr.result')" :span="2">
              {{ detail.resultNote || (detail.status === 'APPROVING' ? $t('qms.ncr.approving') : '-') }}
              <span v-if="detail.reworkWoNo">{{ $t('qms.ncr.reworkWo', { no: detail.reworkWoNo }) }}</span>
            </a-descriptions-item>
          </a-descriptions>
        </template>
      </template>
    </SubPage>

    <InstanceDetailDrawer v-model:open="flowOpen" :instance-id="detail?.flowInstanceId" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import InstanceDetailDrawer from '@/components/workflow/InstanceDetailDrawer.vue'
import { addNcr, cancelNcr, deleteNcr, getNcr, pageNcr, reviewNcr } from '@/api/qms'
import { formatQty } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

/** 不合格品处理（MRB）：检验不合格自动生成，也可手工登记；评审处置后联动库存与工单 */
const dict = useDict('qms_ncr_source', 'qms_ncr_status', 'qms_disposition', 'qms_responsibility', 'mes_defect_type')
const { t } = useI18n()
const router = useRouter()

const DISPOSITION_TIPS = {
  RETURN: t('qms.ncr.tips.RETURN'),
  REWORK: t('qms.ncr.tips.REWORK'),
  SCRAP: t('qms.ncr.tips.SCRAP'),
  CONCESSION: t('qms.ncr.tips.CONCESSION'),
  SORT: t('qms.ncr.tips.SORT')
}

const columns = [
  { title: t('qms.ncr.no'), key: 'ncrNo', width: 170 },
  { title: t('qms.ncr.sourceDoc'), key: 'source', width: 180 },
  { title: t('mes.wo.material'), key: 'material', width: 230 },
  { title: t('qms.ncr.description'), key: 'description', width: 200, ellipsis: true },
  { title: t('qms.ncr.dispResp'), key: 'disposition', width: 150 },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('common.action.operation'), key: 'action', width: 170, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'qms-ncr')
const query = reactive({ status: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageNcr, query)
onMounted(load)

// 手工登记不提供「检验不合格」来源
const sourceOptions = computed(() => toOptions(dict.qms_ncr_source).filter((o) => o.value !== 'INSPECTION'))
const rules = {
  source: [{ required: true, message: t('qms.capa.sourceRequired') }],
  materialId: [{ required: true, message: t('mes.supplementForm.materialRequired') }],
  qty: [{ required: true, message: t('mes.wo.qtyRequired') }],
  description: [{ required: true, whitespace: true, message: t('qms.ncr.descriptionRequired') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ source: 'PRODUCTION', materialId: undefined, qty: undefined, woNo: '', batchNo: '', supplierId: undefined, defectType: undefined, description: '' }),
  create: addNcr,
  update: () => Promise.resolve(),
  toPayload: (f) => ({ ...f, woNo: f.woNo || null, batchNo: f.batchNo || null }),
  onSuccess: load
})
const { formRef, form } = drawer

// ---------------------------------------------------------------- 评审
const reviewOpen = ref(false)
const current = ref()
const review = ref({})
const saving = ref(false)
/** 来料（有供应商、无工单）不能返工；非来料不能退货 */
const dispositionOptions = computed(() => {
  const c = current.value
  const incoming = !!c?.supplierId && !!c?.sourceNo && !c?.woId
  return toOptions(dict.qms_disposition).map((o) => ({
    ...o,
    disabled: (o.value === 'REWORK' && !c?.woId) || (o.value === 'RETURN' && !incoming)
  }))
})
function openReview(record) {
  current.value = record
  review.value = { disposition: undefined, responsibility: record.supplierId && !record.woId ? 'SUPPLIER' : undefined, reviewOpinion: '', reworkWarehouseId: undefined }
  reviewOpen.value = true
}
async function submitReview() {
  if (!review.value.disposition || !review.value.responsibility) return message.warning(t('qms.ncr.reviewRequired'))
  saving.value = true
  try {
    const approving = await reviewNcr(current.value.id, review.value)
    message.success(approving ? t('qms.ncr.submittedApproval') : t('qms.ncr.executed'))
    reviewOpen.value = false
    load()
    if (detailOpen.value) refreshDetail()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 详情
const detailOpen = ref(false)
const detail = ref()
const flowOpen = ref(false)
async function openDetail(id) {
  detail.value = await getNcr(id)
  detailOpen.value = true
}
async function refreshDetail() {
  if (detail.value) detail.value = await getNcr(detail.value.id)
}
async function cancel() {
  await cancelNcr(detail.value.id)
  message.success(t('mes.ecn.cancelled'))
  refreshDetail()
  load()
}
async function remove() {
  await deleteNcr(detail.value.id)
  message.success(t('common.msg.deleted'))
  detailOpen.value = false
  load()
}
</script>

<style scoped>
.filter {
  width: 140px;
}
.filter-wide {
  width: 260px;
}
.block {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.desc {
  margin-left: 4px;
}
.section-title {
  margin: 8px 0;
  font-weight: 600;
}
</style>
