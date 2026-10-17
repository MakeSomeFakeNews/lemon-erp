<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="物料" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="编码或名称" allow-clear />
        </a-form-item>
        <a-form-item label="分类" name="category">
          <a-select v-model:value="query.category" placeholder="全部" allow-clear :options="toOptions(dict.wms_material_category)" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.sys_common_status, true)" />
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
        <a-button v-perm="'wms:material:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增物料
        </a-button>
        <a-button v-perm="'wms:material:add'" @click="importOpen = true">
          <template #icon><ImportOutlined /></template>导入
        </a-button>
        <span v-perm="'wms:material:remove'">
          <a-popconfirm
            :title="`确定删除选中的 ${selectedKeys.length} 个物料？`"
            :disabled="!selectedKeys.length"
            @confirm="remove(selectedKeys)"
          >
            <a-button danger :disabled="!selectedKeys.length">
              <template #icon><DeleteOutlined /></template>批量删除
            </a-button>
          </a-popconfirm>
        </span>
        <a-button :disabled="!selectedKeys.length" @click="openLabels">
          <template #icon><BarcodeOutlined /></template>打印标签{{ selectedKeys.length ? `（${selectedKeys.length}）` : '' }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: 1580, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="openDetail(record.id)">{{ record.name }}</a>
            <a-tag v-if="record.isTemplate === 1" color="magenta" :bordered="false" class="ml">款式</a-tag>
            <div class="sub-text">
              {{ record.code }}
              <template v-if="record.variantText"> · {{ record.variantText }}</template>
            </div>
          </template>
          <template v-else-if="column.key === 'category'">
            <DictTag type="wms_material_category" :value="record.category" />
          </template>
          <template v-else-if="column.key === 'tracking'">
            <a-tag v-if="record.batchEnabled === 1" color="blue" :bordered="false">批次</a-tag>
            <a-tag v-if="record.serialEnabled === 1" color="purple" :bordered="false">序列号</a-tag>
            <a-tag v-if="record.iqcRequired === 1" color="orange" :bordered="false">来料检验</a-tag>
            <span v-if="record.batchEnabled !== 1 && record.serialEnabled !== 1 && record.iqcRequired !== 1" class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'supplyType'">
            <template v-if="record.plan">
              <DictTag type="mes_supply_type" :value="record.plan.effectiveSupplyType" />
              <div v-if="!record.plan.supplyType" class="sub-text">自动</div>
            </template>
          </template>
          <template v-else-if="column.key === 'planRule'">
            <template v-if="record.plan">
              <div v-for="t in planRule(record.plan)" :key="t" class="rule-line">{{ t }}</div>
              <span v-if="!planRule(record.plan).length" class="sub-text">-</span>
            </template>
          </template>
          <template v-else-if="column.key === 'safetyStock'">{{ formatQty(record.safetyStock) }}</template>
          <template v-else-if="column.key === 'barcode'">{{ record.barcode || '-' }}</template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changeMaterialStatus(record.id, s)"
              :disabled="!userStore.hasPerm('wms:material:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openExtras(record)">
                <template #icon><PaperClipOutlined /></template>资料
              </a-button>
              <a-button v-if="canEdit || canEditPlan" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <a-button type="link" size="small" @click="openUnits(record)">
                <template #icon><SwapOutlined /></template>单位
              </a-button>
              <a-button v-if="!record.templateId" type="link" size="small" @click="openVariants(record)">
                <template #icon><AppstoreAddOutlined /></template>多规格
              </a-button>
              <span v-perm="'wms:material:remove'">
                <a-popconfirm :title="`确定删除物料「${record.name}」？`" @confirm="remove([record.id])">
                  <a-button type="link" danger size="small">
                    <template #icon><DeleteOutlined /></template>删除
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer page :drawer="drawer" create-title="新增物料" edit-title="编辑物料" :width="canEditPlan ? 'min(860px, 100vw)' : undefined">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <div v-if="canEditPlan" class="section-title">基础信息</div>
        <a-alert v-if="!canEdit && form.id" type="info" show-icon class="tip" message="没有物料修改权限，只能维护生产计划属性" />
        <a-row :gutter="16">
          <a-col :xs="24" :sm="colSpan">
            <a-form-item label="物料编码" name="code">
              <a-input v-model:value="form.code" :maxlength="64" placeholder="如：M0001" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item label="物料名称" name="name">
              <a-input v-model:value="form.name" :maxlength="128" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item label="规格型号" name="spec">
              <a-input v-model:value="form.spec" :maxlength="128" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item label="计量单位" name="unit">
              <a-auto-complete v-model:value="form.unit" :options="UNITS" :maxlength="16" placeholder="如：个、箱、kg" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item label="分类" name="category">
              <a-select
                v-model:value="form.category"
                placeholder="请选择分类"
                allow-clear
                :options="toOptions(dict.wms_material_category)"
                :disabled="basicReadonly"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item label="安全库存" name="safetyStock" extra="库存合计低于该值时预警，MRP 计算净需求时预留">
              <a-input-number v-model:value="form.safetyStock" :min="0" :precision="4" class="full" placeholder="不预警" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item name="reorderPoint" extra="为空不参与补货建议">
              <template #label>
                再订货点
                <a-tooltip title="预计库存（可用 + 采购在途 + 在制 − 销售待发 − 工单待领料）不高于此值时出现在「补货建议」，为空不参与">
                  <QuestionCircleOutlined class="label-help" />
                </a-tooltip>
              </template>
              <a-input-number v-model:value="form.reorderPoint" :min="0" :precision="4" class="full" placeholder="不参与" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item label="再订货量" name="reorderQty" extra="经济批量，补货建议的建议数量">
              <a-input-number v-model:value="form.reorderQty" :min="0" :precision="4" class="full" placeholder="选填" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item label="条码" name="barcode" extra="扫码选择物料；辅助单位可单独设置条码">
              <a-input v-model:value="form.barcode" :maxlength="64" placeholder="选填" allow-clear :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item label="参考成本价" name="refCost" extra="按基本单位，没有实际成本时参考使用">
              <a-input-number v-model:value="form.refCost" :min="0" :precision="4" class="full" placeholder="选填" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item label="计价方法" name="costMethod" extra="月末成本核算时按此计算出库成本，修改后从下一次核算生效">
              <a-select v-model:value="form.costMethod" :options="toOptions(dict.wms_cost_method)" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item label="默认仓库" name="defaultWarehouseId">
              <WarehouseSelect v-model:value="form.defaultWarehouseId" placeholder="选填" :disabled="basicReadonly" @update:value="form.defaultLocationId = null" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item label="默认库位" name="defaultLocationId" extra="出入库单选择该仓库时自动带出">
              <LocationSelect v-model:value="form.defaultLocationId" :warehouse-id="form.defaultWarehouseId" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="追踪方式" extra="已发生库存业务的物料不能修改追踪方式">
              <a-space direction="vertical">
                <a-checkbox :checked="form.batchEnabled === 1" :disabled="basicReadonly" @change="(e) => (form.batchEnabled = e.target.checked ? 1 : 0)">
                  批次管理<span class="option-desc">：出入库须填写批次号，可记录生产日期、有效期，按批次追踪</span>
                </a-checkbox>
                <a-checkbox :checked="form.serialEnabled === 1" :disabled="basicReadonly" @change="(e) => (form.serialEnabled = e.target.checked ? 1 : 0)">
                  序列号管理<span class="option-desc">：数量为整数，出入库逐个登记序列号，追踪每一件的流转</span>
                </a-checkbox>
                <a-checkbox :checked="form.iqcRequired === 1" :disabled="basicReadonly" @change="(e) => (form.iqcRequired = e.target.checked ? 1 : 0)">
                  来料检验<span class="option-desc">：采购入库后冻结为待检，来料检验合格（或让步接收）后才能出库，不合格的等待退货</span>
                </a-checkbox>
              </a-space>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" show-count :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
        </a-row>

        <template v-if="canEditPlan">
          <div class="section-title">生产计划</div>
          <a-row :gutter="16">
            <a-col :xs="24" :sm="8">
              <a-form-item label="物料属性" name="supplyType" :extra="SUPPLY_HINTS[form.supplyType || 'AUTO']">
                <a-select v-model:value="form.supplyType" :options="supplyOptions" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="8">
              <a-form-item label="提前期" name="leadDays" extra="为空时取 BOM 或供货价格的提前期">
                <a-input-number v-model:value="form.leadDays" :min="0" :max="365" :precision="0" addon-after="天" class="full" placeholder="自动" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="8">
              <a-form-item label="计划员" name="planner">
                <a-input v-model:value="form.planner" :maxlength="64" placeholder="选填" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="8">
              <a-form-item label="最小批量" name="minLot" extra="MRP、缺料下单的建议数量不低于该值">
                <a-input-number v-model:value="form.minLot" :min="0" :precision="4" class="full" placeholder="不限制" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="8">
              <a-form-item label="批量倍数" name="lotMultiple" extra="建议数量向上取整到该值的整数倍">
                <a-input-number v-model:value="form.lotMultiple" :min="0" :precision="4" class="full" placeholder="不取整" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="8">
              <a-form-item label="允许超领" name="overIssueRate" extra="工单领料上限，超出部分须走补料申请">
                <a-input-number v-model:value="form.overIssueRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-checkbox :checked="form.fqcRequired === 1" @change="(e) => (form.fqcRequired = e.target.checked ? 1 : 0)">
                完工需成品检验<span class="option-desc">：工单完工入库的累计数量不能超过已确认的成品检验（FQC）合格数</span>
              </a-checkbox>
            </a-col>
          </a-row>

          <div class="sub-title">
            替代料
            <span class="option-desc">主料缺料时，可在「齐套分析」中把工单未领的部分按比例换成替代料</span>
          </div>
          <a-table
            row-key="key"
            size="small"
            :columns="subColumns"
            :data-source="form.substitutes"
            :pagination="false"
            :scroll="{ x: 700 }"
            :locale="{ emptyText: '没有替代料' }"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'substitute'">
                <a-form-item :name="['substitutes', index, 'substituteId']" :rules="[{ required: true, message: '请选择替代料' }]" class="cell-item">
                  <MaterialSelect
                    :value="record.substituteId"
                    :label="record.label"
                    @update:value="(v) => (record.substituteId = v)"
                    @select="(m) => (record.label = m.label)"
                  />
                </a-form-item>
              </template>
              <template v-else-if="column.key === 'ratio'">
                <a-form-item :name="['substitutes', index, 'ratio']" :rules="[{ required: true, message: '请填写比例' }]" class="cell-item">
                  <a-input-number v-model:value="record.ratio" :min="0" :step="0.5" class="full" />
                </a-form-item>
              </template>
              <template v-else-if="column.key === 'priority'">
                <a-input-number v-model:value="record.priority" :min="1" :max="99" :precision="0" class="full" />
              </template>
              <template v-else-if="column.key === 'remark'">
                <a-input v-model:value="record.remark" :maxlength="255" placeholder="适用条件" />
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" danger size="small" @click="form.substitutes.splice(index, 1)"><DeleteOutlined /></a-button>
              </template>
            </template>
          </a-table>
          <a-button type="dashed" block class="add-row" :disabled="form.supplyType === 'PHANTOM'" @click="addSubstitute">
            <template #icon><PlusOutlined /></template>添加替代料
          </a-button>
        </template>
      </a-form>
    </FormDrawer>
    <VariantDrawer v-model:open="variantOpen" :material="variantMaterial" @generated="load" />

    <!-- 详情：生产计划、被 BOM 使用、在制工单用料 -->
    <SubPage :open="detailOpen" :title="detail ? `${detail.code} ${detail.name}` : '物料'" width="min(960px, 100vw)" destroy-on-close @close="detailOpen = false">
      <template #extra>
        <a-button v-if="detail && (canEdit || canEditPlan)" type="primary" @click="drawer.openEdit({ id: detail.id })">编辑</a-button>
      </template>
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
            <a-descriptions-item label="规格 / 单位">{{ detail.spec || '-' }} / {{ detail.unit || '-' }}</a-descriptions-item>
            <a-descriptions-item label="分类"><DictTag type="wms_material_category" :value="detail.category" /></a-descriptions-item>
            <a-descriptions-item v-if="detail.barcode" label="条码">{{ detail.barcode }}</a-descriptions-item>
            <a-descriptions-item label="库存 / 安全库存">
              <span>
                <span :class="{ danger: belowSafety(detail) }">{{ formatQty(detail.onHandQty) }}</span> /
                {{ detail.safetyStock != null ? formatQty(detail.safetyStock) : '-' }} {{ detail.unit }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item label="物料属性">
              <span>
                <DictTag type="mes_supply_type" :value="detail.effectiveSupplyType" />
                <span v-if="!detail.supplyType" class="sub-text">自动</span>
              </span>
            </a-descriptions-item>
            <a-descriptions-item label="默认 BOM">{{ detail.bomVersion ? `${detail.bomVersion}（生产提前期 ${detail.bomLeadDays} 天）` : '-' }}</a-descriptions-item>
            <a-descriptions-item label="默认供应商">
              {{ detail.supplierName ? `${detail.supplierName}（采购提前期 ${detail.supplierLeadDays} 天）` : '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="提前期">{{ leadText(detail) }}</a-descriptions-item>
            <a-descriptions-item label="批量规则">{{ planRule(detail).join('，') || '-' }}</a-descriptions-item>
            <a-descriptions-item label="允许超领">{{ formatQty(detail.overIssueRate) }}%</a-descriptions-item>
            <a-descriptions-item v-if="detail.fqcRequired === 1" label="完工检验">需成品检验</a-descriptions-item>
            <a-descriptions-item v-if="detail.planner" label="计划员">{{ detail.planner }}</a-descriptions-item>
          </a-descriptions>
          <a-tabs size="small">
            <a-tab-pane v-if="canSeeKit" key="usage" :tab="`在制工单用料（${usage.length}）`">
              <a-table row-key="woMaterialId" size="small" :columns="usageColumns" :data-source="usage" :pagination="false" :scroll="{ x: 820 }">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'wo'">
                    <div>{{ record.woNo }} <DictTag type="mes_wo_status" :value="record.woStatus" /></div>
                    <div class="sub-text">{{ record.productCode }} {{ record.productName }}</div>
                  </template>
                  <template v-else-if="column.key === 'shortageQty'">
                    <span :class="{ danger: Number(record.shortageQty) > 0 }">{{ formatQty(record.shortageQty) }}</span>
                  </template>
                  <template v-else-if="QTY_KEYS.includes(column.key)">{{ formatQty(record[column.key]) }}</template>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="whereUsed" :tab="`被 BOM 使用（${detail.whereUsed.length}）`">
              <a-table row-key="bomId" size="small" :columns="whereUsedColumns" :data-source="detail.whereUsed" :pagination="false">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'product'">{{ record.productCode }} {{ record.productName }}</template>
                  <template v-else-if="column.key === 'version'">
                    {{ record.version }}
                    <a-tag v-if="record.isDefault === 1" color="blue" :bordered="false">默认</a-tag>
                    <a-tag v-if="record.bomStatus !== 1" :bordered="false">停用</a-tag>
                  </template>
                  <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }}</template>
                  <template v-else-if="column.key === 'lossRate'">{{ formatQty(record.lossRate) }}%</template>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="substitutes" :tab="`替代料（${detail.substitutes.length}）`">
              <a-table row-key="id" size="small" :columns="subViewColumns" :data-source="detail.substitutes" :pagination="false">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'substitute'">{{ record.substituteCode }} {{ record.substituteName }}</template>
                  <template v-else-if="column.key === 'ratio'">1 : {{ formatQty(record.ratio) }}</template>
                  <template v-else-if="column.key === 'onHandQty'">{{ formatQty(record.onHandQty) }} {{ record.unit }}</template>
                </template>
              </a-table>
            </a-tab-pane>
          </a-tabs>
        </template>
      </a-spin>
    </SubPage>
    <ImportModal
      v-model:open="importOpen"
      title="物料"
      :columns="importColumns"
      :importer="importMaterial"
      :sample="{ code: 'M0001', name: '螺丝', spec: 'M3*8', unit: '个', batchEnabled: '否', serialEnabled: '否', safetyStock: 100 }"
      @success="search"
    />
    <MaterialUnitsModal v-model:open="unitsOpen" :material="unitsMaterial" :editable="canEdit" />
    <RecordExtrasModal
      v-model:open="extrasOpen"
      :title="extrasTitle"
      biz-type="wms_material"
      :biz-id="extrasId"
      :editable="userStore.hasPerm('wms:material:edit')"
    />
    <LabelPrintModal v-model:open="labelOpen" type-key="label.material" :items="labelItems" />
  </div>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { AppstoreAddOutlined, BarcodeOutlined, DeleteOutlined, EditOutlined, ImportOutlined, PaperClipOutlined, PlusOutlined, QuestionCircleOutlined, SwapOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import ImportModal from '@/components/ImportModal.vue'
import RecordExtrasModal from '@/components/RecordExtrasModal.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import LabelPrintModal from '@/components/label/LabelPrintModal.vue'
import LocationSelect from '@/components/wms/LocationSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import MaterialUnitsModal from '@/components/wms/MaterialUnitsModal.vue'
import VariantDrawer from '@/components/wms/VariantDrawer.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { addMaterial, changeMaterialStatus, deleteMaterial, getMaterial, pageMaterial, updateMaterial, importMaterial } from '@/api/wms/material'
import { getMaterialPlan, kitMaterialLines, materialPlans, saveMaterialPlan } from '@/api/mes/base'
import { formatQty, rowKey } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'

/**
 * 物料管理：物料主数据只有这一份，仓库、采购、销售、生产共用。
 * 有 mes:material:edit 权限时，编辑抽屉中可维护生产计划属性和替代料（只有该权限时基础信息只读）
 */
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('wms:material:edit'))
const canEditPlan = computed(() => userStore.hasPerm('mes:material:edit'))
const canSeeKit = computed(() => userStore.hasPerm('mes:kit:list'))
const dict = useDict('sys_common_status', 'wms_material_category', 'mes_supply_type', 'mes_wo_status', 'wms_cost_method')
const UNITS = ['个', '件', '台', '箱', '包', '桶', '瓶', '袋', 'kg', 'g', 'L', 'm'].map((value) => ({ value }))
const SUPPLY_HINTS = {
  AUTO: '有启用的默认 BOM 为自制，否则为采购',
  MAKE: 'MRP 建议生成工单，按 BOM 展开子件',
  BUY: 'MRP 建议生成采购订单，即使有 BOM 也不展开',
  OUTSOURCE: '向默认供应商（加工商）下采购订单，同时展开要提供的子件',
  PHANTOM: '不入库的中间组件，需求直接传给子件，工单用料展开为子件'
}
const supplyOptions = computed(() => [{ value: '', label: '自动' }, ...toOptions(dict.mes_supply_type)])
const QTY_KEYS = ['requiredQty', 'issuedQty', 'toIssueQty', 'allocatedQty']

const belowSafety = (r) => r.safetyStock != null && Number(r.onHandQty) < Number(r.safetyStock)
const leadText = (p) => {
  if (p.leadDays != null) return `${p.leadDays} 天`
  if (p.effectiveSupplyType === 'MAKE' && p.bomLeadDays != null) return `${p.bomLeadDays} 天（BOM）`
  if (['BUY', 'OUTSOURCE'].includes(p.effectiveSupplyType) && p.supplierLeadDays != null) return `${p.supplierLeadDays} 天（供货价格）`
  return '-'
}
/** 列表中的计划参数摘要 */
const planRule = (p) =>
  [
    p.leadDays != null ? `提前期 ${p.leadDays} 天` : null,
    p.minLot != null ? `最小批量 ${formatQty(p.minLot)}` : null,
    p.lotMultiple != null ? `倍数 ${formatQty(p.lotMultiple)}` : null,
    Number(p.overIssueRate) ? `超领 ${formatQty(p.overIssueRate)}%` : null,
    p.substituteCount ? `替代料 ${p.substituteCount} 个` : null
  ].filter(Boolean)

const importOpen = ref(false)
// 辅助单位
const unitsOpen = ref(false)
const unitsMaterial = ref()
// 多规格：生成 SKU、查看库存矩阵
const variantOpen = ref(false)
const variantMaterial = ref()
function openVariants(record) {
  variantMaterial.value = { ...record }
  variantOpen.value = true
}

function openUnits(record) {
  unitsMaterial.value = record
  unitsOpen.value = true
}
// 附件、变更记录
const extrasOpen = ref(false)
const extrasId = ref()
const extrasTitle = ref('')
function openExtras(record) {
  extrasId.value = record.id
  extrasTitle.value = `${record.code} ${record.name}`
  extrasOpen.value = true
}
const importColumns = [
  { title: '物料编码', key: 'code', required: true },
  { title: '物料名称', key: 'name', required: true },
  { title: '规格型号', key: 'spec' },
  { title: '计量单位', key: 'unit' },
  { title: '分类', key: 'category', dict: 'wms_material_category' },
  { title: '批次管理', key: 'batchEnabled', type: 'bool' },
  { title: '序列号管理', key: 'serialEnabled', type: 'bool' },
  { title: '来料检验', key: 'iqcRequired', type: 'bool' },
  { title: '安全库存', key: 'safetyStock', type: 'number' },
  { title: '备注', key: 'remark' }
]

const columns = [
  { title: '物料', key: 'name', width: 200 },
  { title: '规格型号', dataIndex: 'spec', width: 150, ellipsis: true },
  { title: '单位', dataIndex: 'unit', width: 70 },
  { title: '条码', key: 'barcode', width: 140, ellipsis: true },
  { title: '分类', key: 'category', width: 100, dict: 'wms_material_category' },
  { title: '追踪方式', key: 'tracking', width: 120 },
  { title: '物料属性', key: 'supplyType', width: 90 },
  { title: '计划参数', key: 'planRule', width: 150 },
  { title: '安全库存', key: 'safetyStock', width: 100, align: 'right' },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 270, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'wms-material')
const showSearch = ref(true)

/** 分页后再查一次本页物料的计划属性 */
async function fetchPage(params) {
  const page = await pageMaterial(params)
  if (page.records.length) {
    const plans = await materialPlans(page.records.map((r) => r.id)).catch(() => [])
    const byId = Object.fromEntries(plans.map((p) => [p.id, p]))
    page.records.forEach((r) => (r.plan = byId[r.id]))
  }
  return page
}

const query = reactive({ keyword: undefined, category: undefined, status: undefined })
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(fetchPage, query)

function resetQuery() {
  Object.assign(query, { keyword: undefined, category: undefined, status: undefined })
  search()
}

onMounted(load)

// ---------------------------------------------------------------- 新增 / 编辑
const subColumns = [
  { title: '替代料', key: 'substitute', width: 260 },
  { title: '替代比例', key: 'ratio', width: 120 },
  { title: '优先级', key: 'priority', width: 90 },
  { title: '备注', key: 'remark', width: 180 },
  { title: '', key: 'action', width: 50 }
]
const rules = {
  code: [
    { required: true, message: '请输入物料编码' },
    { pattern: /^[A-Za-z0-9_.-]{1,64}$/, message: '只能包含字母、数字、下划线、点和短横线' }
  ],
  name: [{ required: true, whitespace: true, message: '请输入物料名称' }]
}
const colSpan = computed(() => (canEditPlan.value ? 8 : 12))
const basicReadonly = computed(() => !!form.value.id && !canEdit.value)
const MATERIAL_FIELDS = [
  'code',
  'name',
  'spec',
  'unit',
  'category',
  'batchEnabled',
  'serialEnabled',
  'iqcRequired',
  'safetyStock',
  'reorderPoint',
  'reorderQty',
  'barcode',
  'defaultWarehouseId',
  'defaultLocationId',
  'refCost',
  'costMethod',
  'remark'
]
const num = (v) => (v == null ? null : Number(v))

const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    code: '',
    name: '',
    spec: '',
    unit: undefined,
    category: undefined,
    batchEnabled: 0,
    serialEnabled: 0,
    iqcRequired: 0,
    safetyStock: null,
    reorderPoint: null,
    reorderQty: null,
    barcode: '',
    defaultWarehouseId: null,
    defaultLocationId: null,
    refCost: null,
    costMethod: 'WEIGHTED',
    remark: '',
    supplyType: '',
    leadDays: null,
    minLot: null,
    lotMultiple: null,
    overIssueRate: 0,
    fqcRequired: 0,
    planner: '',
    substitutes: []
  }),
  fetch: async (record) => {
    const [m, p] = await Promise.all([getMaterial(record.id), canEditPlan.value ? getMaterialPlan(record.id) : null])
    const form = { ...m, safetyStock: num(m.safetyStock), reorderPoint: num(m.reorderPoint), reorderQty: num(m.reorderQty), refCost: num(m.refCost), barcode: m.barcode || '' }
    if (p) {
      Object.assign(form, {
        supplyType: p.supplyType || '',
        leadDays: p.leadDays,
        minLot: num(p.minLot),
        lotMultiple: num(p.lotMultiple),
        overIssueRate: Number(p.overIssueRate),
        fqcRequired: p.fqcRequired || 0,
        planner: p.planner || '',
        substitutes: p.substitutes.map((s) => ({
          key: rowKey(),
          substituteId: s.substituteId,
          label: `${s.substituteCode} ${s.substituteName}`,
          ratio: Number(s.ratio),
          priority: s.priority,
          remark: s.remark || ''
        }))
      })
    }
    return form
  },
  create: async ({ material, plan }) => {
    const id = await addMaterial(material)
    if (plan) await saveMaterialPlan(id, plan)
  },
  update: async (id, { material, plan }) => {
    if (canEdit.value) await updateMaterial(id, material)
    if (plan) await saveMaterialPlan(id, plan)
  },
  toPayload: (f) => ({
    material: {
      ...Object.fromEntries(MATERIAL_FIELDS.map((k) => [k, f[k]])),
      reorderPoint: f.reorderPoint ?? null,
      reorderQty: f.reorderQty ?? null,
      refCost: f.refCost ?? null,
      barcode: f.barcode?.trim() || null,
      defaultLocationId: f.defaultWarehouseId ? f.defaultLocationId || null : null
    },
    plan: canEditPlan.value
      ? {
          supplyType: f.supplyType || null,
          leadDays: f.leadDays ?? null,
          minLot: f.minLot || null,
          lotMultiple: f.lotMultiple || null,
          overIssueRate: f.overIssueRate ?? 0,
          fqcRequired: f.fqcRequired || 0,
          planner: f.planner || null,
          substitutes:
            f.supplyType === 'PHANTOM'
              ? []
              : f.substitutes.map(({ substituteId, ratio, priority, remark }) => ({ substituteId, ratio, priority, remark: remark || null }))
        }
      : null
  }),
  onSuccess: () => {
    load()
    if (detailOpen.value && detail.value) openDetail(detail.value.id)
  }
})
const { formRef, form } = drawer

function addSubstitute() {
  form.value.substitutes.push({ key: rowKey(), substituteId: undefined, label: undefined, ratio: 1, priority: form.value.substitutes.length + 1, remark: '' })
}

async function remove(ids) {
  await deleteMaterial(ids)
  message.success('删除成功')
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
}

// ---------------------------------------------------------------- 详情
const usageColumns = [
  { title: '工单 / 产品', key: 'wo', width: 260 },
  { title: '计划开工', dataIndex: 'planStart', width: 110 },
  { title: '需求', key: 'requiredQty', width: 90, align: 'right' },
  { title: '已领', key: 'issuedQty', width: 90, align: 'right' },
  { title: '待领', key: 'toIssueQty', width: 90, align: 'right' },
  { title: '分配库存', key: 'allocatedQty', width: 90, align: 'right' },
  { title: '缺料', key: 'shortageQty', width: 90, align: 'right' }
]
const whereUsedColumns = [
  { title: '产品', key: 'product', width: 260 },
  { title: 'BOM 版本', key: 'version', width: 160 },
  { title: '用量', key: 'qty', width: 100, align: 'right' },
  { title: '损耗率', key: 'lossRate', width: 100, align: 'right' }
]
const subViewColumns = [
  { title: '优先级', dataIndex: 'priority', width: 80 },
  { title: '替代料', key: 'substitute', width: 260 },
  { title: '主料 : 替代料', key: 'ratio', width: 120 },
  { title: '库存', key: 'onHandQty', width: 120, align: 'right' },
  { title: '备注', dataIndex: 'remark', ellipsis: true }
]
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref()
const usage = ref([])

async function openDetail(id) {
  detailOpen.value = true
  detailLoading.value = true
  try {
    const [d, lines] = await Promise.all([getMaterialPlan(id), canSeeKit.value ? kitMaterialLines(id) : []])
    detail.value = d
    usage.value = lines
  } finally {
    detailLoading.value = false
  }
}

// 条码标签：选中的行（当前页）
const labelOpen = ref(false)
const labelItems = ref([])
function openLabels() {
  labelItems.value = list.value.filter((r) => selectedKeys.value.includes(r.id)).map((r) => ({ code: r.code, name: r.name, spec: r.spec, unit: r.unit, barcode: r.barcode }))
  labelOpen.value = true
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.option-desc {
  color: var(--app-text-3);
  font-weight: normal;
  font-size: 12px;
}
.full {
  width: 100%;
}
.label-help {
  margin-left: 4px;
  color: var(--app-text-3);
  cursor: help;
}
.block {
  margin-bottom: 16px;
}
.tip {
  margin-bottom: 16px;
}
.danger {
  color: var(--status-critical);
  font-weight: 500;
}
.rule-line {
  font-size: 12px;
  line-height: 18px;
}
.section-title {
  font-weight: 600;
  margin: 4px 0 12px;
  padding-left: 8px;
  border-left: 3px solid var(--app-primary);
}
.sub-title {
  font-weight: 500;
  margin-bottom: 8px;
}
.cell-item {
  margin-bottom: 0;
}
.add-row {
  margin-top: 8px;
}
.ml {
  margin-left: 6px;
}
</style>
