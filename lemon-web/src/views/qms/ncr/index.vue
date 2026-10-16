<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" placeholder="全部状态" allow-clear :options="toOptions(dict.qms_ncr_status)" class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" placeholder="单号、来源单号、物料、供应商" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'qms:ncr:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>登记不合格
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
            <div class="sub-text">{{ record.supplierName || (record.woNo ? '工单 ' + record.woNo : '') }}</div>
          </template>
          <template v-else-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ formatQty(record.qty) }} {{ record.unit }}{{ record.batchNo ? ' · 批次 ' + record.batchNo : '' }}</div>
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
            <span v-else class="sub-text">待评审</span>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="qms_ncr_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record.id)">查看</a-button>
              <a-button v-if="record.status === 'OPEN'" v-perm="'qms:ncr:edit'" type="link" size="small" @click="openReview(record)">评审处置</a-button>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 手工登记 -->
    <FormDrawer :drawer="drawer" create-title="登记不合格品" edit-title="登记不合格品">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="来源" name="source">
          <a-radio-group v-model:value="form.source" :options="sourceOptions" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="16">
            <a-form-item label="物料" name="materialId">
              <MaterialSelect v-model:value="form.materialId" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="数量" name="qty">
              <a-input-number v-model:value="form.qty" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="生产工单号" name="woNo" extra="返工处置需要关联工单">
              <a-input v-model:value="form.woNo" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="批次号" name="batchNo">
              <a-input v-model:value="form.batchNo" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="供应商" name="supplierId" extra="供应商原因时填写，便于索赔">
              <PartnerSelect v-model:value="form.supplierId" type="supplier" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="不良类型" name="defectType">
              <a-select v-model:value="form.defectType" allow-clear :options="toOptions(dict.mes_defect_type)" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="不合格描述" name="description">
          <a-textarea v-model:value="form.description" :rows="3" :maxlength="500" show-count />
        </a-form-item>
      </a-form>
    </FormDrawer>

    <!-- 评审处置 -->
    <a-modal v-model:open="reviewOpen" :title="`评审处置 - ${current?.ncrNo || ''}`" :confirm-loading="saving" width="560px" @ok="submitReview">
      <a-alert type="info" show-icon class="block" :message="DISPOSITION_TIPS[review.disposition] || '选择处置方式，启用「不合格品处置审批」时审批通过后执行'" />
      <a-form layout="vertical">
        <a-form-item label="处置方式" required>
          <a-radio-group v-model:value="review.disposition">
            <a-radio-button v-for="d in dispositionOptions" :key="d.value" :value="d.value" :disabled="d.disabled">{{ d.label }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="review.disposition === 'REWORK'" label="返工完工入库仓库" required>
          <WarehouseSelect v-model:value="review.reworkWarehouseId" />
        </a-form-item>
        <a-form-item label="责任方" required>
          <a-radio-group v-model:value="review.responsibility" :options="toOptions(dict.qms_responsibility)" />
        </a-form-item>
        <a-form-item label="评审意见">
          <a-textarea v-model:value="review.reviewOpinion" :rows="3" :maxlength="500" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? `不合格品处理 ${detail.ncrNo}` : ''" width="min(900px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space>
          <a-button v-if="detail.flowInstanceId" @click="flowOpen = true">审批记录</a-button>
          <a-button v-if="detail.status === 'DONE'" v-perm="'qms:capa:add'" @click="router.push({ path: '/qms/capa', query: { ncrId: detail.id } })">发起 8D</a-button>
          <a-button v-if="detail.status === 'DONE' && detail.supplierId" v-perm="'qms:claim:add'" @click="router.push({ path: '/qms/claim', query: { ncrId: detail.id } })">
            供应商索赔
          </a-button>
          <template v-if="detail.status === 'OPEN'">
            <a-button v-perm="'qms:ncr:edit'" type="primary" @click="openReview(detail)">评审处置</a-button>
            <span v-perm="'qms:ncr:edit'">
              <a-popconfirm v-if="detail.source === 'INSPECTION'" title="取消该处理单？检验误判时使用" @confirm="cancel">
                <a-button>取消</a-button>
              </a-popconfirm>
            </span>
          </template>
          <span v-if="detail.source !== 'INSPECTION' && ['OPEN', 'CANCELLED'].includes(detail.status)" v-perm="'qms:ncr:remove'">
            <a-popconfirm title="删除该处理单？" @confirm="remove">
              <a-button danger>删除</a-button>
            </a-popconfirm>
          </span>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" bordered class="block">
          <a-descriptions-item label="来源"><DictTag type="qms_ncr_source" :value="detail.source" /> {{ detail.inspNo ? '检验单 ' + detail.inspNo : '' }}</a-descriptions-item>
          <a-descriptions-item label="状态"><DictTag type="qms_ncr_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item label="物料">{{ detail.materialCode }} {{ detail.materialName }} {{ detail.spec || '' }}</a-descriptions-item>
          <a-descriptions-item label="数量">{{ formatQty(detail.qty) }} {{ detail.unit }}{{ detail.batchNo ? ' · 批次 ' + detail.batchNo : '' }}</a-descriptions-item>
          <a-descriptions-item label="来源单据">{{ detail.sourceNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="供应商 / 工单">{{ detail.supplierName || detail.woNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="不合格描述" :span="2">
            <DictTag v-if="detail.defectType" type="mes_defect_type" :value="detail.defectType" /> {{ detail.description }}
          </a-descriptions-item>
          <a-descriptions-item label="登记">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item label="处置完成">{{ detail.doneTime || '-' }}</a-descriptions-item>
        </a-descriptions>
        <template v-if="detail.disposition">
          <div class="section-title">评审处置</div>
          <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" bordered>
            <a-descriptions-item label="处置方式"><DictTag type="qms_disposition" :value="detail.disposition" /></a-descriptions-item>
            <a-descriptions-item label="责任方"><DictTag type="qms_responsibility" :value="detail.responsibility" /></a-descriptions-item>
            <a-descriptions-item label="评审意见" :span="2">{{ detail.reviewOpinion || '-' }}</a-descriptions-item>
            <a-descriptions-item label="处置结果" :span="2">
              {{ detail.resultNote || (detail.status === 'APPROVING' ? '审批中，通过后执行' : '-') }}
              <span v-if="detail.reworkWoNo">（返工工单 {{ detail.reworkWoNo }}）</span>
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
const router = useRouter()

const DISPOSITION_TIPS = {
  RETURN: '退货：来料保持冻结，通知采购办理采购退货',
  REWORK: '返工：按工单产品生成返工工单（草稿），返工完成后入库',
  SCRAP: '报废：来料放行冻结库存并生成报废出库单（草稿，仓库确认后出库）',
  CONCESSION: '让步接收：来料放行冻结库存，可正常使用',
  SORT: '挑选：全检挑出不良品后，合格品按让步或重新检验处理'
}

const columns = [
  { title: '处理单号', key: 'ncrNo', width: 170 },
  { title: '来源单据', key: 'source', width: 180 },
  { title: '物料', key: 'material', width: 230 },
  { title: '不合格描述', key: 'description', width: 200, ellipsis: true },
  { title: '处置 / 责任', key: 'disposition', width: 120 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'qms-ncr')
const query = reactive({ status: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageNcr, query)
onMounted(load)

// 手工登记不提供「检验不合格」来源
const sourceOptions = computed(() => toOptions(dict.qms_ncr_source).filter((o) => o.value !== 'INSPECTION'))
const rules = {
  source: [{ required: true, message: '请选择来源' }],
  materialId: [{ required: true, message: '请选择物料' }],
  qty: [{ required: true, message: '请填写数量' }],
  description: [{ required: true, whitespace: true, message: '请填写不合格描述' }]
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
  if (!review.value.disposition || !review.value.responsibility) return message.warning('请选择处置方式和责任方')
  saving.value = true
  try {
    const approving = await reviewNcr(current.value.id, review.value)
    message.success(approving ? '已提交审批，通过后执行处置' : '处置已执行')
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
  message.success('已取消')
  refreshDetail()
  load()
}
async function remove() {
  await deleteNcr(detail.value.id)
  message.success('已删除')
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
