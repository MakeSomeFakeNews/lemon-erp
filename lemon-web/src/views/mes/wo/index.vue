<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="工单号" name="woNo">
          <a-input v-model:value="query.woNo" placeholder="请输入工单号" allow-clear />
        </a-form-item>
        <a-form-item label="产品" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="产品编码或名称" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.mes_wo_status)" />
        </a-form-item>
        <a-form-item label="计划开工" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" style="width: 100%" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll"
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'mes:wo:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增工单
        </a-button>
        <a-button v-perm="'mes:wo:add'" @click="openRework()">
          <template #icon><RedoOutlined /></template>新建返工工单
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1350, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'woNo'">
            <a @click="openDetail(record.id)">{{ record.woNo }}</a>
            <a-tag v-if="record.source === 'MRP'" color="purple" :bordered="false" class="source-tag">MRP</a-tag>
            <a-tag v-else-if="record.source === 'KIT'" color="orange" :bordered="false" class="source-tag">缺料</a-tag>
            <a-tag v-else-if="record.source === 'PLAN'" color="blue" :bordered="false" class="source-tag">计划</a-tag>
            <a-tag v-else-if="record.source === 'SPLIT'" color="cyan" :bordered="false" class="source-tag">拆分</a-tag>
            <a-tag v-if="record.woType === 'REWORK'" color="orange" :bordered="false" class="source-tag">返工</a-tag>
          </template>
          <template v-else-if="column.key === 'product'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">BOM {{ record.bomVersion }}</div>
          </template>
          <template v-else-if="column.key === 'progress'">
            <a-progress
              :percent="pct(record.finishedQty, record.qty)"
              size="small"
              :status="pct(record.finishedQty, record.qty) >= 100 ? 'success' : 'normal'"
              :format="() => `${formatQty(record.finishedQty)} / ${formatQty(record.qty)}`"
            />
          </template>
          <template v-else-if="column.key === 'plan'">
            <div>{{ record.planStart || '-' }}</div>
            <div class="sub-text">至 {{ record.planEnd || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="mes_wo_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record.id)">
                <template #icon><EyeOutlined /></template>查看
              </a-button>
              <template v-if="record.status === 'DRAFT'">
                <a-button v-perm="'mes:wo:edit'" type="link" size="small" @click="drawer.openEdit(record)">编辑</a-button>
                <a-button v-perm="'mes:wo:release'" type="link" size="small" @click="release(record)">下达</a-button>
              </template>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增 / 编辑 -->
    <FormDrawer :drawer="drawer" create-title="新增工单" edit-title="编辑工单">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="产品" name="materialId">
          <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" @select="onProductSelect" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="BOM" name="bomId">
              <a-select v-model:value="form.bomId" :options="bomOptions" placeholder="默认 BOM" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="生产数量" name="qty">
              <a-input-number v-model:value="form.qty" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="计划开工" name="planStart">
              <a-date-picker v-model:value="form.planStart" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="计划完工" name="planEnd">
              <a-date-picker v-model:value="form.planEnd" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="完工入库仓库" name="warehouseId">
              <WarehouseSelect v-model:value="form.warehouseId" placeholder="选填，完工入库时默认" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <!-- 详情：用料、工序、单据、报工 -->
    <SubPage
      :open="detailOpen"
      :title="detail ? `工单 ${detail.woNo}` : '工单'"
      width="min(1100px, 100vw)"
      destroy-on-close
      @close="detailOpen = false"
    >
      <template #extra>
        <a-space>
          <a-button v-if="detail" size="small" @click="print"><template #icon><PrinterOutlined /></template>打印</a-button>
          <DictTag v-if="detail?.woType === 'REWORK'" type="mes_wo_type" :value="detail.woType" />
          <DictTag v-if="detail" type="mes_wo_status" :value="detail.status" />
        </a-space>
      </template>
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
            <a-descriptions-item label="产品">{{ detail.materialCode }} {{ detail.materialName }}</a-descriptions-item>
            <a-descriptions-item label="BOM">{{ detail.bomVersion }}</a-descriptions-item>
            <a-descriptions-item label="完工 / 计划">{{ formatQty(detail.finishedQty) }} / {{ formatQty(detail.qty) }} {{ detail.unit }}</a-descriptions-item>
            <a-descriptions-item label="计划日期">{{ detail.planStart || '-' }} ~ {{ detail.planEnd || '-' }}</a-descriptions-item>
            <a-descriptions-item label="完工入库仓库">{{ detail.warehouseName || '-' }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.sourceWoId" :label="detail.woType === 'REWORK' ? '返工自' : '拆分自'">
              <a @click="openDetail(detail.sourceWoId)">{{ sourceWoNo || `工单 #${detail.sourceWoId}` }}</a>
            </a-descriptions-item>
            <a-descriptions-item v-if="scrapReported > 0" label="报工报废 / 已报废入库">
              {{ formatQty(scrapReported) }} / {{ formatQty(scrapIn) }} {{ detail.unit }}
            </a-descriptions-item>
            <a-descriptions-item label="制单">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.releaseTime" label="下达时间">{{ detail.releaseTime }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.finishTime" label="完工时间">{{ detail.finishTime }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.remark" label="备注">{{ detail.remark }}</a-descriptions-item>
          </a-descriptions>
          <a-tabs size="small">
            <a-tab-pane key="materials" tab="用料">
              <a-table row-key="id" size="small" :columns="materialColumns" :data-source="detail.materials" :pagination="false">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
                  <template v-else-if="column.key === 'unitQty'">{{ formatQty(record.unitQty) }}</template>
                  <template v-else-if="column.key === 'progress'">
                    <a-progress :percent="pct(record.issuedQty, Number(record.requiredQty) + Number(record.supplementQty || 0))" size="small" />
                    <div class="sub-text">
                      已领 {{ formatQty(record.issuedQty) }} / 需求 {{ formatQty(record.requiredQty) }} {{ record.unit }}
                      <template v-if="Number(record.supplementQty)">，含补料 {{ formatQty(record.supplementQty) }}</template>
                    </div>
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="ops" tab="工序">
              <a-table row-key="id" size="small" :columns="opColumns" :data-source="detail.ops" :pagination="false">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'name'">
                    {{ record.name }}
                    <a-tooltip v-if="record.firstArticle === 1" title="首件检验合格前只能报工 1 件，在「质量检验」中新增首件检验">
                      <a-tag color="purple" :bordered="false">首件</a-tag>
                    </a-tooltip>
                  </template>
                  <template v-else-if="column.key === 'progress'">
                    <a-progress :percent="pct(record.goodQty, detail.qty)" size="small" />
                    <div class="sub-text">合格 {{ formatQty(record.goodQty) }}，报废 {{ formatQty(record.scrapQty) }}</div>
                  </template>
                  <template v-else-if="column.key === 'hours'">
                    {{ formatQty(record.hours) }} / {{ formatQty(Number(record.stdHours) * Number(detail.qty)) }} 小时
                  </template>
                  <template v-else-if="column.key === 'plan'">
                    <template v-if="record.planStart">
                      <div>{{ record.planStart }}</div>
                      <div class="sub-text">至 {{ record.planEnd || '-' }}</div>
                    </template>
                    <span v-else class="sub-text">未排程</span>
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <a-button
                      v-if="['RELEASED', 'STARTED'].includes(detail.status)"
                      v-perm="'mes:wo:report'"
                      type="link"
                      size="small"
                      @click="openReport(record)"
                    >
                      报工
                    </a-button>
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="docs" tab="领料 / 完工记录">
              <a-table row-key="id" size="small" :columns="docColumns" :data-source="docs" :pagination="false">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'kind'"><DictTag type="mes_wo_doc_kind" :value="record.kind" /></template>
                  <template v-else-if="column.key === 'totalQty'">{{ formatQty(record.totalQty) }}</template>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="reports" tab="报工记录">
              <a-table row-key="id" size="small" :columns="reportColumns" :data-source="reports" :pagination="false">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'op'">{{ record.opSeq }} {{ record.opName }}</template>
                  <template v-else-if="['goodQty', 'scrapQty', 'hours'].includes(column.key)">{{ formatQty(record[column.key]) }}</template>
                  <template v-else-if="column.key === 'operator'">{{ record.operatorName || record.createByName }}</template>
                  <template v-else-if="column.key === 'pieceWage'">{{ record.pieceWage != null ? '¥' + formatQty(record.pieceWage) : '-' }}</template>
                  <template v-else-if="column.key === 'equipment'">
                    {{ record.equipmentName || '-' }}
                    <div v-if="record.moldCode" class="sub-text">模具 {{ record.moldCode }} × {{ record.shots }} 模次</div>
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="attachment" tab="附件">
              <AttachmentPanel biz-type="mes_wo" :biz-id="detail.id" :editable="userStore.hasPerm('mes:wo:edit')" />
            </a-tab-pane>
          </a-tabs>
        </template>
      </a-spin>
      <template v-if="detail && actions.length" #footer>
        <div class="drawer-footer">
          <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">
            {{ a.label }}
          </a-button>
        </div>
      </template>
    </SubPage>

    <WoDocForm v-model:open="docOpen" :kind="docKind" :wo="detail" :scrap-in="scrapIn" @success="refreshDetail" />

    <!-- 改量 -->
    <a-modal v-model:open="qtyOpen" :title="`改量 - 工单 ${detail?.woNo}`" :confirm-loading="saving" @ok="submitQty">
      <a-alert type="info" show-icon class="block" message="新数量不少于已完工、已报工数量，用料需求按新数量重算。" />
      <a-form layout="vertical">
        <a-form-item label="生产数量" required :extra="detail ? `当前 ${formatQty(detail.qty)}，已完工 ${formatQty(detail.finishedQty)} ${detail.unit || ''}` : ''">
          <a-input-number v-model:value="qtyValue" :min="0" :precision="4" :addon-after="detail?.unit || undefined" class="full" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 拆分 -->
    <a-modal v-model:open="splitOpen" :title="`拆分 - 工单 ${detail?.woNo}`" :confirm-loading="saving" @ok="submitSplit">
      <a-alert
        type="info"
        show-icon
        class="block"
        message="从本工单拆出部分数量生成新的草稿工单，本工单数量相应减少；拆分数量不超过未开工的数量，用料需求按数量重算。"
      />
      <a-form layout="vertical">
        <a-form-item label="拆分数量" required :extra="detail ? `工单数量 ${formatQty(detail.qty)} ${detail.unit || ''}` : ''">
          <a-input-number v-model:value="splitForm.qty" :min="0" :precision="4" :addon-after="detail?.unit || undefined" class="full" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="计划开工">
              <a-date-picker v-model:value="splitForm.planStart" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="计划完工">
              <a-date-picker v-model:value="splitForm.planEnd" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 返工工单 -->
    <a-modal v-model:open="reworkOpen" title="新建返工工单" width="640px" :confirm-loading="saving" @ok="submitRework">
      <a-alert
        type="info"
        show-icon
        class="block"
        message="返工工单的用料为产品本身（领出待返工的产品），工序取默认 BOM，完工后重新入库；保存为草稿，下达后才能领料、报工。"
      />
      <a-form ref="reworkFormRef" :model="reworkForm" layout="vertical">
        <a-form-item v-if="reworkForm.sourceWoId" label="返工来源工单">{{ reworkForm.sourceWoNo }}</a-form-item>
        <a-form-item label="产品" name="materialId" :rules="[{ required: true, message: '请选择产品' }]">
          <MaterialSelect
            v-model:value="reworkForm.materialId"
            :label="reworkForm.materialLabel"
            :disabled="!!reworkForm.sourceWoId"
            @select="(m) => (reworkForm.materialLabel = m.label)"
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="返工数量" name="qty" :rules="[{ required: true, message: '请填写数量' }]">
              <a-input-number v-model:value="reworkForm.qty" :min="0" :precision="4" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="完工入库仓库" name="warehouseId">
              <WarehouseSelect v-model:value="reworkForm.warehouseId" placeholder="选填，完工入库时默认" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="计划开工" name="planStart">
              <a-date-picker v-model:value="reworkForm.planStart" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="计划完工" name="planEnd">
              <a-date-picker v-model:value="reworkForm.planEnd" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="reworkForm.remark" :rows="2" :maxlength="255" placeholder="返工原因等" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
    <SupplementForm v-model:open="supplementOpen" :wo="detail" />

    <a-modal v-model:open="reportOpen" :title="`报工 - ${reportOp?.seq} ${reportOp?.name}`" :confirm-loading="saving" @ok="submitReport">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="报工日期" required>
              <a-date-picker v-model:value="report.reportDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="实际工时" required>
              <a-input-number v-model:value="report.hours" :min="0" :precision="2" addon-after="小时" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="合格数量" required>
              <a-input-number v-model:value="report.goodQty" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="报废数量">
              <a-input-number v-model:value="report.scrapQty" :min="0" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="生产设备" extra="用于设备 OEE">
              <a-select v-model:value="report.equipmentId" allow-clear show-search option-filter-prop="label" placeholder="不指定" :options="equipmentChoices" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="模具工装" :extra="shotsHint">
              <a-select v-model:value="report.moldId" allow-clear placeholder="不使用" :options="moldChoices" :not-found-content="'没有领用到该工单的模具'" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="操作员" :extra="reportOp?.pieceRate != null ? `计件单价 ¥${formatQty(reportOp.pieceRate)} / 件，计入操作员的计件工资` : '不填为本人'">
          <IdentityUserSelect v-model:value="report.operatorId" placeholder="本人" allow-clear />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="report.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { EyeOutlined, PlusOutlined, PrinterOutlined, RedoOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import SupplementForm from '@/components/mes/SupplementForm.vue'
import WoDocForm from '@/components/mes/WoDocForm.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { bomsOfMaterial, equipmentOptions, moldsOfWo } from '@/api/mes/base'
import { scheduleWo } from '@/api/mes/capacity'
import {
  addReworkWo,
  addWo,
  changeWoQty,
  closeWo,
  getWo,
  pageReport,
  pageWo,
  pageWoDoc,
  releaseWo,
  reportWo,
  splitWo,
  updateWo
} from '@/api/mes/wo'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { formatQty, today } from '@/components/wms/wms'
import { materialColumns as printMaterialColumns, printDocs } from '@/utils/print'

const userStore = useUserStore()
const dict = useDict('mes_wo_status', 'mes_wo_type')

const pct = (a, b) => (Number(b) ? Math.min(Math.round((Number(a) / Number(b)) * 100), 100) : 0)

const columns = [
  { title: '工单号', key: 'woNo', width: 190 },
  { title: '产品', key: 'product', width: 240 },
  { title: '完工进度', key: 'progress', width: 180 },
  { title: '计划日期', key: 'plan', width: 120 },
  { title: '状态', key: 'status', width: 90, dict: 'mes_wo_status' },
  { title: '完工仓库', dataIndex: 'warehouseName', width: 120 },
  { title: '制单人', dataIndex: 'createByName', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 190, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'mes-wo')
const showSearch = ref(true)

const emptyQuery = () => ({ woNo: undefined, keyword: undefined, status: undefined, range: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageWo, query, {
  params: ({ range, ...rest }) => ({ ...rest, beginDate: range?.[0], endDate: range?.[1] })
})

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const route = useRoute()
const router = useRouter()
onMounted(() => {
  load()
  // 从产能排程等页面跳转过来时（?id=工单ID）直接打开详情
  const id = Number(route.query.id)
  if (id) {
    openDetail(id)
    router.replace({ query: {} })
  }
})

// ---------------------------------------------------------------- 新增 / 编辑
const boms = ref([])
const bomOptions = computed(() => boms.value.map((b) => ({ value: b.id, label: `${b.version}${b.isDefault === 1 ? '（默认）' : ''}` })))
async function loadBoms(materialId) {
  boms.value = materialId ? await bomsOfMaterial(materialId) : []
}
async function onProductSelect(m) {
  form.value.materialLabel = m.label
  form.value.bomId = undefined
  await loadBoms(m.id)
}

const rules = {
  materialId: [{ required: true, message: '请选择产品' }],
  qty: [{ required: true, message: '请填写数量' }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, materialId: undefined, bomId: undefined, qty: undefined, planStart: today(), planEnd: undefined, warehouseId: undefined, remark: '' }),
  fetch: async (record) => {
    const wo = await getWo(record.id)
    await loadBoms(wo.materialId)
    return { ...wo, qty: Number(wo.qty), materialLabel: `${wo.materialCode} ${wo.materialName}` }
  },
  beforeOpen: (record) => {
    if (!record) boms.value = []
  },
  create: addWo,
  update: updateWo,
  toPayload: (f) => ({
    materialId: f.materialId,
    bomId: f.bomId || null,
    qty: f.qty,
    planStart: f.planStart || null,
    planEnd: f.planEnd || null,
    warehouseId: f.warehouseId || null,
    remark: f.remark || null
  }),
  onSuccess: load
})
const { formRef, form } = drawer

function release(record) {
  Modal.confirm({
    title: `下达工单 ${record.woNo}？`,
    content: '下达后车间可以领料、报工，工单不能再修改。',
    async onOk() {
      await releaseWo(record.id)
      message.success('已下达')
      load()
      if (detailOpen.value) refreshDetail()
    }
  })
}

// ---------------------------------------------------------------- 详情
const materialColumns = [
  { title: '物料', key: 'material', width: 260 },
  { title: '单位用量（含损耗）', key: 'unitQty', width: 150, align: 'right' },
  { title: '领料进度', key: 'progress', width: 260 }
]
const opColumns = [
  { title: '工序号', dataIndex: 'seq', width: 80 },
  { title: '工序', key: 'name', width: 160 },
  { title: '工作中心', dataIndex: 'workCenterName', width: 140 },
  { title: '进度', key: 'progress', width: 220 },
  { title: '实际 / 标准工时', key: 'hours', width: 170 },
  { title: '计划日期', key: 'plan', width: 120 },
  { title: '', key: 'action', width: 70 }
]
const docColumns = [
  { title: '单据号', dataIndex: 'docNo', width: 170 },
  { title: '类型', key: 'kind', width: 100, dict: 'mes_wo_doc_kind' },
  { title: '仓库', dataIndex: 'warehouseName', width: 130 },
  { title: '业务日期', dataIndex: 'bizDate', width: 110 },
  { title: '数量', key: 'totalQty', width: 90, align: 'right' },
  { title: '仓库单据', dataIndex: 'wmsDocNo', width: 170 },
  { title: '制单人', dataIndex: 'createByName', width: 100 }
]
const reportColumns = [
  { title: '日期', dataIndex: 'reportDate', width: 110 },
  { title: '工序', key: 'op', width: 140 },
  { title: '合格', key: 'goodQty', width: 80, align: 'right' },
  { title: '报废', key: 'scrapQty', width: 80, align: 'right' },
  { title: '工时', key: 'hours', width: 80, align: 'right' },
  { title: '操作员', key: 'operator', width: 100 },
  { title: '计件工资', key: 'pieceWage', width: 90, align: 'right' },
  { title: '设备 / 模具', key: 'equipment', width: 160 },
  { title: '备注', dataIndex: 'remark', ellipsis: true }
]

const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref()

/** 生产任务单：用料（含补料）和工序 */
function print() {
  const d = detail.value
  printDocs({
    title: '生产任务单',
    no: d.woNo,
    fields: [
      ['产品', `${d.materialCode} ${d.materialName}`],
      ['BOM 版本', d.bomVersion],
      ['计划数量', `${formatQty(d.qty)} ${d.unit || ''}`],
      ['计划开工', d.planStart],
      ['计划完工', d.planEnd],
      ['完工入库仓库', d.warehouseName]
    ],
    columns: [
      ...printMaterialColumns(),
      { title: '单位用量', value: (r) => formatQty(r.unitQty), align: 'right' },
      { title: '需求数量', value: (r) => formatQty(r.requiredQty), align: 'right' },
      { title: '补料', value: (r) => (Number(r.supplementQty) ? formatQty(r.supplementQty) : ''), align: 'right' },
      { title: '已领', value: (r) => formatQty(r.issuedQty), align: 'right' }
    ],
    rows: d.materials,
    sections: d.ops?.length
      ? [{
          title: '工序',
          columns: [
            { title: '工序号', key: 'seq' },
            { title: '工序', key: 'name' },
            { title: '工作中心', key: 'workCenterName' },
            { title: '合格数', value: () => '' },
            { title: '报废数', value: () => '' },
            { title: '操作员', value: () => '' }
          ],
          rows: d.ops
        }]
      : [],
    note: d.remark,
    signs: [['制单', d.createByName], '生产主管', '仓库']
  }, `生产任务单 ${d.woNo}`, 'mes.workOrder')
}
const docs = ref([])
const reports = ref([])
const sourceWoNo = ref()
let detailId

/** 各工序报工报废数合计 */
const scrapReported = computed(() => (detail.value?.ops || []).reduce((sum, o) => sum + Number(o.scrapQty || 0), 0))
/** 已报废入库数量 */
const scrapIn = computed(() => docs.value.filter((d) => d.kind === 'SCRAP').reduce((sum, d) => sum + Number(d.totalQty || 0), 0))

async function refreshDetail() {
  detailLoading.value = true
  try {
    const [wo, docPage, reportPage] = await Promise.all([
      getWo(detailId),
      pageWoDoc({ woId: detailId, current: 1, size: 200 }),
      pageReport({ woId: detailId, current: 1, size: 200 })
    ])
    detail.value = wo
    docs.value = docPage.records
    reports.value = reportPage.records
    sourceWoNo.value = wo.sourceWoId ? (await getWo(wo.sourceWoId).catch(() => null))?.woNo : undefined
  } finally {
    detailLoading.value = false
  }
  load()
}

function openDetail(id) {
  detailId = id
  detail.value = undefined
  detailOpen.value = true
  refreshDetail()
}

const supplementOpen = ref(false)
const docOpen = ref(false)
const docKind = ref('issue')
function openDoc(kind) {
  docKind.value = kind
  docOpen.value = true
}

const actions = computed(() => {
  const d = detail.value
  if (!d) return []
  const list = []
  const can = (p) => userStore.hasPerm(`mes:wo:${p}`)
  if (['DRAFT', 'RELEASED', 'STARTED'].includes(d.status)) {
    if (userStore.hasPerm('mes:capacity:edit') || can('edit')) list.push({ key: 'schedule', label: '排程', run: () => schedule(d) })
    if (can('change')) list.push({ key: 'split', label: '拆分', run: openSplit })
  }
  if (['STARTED', 'COMPLETED'].includes(d.status) && can('add')) list.push({ key: 'rework', label: '新建返工工单', run: () => openRework(d) })
  if (d.status === 'DRAFT' && can('release')) list.push({ key: 'release', label: '下达', primary: true, run: () => release(d) })
  if (['RELEASED', 'STARTED'].includes(d.status)) {
    if (can('change')) list.push({ key: 'qty', label: '改量', run: openQty })
    if (can('release'))
      list.push({
        key: 'close',
        label: '关闭',
        danger: true,
        run: () =>
          Modal.confirm({
            title: '关闭工单？',
            content: '未完工部分不再生产，已领未用的物料请先退料。',
            async onOk() {
              await closeWo(d.id)
              message.success('已关闭')
              refreshDetail()
            }
          })
      })
    if (can('issue')) {
      list.push({ key: 'return', label: '退料', run: () => openDoc('return') })
      list.push({ key: 'issue', label: '领料', run: () => openDoc('issue') })
    }
    if (userStore.hasPerm('mes:supplement:add')) list.push({ key: 'supplement', label: '补料申请', run: () => (supplementOpen.value = true) })
    if (can('finish')) {
      list.push({ key: 'scrap', label: '报废入库', run: () => openDoc('scrap') })
      list.push({ key: 'finish', label: '完工入库', primary: true, run: () => openDoc('finish') })
    }
  }
  return list
})

// ---------------------------------------------------------------- 排程、改量、拆分、返工
function schedule(d) {
  Modal.confirm({
    title: `排程工单 ${d.woNo}？`,
    content: '按工作中心的有限产能从计划开工日起排定各工序的计划日期，覆盖已有的工序计划日期。',
    async onOk() {
      await scheduleWo(d.id)
      message.success('排程完成')
      refreshDetail()
    }
  })
}

const qtyOpen = ref(false)
const qtyValue = ref()
function openQty() {
  qtyValue.value = Number(detail.value.qty)
  qtyOpen.value = true
}
async function submitQty() {
  if (!(Number(qtyValue.value) > 0)) {
    message.warning('请填写生产数量')
    return
  }
  saving.value = true
  try {
    await changeWoQty(detail.value.id, qtyValue.value)
    message.success('已修改数量')
    qtyOpen.value = false
    refreshDetail()
  } finally {
    saving.value = false
  }
}

const splitOpen = ref(false)
const splitForm = reactive({ qty: undefined, planStart: undefined, planEnd: undefined })
function openSplit() {
  Object.assign(splitForm, { qty: undefined, planStart: detail.value.planStart, planEnd: detail.value.planEnd })
  splitOpen.value = true
}
async function submitSplit() {
  if (!(Number(splitForm.qty) > 0)) {
    message.warning('请填写拆分数量')
    return
  }
  saving.value = true
  try {
    const id = await splitWo(detail.value.id, { qty: splitForm.qty, planStart: splitForm.planStart || null, planEnd: splitForm.planEnd || null })
    splitOpen.value = false
    refreshDetail()
    offerOpen(id, '拆分成功', '已生成新的草稿工单，下达后才能领料、报工。')
  } finally {
    saving.value = false
  }
}

/** 新工单生成后询问是否打开 */
function offerOpen(id, title, content) {
  Modal.confirm({ title, content, okText: '打开新工单', cancelText: '留在本页', onOk: () => openDetail(id) })
}

const reworkOpen = ref(false)
const reworkFormRef = ref()
const emptyRework = () => ({
  sourceWoId: undefined,
  sourceWoNo: undefined,
  materialId: undefined,
  materialLabel: undefined,
  qty: undefined,
  planStart: today(),
  planEnd: undefined,
  warehouseId: undefined,
  remark: ''
})
const reworkForm = reactive(emptyRework())
/** 新建返工工单；从工单详情进入时带出产品和来源工单 */
function openRework(wo) {
  Object.assign(reworkForm, emptyRework())
  if (wo) {
    Object.assign(reworkForm, {
      sourceWoId: wo.id,
      sourceWoNo: wo.woNo,
      materialId: wo.materialId,
      materialLabel: `${wo.materialCode} ${wo.materialName}`,
      warehouseId: wo.warehouseId || undefined
    })
  }
  reworkOpen.value = true
}
async function submitRework() {
  try {
    await reworkFormRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const f = reworkForm
    const id = await addReworkWo(
      { materialId: f.materialId, qty: f.qty, planStart: f.planStart || null, planEnd: f.planEnd || null, warehouseId: f.warehouseId || null, remark: f.remark || null },
      f.sourceWoId
    )
    reworkOpen.value = false
    load()
    offerOpen(id, '已新建返工工单', '返工工单为草稿，下达后才能领料、报工。')
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 报工
const reportOpen = ref(false)
const reportOp = ref()
const saving = ref(false)
const report = reactive({ reportDate: today(), goodQty: undefined, scrapQty: 0, hours: undefined, remark: '', operatorId: undefined,
  equipmentId: undefined, moldId: undefined })
const equipments = ref([])
const molds = ref([])
/** 工序所在工作中心的设备排在前面 */
const equipmentChoices = computed(() => [...equipments.value]
  .sort((a, b) => Number(b.workCenterId === reportOp.value?.workCenterId) - Number(a.workCenterId === reportOp.value?.workCenterId))
  .map((e) => ({ value: e.id, label: `${e.code} ${e.name}` })))
const moldChoices = computed(() => molds.value.map((m) => ({ value: m.id, label: `${m.code} ${m.name}（${m.cavities} 穴）` })))
const shotsHint = computed(() => {
  const m = molds.value.find((x) => x.id === report.moldId)
  if (!m) return '须先在「模具工装」中领用到该工单'
  const shots = Math.ceil((Number(report.goodQty || 0) + Number(report.scrapQty || 0)) / m.cavities)
  return `本次 ${shots} 模次，累计 ${m.usedCount + shots}${m.designLife ? ` / ${m.designLife}` : ''}`
})
async function openReport(op) {
  reportOp.value = op
  const remaining = Math.max(Number(detail.value.qty) - Number(op.goodQty) - Number(op.scrapQty), 0)
  const hours = Math.round(Number(op.stdHours) * remaining * 100) / 100
  const [eqs, ms] = await Promise.all([equipmentOptions(), moldsOfWo(detail.value.id)])
  equipments.value = eqs
  molds.value = ms
  const sameCenter = eqs.filter((e) => e.workCenterId === op.workCenterId)
  Object.assign(report, { reportDate: today(), goodQty: remaining, scrapQty: 0, hours, remark: '', operatorId: undefined,
    equipmentId: ms[0]?.equipmentId ?? (sameCenter.length === 1 ? sameCenter[0].id : undefined), moldId: ms.length === 1 ? ms[0].id : undefined })
  reportOpen.value = true
}
async function submitReport() {
  saving.value = true
  try {
    await reportWo(detail.value.id, { opId: reportOp.value.id, ...report, operatorId: report.operatorId ?? null,
      equipmentId: report.equipmentId ?? null, moldId: report.moldId ?? null })
    message.success('报工成功')
    reportOpen.value = false
    refreshDetail()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.source-tag {
  margin-left: 4px;
}
</style>
