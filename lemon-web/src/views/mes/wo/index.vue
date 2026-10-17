<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('mes.wo.woNo')" name="woNo">
          <a-input v-model:value="query.woNo" :placeholder="$t('mes.wo.woNoPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('mes.wo.product')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('mes.wo.productPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.mes_wo_status)" />
        </a-form-item>
        <a-form-item :label="$t('mes.wo.planStart')" name="range">
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
          <template #icon><PlusOutlined /></template>{{ $t('mes.wo.add') }}
        </a-button>
        <a-button v-perm="'mes:wo:add'" @click="openRework()">
          <template #icon><RedoOutlined /></template>{{ $t('mes.wo.addRework') }}
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
            <a-tag v-else-if="record.source === 'KIT'" color="orange" :bordered="false" class="source-tag">{{ $t('mes.wo.source.kit') }}</a-tag>
            <a-tag v-else-if="record.source === 'PLAN'" color="blue" :bordered="false" class="source-tag">{{ $t('mes.wo.source.plan') }}</a-tag>
            <a-tag v-else-if="record.source === 'SPLIT'" color="cyan" :bordered="false" class="source-tag">{{ $t('mes.wo.source.split') }}</a-tag>
            <a-tag v-if="record.woType === 'REWORK'" color="orange" :bordered="false" class="source-tag">{{ $t('mes.wo.source.rework') }}</a-tag>
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
            <div class="sub-text">{{ $t('mes.wo.to', { v: record.planEnd || '-' }) }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="mes_wo_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record.id)">
                <template #icon><EyeOutlined /></template>{{ $t('common.action.view') }}
              </a-button>
              <template v-if="record.status === 'DRAFT'">
                <a-button v-perm="'mes:wo:edit'" type="link" size="small" @click="drawer.openEdit(record)">{{ $t('common.action.edit') }}</a-button>
                <a-button v-perm="'mes:wo:release'" type="link" size="small" @click="release(record)">{{ $t('mes.wo.release') }}</a-button>
              </template>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增 / 编辑 -->
    <FormDrawer :drawer="drawer" :create-title="$t('mes.wo.add')" :edit-title="$t('mes.wo.edit')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('mes.wo.product')" name="materialId">
          <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" @select="onProductSelect" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="BOM" name="bomId">
              <a-select v-model:value="form.bomId" :options="bomOptions" :placeholder="$t('mes.wo.defaultBom')" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.wo.qty')" name="qty">
              <a-input-number v-model:value="form.qty" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.wo.planStart')" name="planStart">
              <a-date-picker v-model:value="form.planStart" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.wo.planEnd')" name="planEnd">
              <a-date-picker v-model:value="form.planEnd" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('mes.wo.finishWarehouse')" name="warehouseId">
              <WarehouseSelect v-model:value="form.warehouseId" :placeholder="$t('mes.wo.finishWarehousePh')" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <!-- 详情：用料、工序、单据、报工 -->
    <SubPage
      :open="detailOpen"
      :title="detail ? $t('mes.wo.detailTitle', { no: detail.woNo }) : $t('mes.wo.wo')"
      width="min(1100px, 100vw)"
      destroy-on-close
      @close="detailOpen = false"
    >
      <template #extra>
        <a-space>
          <a-button v-if="detail" size="small" @click="print"><template #icon><PrinterOutlined /></template>{{ $t('common.action.print') }}</a-button>
          <DictTag v-if="detail?.woType === 'REWORK'" type="mes_wo_type" :value="detail.woType" />
          <DictTag v-if="detail" type="mes_wo_status" :value="detail.status" />
        </a-space>
      </template>
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
            <a-descriptions-item :label="$t('mes.wo.product')">{{ detail.materialCode }} {{ detail.materialName }}</a-descriptions-item>
            <a-descriptions-item label="BOM">{{ detail.bomVersion }}</a-descriptions-item>
            <a-descriptions-item :label="$t('mes.wo.finishedOfPlan')">{{ formatQty(detail.finishedQty) }} / {{ formatQty(detail.qty) }} {{ detail.unit }}</a-descriptions-item>
            <a-descriptions-item :label="$t('mes.wo.planDate')">{{ detail.planStart || '-' }} ~ {{ detail.planEnd || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('mes.wo.finishWarehouse')">{{ detail.warehouseName || '-' }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.sourceWoId" :label="detail.woType === 'REWORK' ? $t('mes.wo.reworkFrom') : $t('mes.wo.splitFrom')">
              <a @click="openDetail(detail.sourceWoId)">{{ sourceWoNo || $t('mes.wo.woById', { id: detail.sourceWoId }) }}</a>
            </a-descriptions-item>
            <a-descriptions-item v-if="scrapReported > 0" :label="$t('mes.wo.scrapReportedIn')">
              {{ formatQty(scrapReported) }} / {{ formatQty(scrapIn) }} {{ detail.unit }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('mes.wo.preparedBy')">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.releaseTime" :label="$t('mes.wo.releaseTime')">{{ detail.releaseTime }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.finishTime" :label="$t('mes.wo.finishTime')">{{ detail.finishTime }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
          </a-descriptions>
          <a-tabs size="small">
            <a-tab-pane key="materials" :tab="$t('mes.wo.tab.materials')">
              <a-table row-key="id" size="small" :columns="materialColumns" :data-source="detail.materials" :pagination="false">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
                  <template v-else-if="column.key === 'unitQty'">{{ formatQty(record.unitQty) }}</template>
                  <template v-else-if="column.key === 'progress'">
                    <a-progress :percent="pct(record.issuedQty, Number(record.requiredQty) + Number(record.supplementQty || 0))" size="small" />
                    <div class="sub-text">
                      {{ $t('mes.wo.issuedOfRequired', { issued: formatQty(record.issuedQty), req: formatQty(record.requiredQty), unit: record.unit || '' }) }}
                      <template v-if="Number(record.supplementQty)">{{ $t('mes.wo.inclSupplement', { v: formatQty(record.supplementQty) }) }}</template>
                    </div>
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="ops" :tab="$t('mes.wo.tab.ops')">
              <a-table row-key="id" size="small" :columns="opColumns" :data-source="detail.ops" :pagination="false">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'name'">
                    {{ record.name }}
                    <a-tooltip v-if="record.firstArticle === 1" :title="$t('mes.wo.firstArticleTip')">
                      <a-tag color="purple" :bordered="false">{{ $t('mes.wo.firstArticle') }}</a-tag>
                    </a-tooltip>
                  </template>
                  <template v-else-if="column.key === 'progress'">
                    <a-progress :percent="pct(record.goodQty, detail.qty)" size="small" />
                    <div class="sub-text">{{ $t('mes.wo.goodScrap', { good: formatQty(record.goodQty), scrap: formatQty(record.scrapQty) }) }}</div>
                  </template>
                  <template v-else-if="column.key === 'hours'">
                    {{ $t('mes.wo.hoursOf', { actual: formatQty(record.hours), std: formatQty(Number(record.stdHours) * Number(detail.qty)) }) }}
                  </template>
                  <template v-else-if="column.key === 'plan'">
                    <template v-if="record.planStart">
                      <div>{{ record.planStart }}</div>
                      <div class="sub-text">{{ $t('mes.wo.to', { v: record.planEnd || '-' }) }}</div>
                    </template>
                    <span v-else class="sub-text">{{ $t('mes.wo.unscheduled') }}</span>
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <a-button
                      v-if="['RELEASED', 'STARTED'].includes(detail.status)"
                      v-perm="'mes:wo:report'"
                      type="link"
                      size="small"
                      @click="openReport(record)"
                    >
                      {{ $t('mes.wo.report') }}
                    </a-button>
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="docs" :tab="$t('mes.wo.tab.docs')">
              <a-table row-key="id" size="small" :columns="docColumns" :data-source="docs" :pagination="false">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'kind'"><DictTag type="mes_wo_doc_kind" :value="record.kind" /></template>
                  <template v-else-if="column.key === 'totalQty'">{{ formatQty(record.totalQty) }}</template>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="reports" :tab="$t('mes.wo.tab.reports')">
              <a-table row-key="id" size="small" :columns="reportColumns" :data-source="reports" :pagination="false">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'op'">{{ record.opSeq }} {{ record.opName }}</template>
                  <template v-else-if="['goodQty', 'scrapQty', 'hours'].includes(column.key)">{{ formatQty(record[column.key]) }}</template>
                  <template v-else-if="column.key === 'operator'">{{ record.operatorName || record.createByName }}</template>
                  <template v-else-if="column.key === 'pieceWage'">{{ record.pieceWage != null ? '¥' + formatQty(record.pieceWage) : '-' }}</template>
                  <template v-else-if="column.key === 'equipment'">
                    {{ record.equipmentName || '-' }}
                    <div v-if="record.moldCode" class="sub-text">{{ $t('mes.wo.moldShots', { code: record.moldCode, shots: record.shots }) }}</div>
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="attachment" :tab="$t('mes.wo.tab.attachment')">
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
    <a-modal v-model:open="qtyOpen" :title="$t('mes.wo.qtyTitle', { no: detail?.woNo })" :confirm-loading="saving" @ok="submitQty">
      <a-alert type="info" show-icon class="block" :message="$t('mes.wo.qtyTip')" />
      <a-form layout="vertical">
        <a-form-item :label="$t('mes.wo.qty')" required :extra="detail ? $t('mes.wo.qtyExtra', { qty: formatQty(detail.qty), finished: formatQty(detail.finishedQty), unit: detail.unit || '' }) : ''">
          <a-input-number v-model:value="qtyValue" :min="0" :precision="4" :addon-after="detail?.unit || undefined" class="full" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 拆分 -->
    <a-modal v-model:open="splitOpen" :title="$t('mes.wo.splitTitle', { no: detail?.woNo })" :confirm-loading="saving" @ok="submitSplit">
      <a-alert
        type="info"
        show-icon
        class="block"
        :message="$t('mes.wo.splitTip')"
      />
      <a-form layout="vertical">
        <a-form-item :label="$t('mes.wo.splitQty')" required :extra="detail ? $t('mes.wo.splitExtra', { qty: formatQty(detail.qty), unit: detail.unit || '' }) : ''">
          <a-input-number v-model:value="splitForm.qty" :min="0" :precision="4" :addon-after="detail?.unit || undefined" class="full" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('mes.wo.planStart')">
              <a-date-picker v-model:value="splitForm.planStart" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.wo.planEnd')">
              <a-date-picker v-model:value="splitForm.planEnd" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 返工工单 -->
    <a-modal v-model:open="reworkOpen" :title="$t('mes.wo.addRework')" width="640px" :confirm-loading="saving" @ok="submitRework">
      <a-alert
        type="info"
        show-icon
        class="block"
        :message="$t('mes.wo.reworkTip')"
      />
      <a-form ref="reworkFormRef" :model="reworkForm" layout="vertical">
        <a-form-item v-if="reworkForm.sourceWoId" :label="$t('mes.wo.reworkSource')">{{ reworkForm.sourceWoNo }}</a-form-item>
        <a-form-item :label="$t('mes.wo.product')" name="materialId" :rules="[{ required: true, message: $t('mes.wo.productRequired') }]">
          <MaterialSelect
            v-model:value="reworkForm.materialId"
            :label="reworkForm.materialLabel"
            :disabled="!!reworkForm.sourceWoId"
            @select="(m) => (reworkForm.materialLabel = m.label)"
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.wo.reworkQty')" name="qty" :rules="[{ required: true, message: $t('mes.wo.qtyRequired') }]">
              <a-input-number v-model:value="reworkForm.qty" :min="0" :precision="4" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.wo.finishWarehouse')" name="warehouseId">
              <WarehouseSelect v-model:value="reworkForm.warehouseId" :placeholder="$t('mes.wo.finishWarehousePh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.wo.planStart')" name="planStart">
              <a-date-picker v-model:value="reworkForm.planStart" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.wo.planEnd')" name="planEnd">
              <a-date-picker v-model:value="reworkForm.planEnd" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-textarea v-model:value="reworkForm.remark" :rows="2" :maxlength="255" :placeholder="$t('mes.wo.reworkRemarkPh')" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
    <SupplementForm v-model:open="supplementOpen" :wo="detail" />

    <a-modal v-model:open="reportOpen" :title="$t('mes.wo.reportTitle', { op: `${reportOp?.seq} ${reportOp?.name}` })" :confirm-loading="saving" @ok="submitReport">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('mes.wo.reportDate')" required>
              <a-date-picker v-model:value="report.reportDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.wo.actualHours')" required>
              <a-input-number v-model:value="report.hours" :min="0" :precision="2" :addon-after="$t('mes.wo.hoursUnit')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.wo.goodQty')" required>
              <a-input-number v-model:value="report.goodQty" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.wo.scrapQty')">
              <a-input-number v-model:value="report.scrapQty" :min="0" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('mes.wo.equipment')" :extra="$t('mes.wo.equipmentExtra')">
              <a-select v-model:value="report.equipmentId" allow-clear show-search option-filter-prop="label" :placeholder="$t('mes.wo.notSpecified')" :options="equipmentChoices" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.wo.mold')" :extra="shotsHint">
              <a-select v-model:value="report.moldId" allow-clear :placeholder="$t('mes.wo.notUsed')" :options="moldChoices" :not-found-content="$t('mes.wo.noMold')" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('mes.wo.operator')" :extra="reportOp?.pieceRate != null ? $t('mes.wo.pieceRateExtra', { v: formatQty(reportOp.pieceRate) }) : $t('mes.wo.operatorDefault')">
          <IdentityUserSelect v-model:value="report.operatorId" :placeholder="$t('mes.wo.self')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')">
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
import { useI18n } from 'vue-i18n'
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
import { printLabel } from '@/utils/printTemplates'

const userStore = useUserStore()
const { t } = useI18n()
const dict = useDict('mes_wo_status', 'mes_wo_type')

const pct = (a, b) => (Number(b) ? Math.min(Math.round((Number(a) / Number(b)) * 100), 100) : 0)

const columns = [
  { title: t('mes.wo.woNo'), key: 'woNo', width: 190 },
  { title: t('mes.wo.product'), key: 'product', width: 240 },
  { title: t('mes.wo.progress'), key: 'progress', width: 180 },
  { title: t('mes.wo.planDate'), key: 'plan', width: 120 },
  { title: t('common.field.status'), key: 'status', width: 100, dict: 'mes_wo_status' },
  { title: t('mes.wo.finishWarehouseShort'), dataIndex: 'warehouseName', width: 130 },
  { title: t('mes.wo.preparedByCol'), dataIndex: 'createByName', width: 110 },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 190, fixed: 'right' }
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
const bomOptions = computed(() => boms.value.map((b) => ({ value: b.id, label: b.isDefault === 1 ? t('mes.wo.bomDefault', { v: b.version }) : b.version })))
async function loadBoms(materialId) {
  boms.value = materialId ? await bomsOfMaterial(materialId) : []
}
async function onProductSelect(m) {
  form.value.materialLabel = m.label
  form.value.bomId = undefined
  await loadBoms(m.id)
}

const rules = {
  materialId: [{ required: true, message: t('mes.wo.productRequired') }],
  qty: [{ required: true, message: t('mes.wo.qtyRequired') }]
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
    title: t('mes.wo.releaseConfirm', { no: record.woNo }),
    content: t('mes.wo.releaseContent'),
    async onOk() {
      await releaseWo(record.id)
      message.success(t('mes.wo.released'))
      load()
      if (detailOpen.value) refreshDetail()
    }
  })
}

// ---------------------------------------------------------------- 详情
const materialColumns = [
  { title: t('mes.wo.material'), key: 'material', width: 260 },
  { title: t('mes.wo.unitQty'), key: 'unitQty', width: 170, align: 'right' },
  { title: t('mes.wo.issueProgress'), key: 'progress', width: 260 }
]
const opColumns = [
  { title: t('mes.wo.opSeq'), dataIndex: 'seq', width: 80 },
  { title: t('mes.wo.op'), key: 'name', width: 160 },
  { title: t('mes.wo.workCenter'), dataIndex: 'workCenterName', width: 140 },
  { title: t('mes.wo.opProgress'), key: 'progress', width: 220 },
  { title: t('mes.wo.actualStdHours'), key: 'hours', width: 170 },
  { title: t('mes.wo.planDate'), key: 'plan', width: 120 },
  { title: '', key: 'action', width: 70 }
]
const docColumns = [
  { title: t('mes.wo.docNo'), dataIndex: 'docNo', width: 170 },
  { title: t('common.field.type'), key: 'kind', width: 130, dict: 'mes_wo_doc_kind' },
  { title: t('mes.wo.warehouse'), dataIndex: 'warehouseName', width: 130 },
  { title: t('mes.wo.bizDate'), dataIndex: 'bizDate', width: 120 },
  { title: t('common.field.quantity'), key: 'totalQty', width: 90, align: 'right' },
  { title: t('mes.wo.wmsDoc'), dataIndex: 'wmsDocNo', width: 170 },
  { title: t('mes.wo.preparedByCol'), dataIndex: 'createByName', width: 110 }
]
const reportColumns = [
  { title: t('common.field.date'), dataIndex: 'reportDate', width: 110 },
  { title: t('mes.wo.op'), key: 'op', width: 140 },
  { title: t('mes.wo.good'), key: 'goodQty', width: 80, align: 'right' },
  { title: t('mes.wo.scrap'), key: 'scrapQty', width: 80, align: 'right' },
  { title: t('mes.wo.hours'), key: 'hours', width: 80, align: 'right' },
  { title: t('mes.wo.operator'), key: 'operator', width: 100 },
  { title: t('mes.wo.pieceWage'), key: 'pieceWage', width: 100, align: 'right' },
  { title: t('mes.wo.equipmentMold'), key: 'equipment', width: 160 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
]

const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref()

/** 生产任务单：用料（含补料）和工序。名称写中文（打印模板的匹配键），显示时按界面语言翻译（utils/print.js） */
function print() {
  const d = detail.value
  printDocs({
    title: '生产任务单', // i18n-ignore
    no: d.woNo,
    fields: [
      ['产品', `${d.materialCode} ${d.materialName}`], // i18n-ignore
      ['BOM 版本', d.bomVersion], // i18n-ignore
      ['计划数量', `${formatQty(d.qty)} ${d.unit || ''}`], // i18n-ignore
      ['计划开工', d.planStart], // i18n-ignore
      ['计划完工', d.planEnd], // i18n-ignore
      ['完工入库仓库', d.warehouseName] // i18n-ignore
    ],
    columns: [
      ...printMaterialColumns(),
      { title: '单位用量', value: (r) => formatQty(r.unitQty), align: 'right' }, // i18n-ignore
      { title: '需求数量', value: (r) => formatQty(r.requiredQty), align: 'right' }, // i18n-ignore
      { title: '补料', value: (r) => (Number(r.supplementQty) ? formatQty(r.supplementQty) : ''), align: 'right' }, // i18n-ignore
      { title: '已领', value: (r) => formatQty(r.issuedQty), align: 'right' } // i18n-ignore
    ],
    rows: d.materials,
    sections: d.ops?.length
      ? [{
          title: '工序', // i18n-ignore
          columns: [
            { title: '工序号', key: 'seq' }, // i18n-ignore
            { title: '工序', key: 'name' }, // i18n-ignore
            { title: '工作中心', key: 'workCenterName' }, // i18n-ignore
            { title: '合格数', value: () => '' }, // i18n-ignore
            { title: '报废数', value: () => '' }, // i18n-ignore
            { title: '操作员', value: () => '' } // i18n-ignore
          ],
          rows: d.ops
        }]
      : [],
    note: d.remark,
    signs: [['制单', d.createByName], '生产主管', '仓库'] // i18n-ignore
  }, `${printLabel('生产任务单')} ${d.woNo}`, 'mes.workOrder') // i18n-ignore
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
    if (userStore.hasPerm('mes:capacity:edit') || can('edit')) list.push({ key: 'schedule', label: t('mes.wo.schedule'), run: () => schedule(d) })
    if (can('change')) list.push({ key: 'split', label: t('mes.wo.split'), run: openSplit })
  }
  if (['STARTED', 'COMPLETED'].includes(d.status) && can('add')) list.push({ key: 'rework', label: t('mes.wo.addRework'), run: () => openRework(d) })
  if (d.status === 'DRAFT' && can('release')) list.push({ key: 'release', label: t('mes.wo.release'), primary: true, run: () => release(d) })
  if (['RELEASED', 'STARTED'].includes(d.status)) {
    if (can('change')) list.push({ key: 'qty', label: t('mes.wo.changeQty'), run: openQty })
    if (can('release'))
      list.push({
        key: 'close',
        label: t('common.action.close'),
        danger: true,
        run: () =>
          Modal.confirm({
            title: t('mes.wo.closeConfirm'),
            content: t('mes.wo.closeContent'),
            async onOk() {
              await closeWo(d.id)
              message.success(t('mes.wo.closed'))
              refreshDetail()
            }
          })
      })
    if (can('issue')) {
      list.push({ key: 'return', label: t('mes.woDocForm.kind.return'), run: () => openDoc('return') })
      list.push({ key: 'issue', label: t('mes.woDocForm.kind.issue'), run: () => openDoc('issue') })
    }
    if (userStore.hasPerm('mes:supplement:add')) list.push({ key: 'supplement', label: t('mes.supplementForm.title'), run: () => (supplementOpen.value = true) })
    if (can('finish')) {
      list.push({ key: 'scrap', label: t('mes.woDocForm.kind.scrap'), run: () => openDoc('scrap') })
      list.push({ key: 'finish', label: t('mes.woDocForm.kind.finish'), primary: true, run: () => openDoc('finish') })
    }
  }
  return list
})

// ---------------------------------------------------------------- 排程、改量、拆分、返工
function schedule(d) {
  Modal.confirm({
    title: t('mes.wo.scheduleConfirm', { no: d.woNo }),
    content: t('mes.wo.scheduleContent'),
    async onOk() {
      await scheduleWo(d.id)
      message.success(t('mes.wo.scheduled'))
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
    message.warning(t('mes.wo.qtyInputRequired'))
    return
  }
  saving.value = true
  try {
    await changeWoQty(detail.value.id, qtyValue.value)
    message.success(t('mes.wo.qtyChanged'))
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
    message.warning(t('mes.wo.splitQtyRequired'))
    return
  }
  saving.value = true
  try {
    const id = await splitWo(detail.value.id, { qty: splitForm.qty, planStart: splitForm.planStart || null, planEnd: splitForm.planEnd || null })
    splitOpen.value = false
    refreshDetail()
    offerOpen(id, t('mes.wo.splitSuccess'), t('mes.wo.splitSuccessContent'))
  } finally {
    saving.value = false
  }
}

/** 新工单生成后询问是否打开 */
function offerOpen(id, title, content) {
  Modal.confirm({ title, content, okText: t('mes.wo.openNew'), cancelText: t('mes.wo.stay'), onOk: () => openDetail(id) })
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
    offerOpen(id, t('mes.wo.reworkCreated'), t('mes.wo.reworkCreatedContent'))
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
const moldChoices = computed(() => molds.value.map((m) => ({ value: m.id, label: t('mes.wo.moldOption', { code: m.code, name: m.name, n: m.cavities }) })))
const shotsHint = computed(() => {
  const m = molds.value.find((x) => x.id === report.moldId)
  if (!m) return t('mes.wo.moldRequired')
  const shots = Math.ceil((Number(report.goodQty || 0) + Number(report.scrapQty || 0)) / m.cavities)
  return m.designLife ? t('mes.wo.shotsHintLife', { shots, used: m.usedCount + shots, life: m.designLife }) : t('mes.wo.shotsHint', { shots, used: m.usedCount + shots })
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
    message.success(t('mes.wo.reportSuccess'))
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
