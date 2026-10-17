<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('wms.common.material')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('wms.flow.codeOrName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('wms.inout.category')" name="category">
          <a-select v-model:value="query.category" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wms_material_category)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
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
          <template #icon><PlusOutlined /></template>{{ $t('wms.material.createTitle') }}
        </a-button>
        <a-button v-perm="'wms:material:add'" @click="importOpen = true">
          <template #icon><ImportOutlined /></template>{{ $t('common.action.import') }}
        </a-button>
        <span v-perm="'wms:material:remove'">
          <a-popconfirm
            :title="$t('wms.material.confirmDeleteN', { n: selectedKeys.length })"
            :disabled="!selectedKeys.length"
            @confirm="remove(selectedKeys)"
          >
            <a-button danger :disabled="!selectedKeys.length">
              <template #icon><DeleteOutlined /></template>{{ $t('common.action.batchDelete') }}
            </a-button>
          </a-popconfirm>
        </span>
        <a-button :disabled="!selectedKeys.length" @click="openLabels">
          <template #icon><BarcodeOutlined /></template>{{ $t('wms.material.printLabel') }}{{ selectedKeys.length ? ` (${selectedKeys.length})` : '' }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="tableColumns"
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
            <a-tag v-if="record.isTemplate === 1" color="magenta" :bordered="false" class="ml">{{ $t('wms.material.style') }}</a-tag>
            <div class="sub-text">
              {{ record.code }}
              <template v-if="record.variantText"> · {{ record.variantText }}</template>
            </div>
          </template>
          <template v-else-if="column.key === 'category'">
            <DictTag type="wms_material_category" :value="record.category" />
          </template>
          <template v-else-if="column.key === 'tracking'">
            <a-tag v-if="record.batchEnabled === 1" color="blue" :bordered="false">{{ $t('wms.common.batch') }}</a-tag>
            <a-tag v-if="record.serialEnabled === 1" color="purple" :bordered="false">{{ $t('wms.common.serialNo') }}</a-tag>
            <a-tag v-if="record.iqcRequired === 1" color="orange" :bordered="false">{{ $t('wms.material.iqc') }}</a-tag>
            <span v-if="record.batchEnabled !== 1 && record.serialEnabled !== 1 && record.iqcRequired !== 1" class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'supplyType'">
            <template v-if="record.plan">
              <DictTag type="mes_supply_type" :value="record.plan.effectiveSupplyType" />
              <div v-if="!record.plan.supplyType" class="sub-text">{{ $t('wms.material.auto') }}</div>
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
          <template v-else-if="column.custom">{{ cfCell(record, column) }}</template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openExtras(record)">
                <template #icon><PaperClipOutlined /></template>{{ $t('wms.material.extras') }}
              </a-button>
              <a-button v-if="canEdit || canEditPlan" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <a-button type="link" size="small" @click="openUnits(record)">
                <template #icon><SwapOutlined /></template>{{ $t('common.field.unit') }}
              </a-button>
              <a-button v-if="!record.templateId" type="link" size="small" @click="openVariants(record)">
                <template #icon><AppstoreAddOutlined /></template>{{ $t('wms.variant.title') }}
              </a-button>
              <span v-perm="'wms:material:remove'">
                <a-popconfirm :title="$t('wms.material.confirmDelete', { name: record.name })" @confirm="remove([record.id])">
                  <a-button type="link" danger size="small">
                    <template #icon><DeleteOutlined /></template>{{ $t('common.action.delete') }}
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer page :drawer="drawer" :create-title="$t('wms.material.createTitle')" :edit-title="$t('wms.material.editTitle')" :width="canEditPlan ? 'min(860px, 100vw)' : undefined">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <div v-if="canEditPlan" class="section-title">{{ $t('wms.material.basic') }}</div>
        <a-alert v-if="!canEdit && form.id" type="info" show-icon class="tip" :message="$t('wms.material.planOnly')" />
        <a-row :gutter="16">
          <a-col :xs="24" :sm="colSpan">
            <a-form-item :label="$t('wms.material.code')" name="code">
              <a-input v-model:value="form.code" :maxlength="64" :placeholder="$t('wms.material.codePh')" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item :label="$t('wms.material.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="128" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item :label="$t('wms.inout.spec')" name="spec">
              <a-input v-model:value="form.spec" :maxlength="128" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item :label="$t('wms.material.unit')" name="unit">
              <a-auto-complete v-model:value="form.unit" :options="UNITS" :maxlength="16" :placeholder="$t('wms.material.unitPh')" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item :label="$t('wms.inout.category')" name="category">
              <a-select
                v-model:value="form.category"
                :placeholder="$t('common.placeholder.select')"
                allow-clear
                :options="toOptions(dict.wms_material_category)"
                :disabled="basicReadonly"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item :label="$t('wms.stock.safetyStock')" name="safetyStock" :extra="$t('wms.material.safetyTip')">
              <a-input-number v-model:value="form.safetyStock" :min="0" :precision="4" class="full" :placeholder="$t('wms.material.noAlert')" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item name="reorderPoint" :extra="$t('wms.material.reorderPointExtra')">
              <template #label>
                {{ $t('wms.material.reorderPoint') }}
                <a-tooltip :title="$t('wms.material.reorderPointTip')">
                  <QuestionCircleOutlined class="label-help" />
                </a-tooltip>
              </template>
              <a-input-number v-model:value="form.reorderPoint" :min="0" :precision="4" class="full" :placeholder="$t('wms.material.notInvolved')" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item :label="$t('wms.material.reorderQty')" name="reorderQty" :extra="$t('wms.material.reorderQtyTip')">
              <a-input-number v-model:value="form.reorderQty" :min="0" :precision="4" class="full" :placeholder="$t('wms.common.optional')" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item :label="$t('wms.units.barcode')" name="barcode" :extra="$t('wms.material.barcodeTip')">
              <a-input v-model:value="form.barcode" :maxlength="64" :placeholder="$t('wms.common.optional')" allow-clear :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item :label="$t('wms.material.refCost')" name="refCost" :extra="$t('wms.material.refCostTip')">
              <a-input-number v-model:value="form.refCost" :min="0" :precision="4" class="full" :placeholder="$t('wms.common.optional')" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item :label="$t('wms.material.costMethod')" name="costMethod" :extra="$t('wms.material.costMethodTip')">
              <a-select v-model:value="form.costMethod" :options="toOptions(dict.wms_cost_method)" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item :label="$t('wms.material.defaultWarehouse')" name="defaultWarehouseId">
              <WarehouseSelect v-model:value="form.defaultWarehouseId" :placeholder="$t('wms.common.optional')" :disabled="basicReadonly" @update:value="form.defaultLocationId = null" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="colSpan">
            <a-form-item :label="$t('wms.material.defaultLocation')" name="defaultLocationId" :extra="$t('wms.material.defaultLocationTip')">
              <LocationSelect v-model:value="form.defaultLocationId" :warehouse-id="form.defaultWarehouseId" :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('wms.material.tracking')" :extra="$t('wms.material.trackingTip')">
              <a-space direction="vertical">
                <a-checkbox :checked="form.batchEnabled === 1" :disabled="basicReadonly" @change="(e) => (form.batchEnabled = e.target.checked ? 1 : 0)">
                  {{ $t('wms.material.batchManaged') }}<span class="option-desc">{{ $t('wms.material.batchManagedDesc') }}</span>
                </a-checkbox>
                <a-checkbox :checked="form.serialEnabled === 1" :disabled="basicReadonly" @change="(e) => (form.serialEnabled = e.target.checked ? 1 : 0)">
                  {{ $t('wms.material.serialManaged') }}<span class="option-desc">{{ $t('wms.material.serialManagedDesc') }}</span>
                </a-checkbox>
                <a-checkbox :checked="form.iqcRequired === 1" :disabled="basicReadonly" @change="(e) => (form.iqcRequired = e.target.checked ? 1 : 0)">
                  {{ $t('wms.material.iqc') }}<span class="option-desc">{{ $t('wms.material.iqcDesc') }}</span>
                </a-checkbox>
              </a-space>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" show-count :disabled="basicReadonly" />
            </a-form-item>
          </a-col>
        </a-row>

        <template v-if="canEditPlan">
          <div class="section-title">{{ $t('wms.material.plan') }}</div>
          <a-row :gutter="16">
            <a-col :xs="24" :sm="8">
              <a-form-item :label="$t('wms.material.supplyType')" name="supplyType" :extra="SUPPLY_HINTS[form.supplyType || 'AUTO']">
                <a-select v-model:value="form.supplyType" :options="supplyOptions" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="8">
              <a-form-item :label="$t('wms.material.leadDays')" name="leadDays" :extra="$t('wms.material.leadDaysTip')">
                <a-input-number v-model:value="form.leadDays" :min="0" :max="365" :precision="0" :addon-after="$t('common.unit.day')" class="full" :placeholder="$t('wms.material.auto')" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="8">
              <a-form-item :label="$t('wms.material.planner')" name="planner">
                <a-input v-model:value="form.planner" :maxlength="64" :placeholder="$t('wms.common.optional')" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="8">
              <a-form-item :label="$t('wms.material.minLot')" name="minLot" :extra="$t('wms.material.minLotTip')">
                <a-input-number v-model:value="form.minLot" :min="0" :precision="4" class="full" :placeholder="$t('wms.material.noLimit')" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="8">
              <a-form-item :label="$t('wms.material.lotMultiple')" name="lotMultiple" :extra="$t('wms.material.lotMultipleTip')">
                <a-input-number v-model:value="form.lotMultiple" :min="0" :precision="4" class="full" :placeholder="$t('wms.material.noRounding')" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="8">
              <a-form-item :label="$t('wms.material.overIssue')" name="overIssueRate" :extra="$t('wms.material.overIssueTip')">
                <a-input-number v-model:value="form.overIssueRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-checkbox :checked="form.fqcRequired === 1" @change="(e) => (form.fqcRequired = e.target.checked ? 1 : 0)">
                {{ $t('wms.material.fqcRequired') }}<span class="option-desc">{{ $t('wms.material.fqcDesc') }}</span>
              </a-checkbox>
            </a-col>
          </a-row>

          <div class="sub-title">
            {{ $t('wms.material.substitute') }}
            <span class="option-desc">{{ $t('wms.material.substituteTip') }}</span>
          </div>
          <a-table
            row-key="key"
            size="small"
            :columns="subColumns"
            :data-source="form.substitutes"
            :pagination="false"
            :scroll="{ x: subScrollX }"
            :locale="{ emptyText: $t('wms.material.noSubstitute') }"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'substitute'">
                <a-form-item :name="['substitutes', index, 'substituteId']" :rules="[{ required: true, message: $t('wms.material.selectSubstitute') }]" class="cell-item">
                  <MaterialSelect
                    :value="record.substituteId"
                    :label="record.label"
                    @update:value="(v) => (record.substituteId = v)"
                    @select="(m) => (record.label = m.label)"
                  />
                </a-form-item>
              </template>
              <template v-else-if="column.key === 'ratio'">
                <a-form-item :name="['substitutes', index, 'ratio']" :rules="[{ required: true, message: $t('wms.material.inputRatio') }]" class="cell-item">
                  <a-input-number v-model:value="record.ratio" :min="0" :step="0.5" class="full" />
                </a-form-item>
              </template>
              <template v-else-if="column.key === 'priority'">
                <a-input-number v-model:value="record.priority" :min="1" :max="99" :precision="0" class="full" />
              </template>
              <template v-else-if="column.key === 'remark'">
                <a-input v-model:value="record.remark" :maxlength="255" :placeholder="$t('wms.material.condition')" />
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" danger size="small" @click="form.substitutes.splice(index, 1)"><DeleteOutlined /></a-button>
              </template>
            </template>
          </a-table>
          <a-button type="dashed" block class="add-row" :disabled="form.supplyType === SupplyType.PHANTOM" @click="addSubstitute">
            <template #icon><PlusOutlined /></template>{{ $t('wms.material.addSubstitute') }}
          </a-button>
        </template>
        <CustomFieldsForm :fields="cfFields" :values="form.cf || {}" :title="$t('wms.material.customFields')" />
      </a-form>
    </FormDrawer>
    <VariantDrawer v-model:open="variantOpen" :material="variantMaterial" @generated="load" />

    <!-- 详情：生产计划、被 BOM 使用、在制工单用料 -->
    <SubPage :open="detailOpen" :title="detail ? `${detail.code} ${detail.name}` : $t('wms.common.material')" width="min(960px, 100vw)" destroy-on-close @close="detailOpen = false">
      <template #extra>
        <a-button v-if="detail && (canEdit || canEditPlan)" type="primary" @click="drawer.openEdit({ id: detail.id })">{{ $t('common.action.edit') }}</a-button>
      </template>
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
            <a-descriptions-item :label="$t('wms.material.specUnit')">{{ detail.spec || '-' }} / {{ detail.unit || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('wms.inout.category')"><DictTag type="wms_material_category" :value="detail.category" /></a-descriptions-item>
            <a-descriptions-item v-if="detail.barcode" :label="$t('wms.units.barcode')">{{ detail.barcode }}</a-descriptions-item>
            <a-descriptions-item :label="$t('wms.material.stockSafety')">
              <span>
                <span :class="{ danger: belowSafety(detail) }">{{ formatQty(detail.onHandQty) }}</span> /
                {{ detail.safetyStock != null ? formatQty(detail.safetyStock) : '-' }} {{ detail.unit }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('wms.material.supplyType')">
              <span>
                <DictTag type="mes_supply_type" :value="detail.effectiveSupplyType" />
                <span v-if="!detail.supplyType" class="sub-text">{{ $t('wms.material.auto') }}</span>
              </span>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('wms.material.defaultBom')">{{ detail.bomVersion ? $t('wms.material.bomLead', { version: detail.bomVersion, days: detail.bomLeadDays }) : '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('wms.material.defaultSupplier')">
              {{ detail.supplierName ? $t('wms.material.supplierLead', { name: detail.supplierName, days: detail.supplierLeadDays }) : '-' }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('wms.material.leadDays')">{{ leadText(detail) }}</a-descriptions-item>
            <a-descriptions-item :label="$t('wms.material.lotRule')">{{ planRule(detail).join($t('wms.material.listSep')) || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('wms.material.overIssue')">{{ formatQty(detail.overIssueRate) }}%</a-descriptions-item>
            <a-descriptions-item v-if="detail.fqcRequired === 1" :label="$t('wms.material.fqc')">{{ $t('wms.material.fqcYes') }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.planner" :label="$t('wms.material.planner')">{{ detail.planner }}</a-descriptions-item>
          </a-descriptions>
          <a-tabs size="small">
            <a-tab-pane v-if="canSeeKit" key="usage" :tab="$t('wms.material.usageTab', { n: usage.length })">
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
            <a-tab-pane key="whereUsed" :tab="$t('wms.material.whereUsedTab', { n: detail.whereUsed.length })">
              <a-table row-key="bomId" size="small" :columns="whereUsedColumns" :data-source="detail.whereUsed" :pagination="false">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'product'">{{ record.productCode }} {{ record.productName }}</template>
                  <template v-else-if="column.key === 'version'">
                    {{ record.version }}
                    <a-tag v-if="record.isDefault === 1" color="blue" :bordered="false">{{ $t('wms.material.default') }}</a-tag>
                    <a-tag v-if="record.bomStatus !== 1" :bordered="false">{{ $t('common.status.disabled') }}</a-tag>
                  </template>
                  <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }}</template>
                  <template v-else-if="column.key === 'lossRate'">{{ formatQty(record.lossRate) }}%</template>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="substitutes" :tab="$t('wms.material.substituteTab', { n: detail.substitutes.length })">
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
      :title="$t('wms.common.material')"
      :columns="importColumns"
      :importer="importMaterial"
      :sample="{ code: 'M0001', name: $t('wms.material.sampleName'), spec: 'M3*8', unit: $t('wms.material.sampleUnit'), batchEnabled: $t('wms.material.sampleNo'), serialEnabled: $t('wms.material.sampleNo'), safetyStock: 100 }"
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
import { useI18n } from 'vue-i18n'
import { isZh } from '@/locales'
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
import CustomFieldsForm from '@/components/common/CustomFieldsForm.vue'
import { useCustomColumns, useCustomFields } from '@/composables/useCustomFields'

import { CostMethod, SupplyType } from '@/constants/enums'
const { t } = useI18n()

/**
 * 物料管理：物料主数据只有这一份，仓库、采购、销售、生产共用。
 * 有 mes:material:edit 权限时，编辑抽屉中可维护生产计划属性和替代料（只有该权限时基础信息只读）
 */
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('wms:material:edit'))
const canEditPlan = computed(() => userStore.hasPerm('mes:material:edit'))
const canSeeKit = computed(() => userStore.hasPerm('mes:kit:list'))
const dict = useDict('sys_common_status', 'wms_material_category', 'mes_supply_type', 'mes_wo_status', 'wms_cost_method')
const UNITS = (isZh() ? ['个', '件', '台', '箱', '包', '桶', '瓶', '袋', 'kg', 'g', 'L', 'm'] : ['pcs', 'set', 'box', 'pack', 'bag', 'bottle', 'drum', 'kg', 'g', 'L', 'm']).map((value) => ({ value })) // i18n-ignore
const SUPPLY_HINTS = {
  AUTO: t('wms.material.supplyHint.AUTO'),
  MAKE: t('wms.material.supplyHint.MAKE'),
  BUY: t('wms.material.supplyHint.BUY'),
  OUTSOURCE: t('wms.material.supplyHint.OUTSOURCE'),
  PHANTOM: t('wms.material.supplyHint.PHANTOM')
}
const supplyOptions = computed(() => [{ value: '', label: t('wms.material.auto') }, ...toOptions(dict.mes_supply_type)])
const QTY_KEYS = ['requiredQty', 'issuedQty', 'toIssueQty', 'allocatedQty']

const belowSafety = (r) => r.safetyStock != null && Number(r.onHandQty) < Number(r.safetyStock)
const leadText = (p) => {
  if (p.leadDays != null) return t('wms.material.days', { n: p.leadDays })
  if (p.effectiveSupplyType === SupplyType.MAKE && p.bomLeadDays != null) return t('wms.material.daysBom', { n: p.bomLeadDays })
  if ([SupplyType.BUY, SupplyType.OUTSOURCE].includes(p.effectiveSupplyType) && p.supplierLeadDays != null) return t('wms.material.daysSupplier', { n: p.supplierLeadDays })
  return '-'
}
/** 列表中的计划参数摘要 */
const planRule = (p) =>
  [
    p.leadDays != null ? t('wms.material.rule.lead', { n: p.leadDays }) : null,
    p.minLot != null ? t('wms.material.rule.minLot', { n: formatQty(p.minLot) }) : null,
    p.lotMultiple != null ? t('wms.material.rule.multiple', { n: formatQty(p.lotMultiple) }) : null,
    Number(p.overIssueRate) ? t('wms.material.rule.overIssue', { n: formatQty(p.overIssueRate) }) : null,
    p.substituteCount ? t('wms.material.rule.substitutes', { n: p.substituteCount }) : null
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
  { title: t('wms.material.code'), key: 'code', required: true },
  { title: t('wms.material.name'), key: 'name', required: true },
  { title: t('wms.inout.spec'), key: 'spec' },
  { title: t('wms.material.unit'), key: 'unit' },
  { title: t('wms.inout.category'), key: 'category', dict: 'wms_material_category' },
  { title: t('wms.material.batchManaged'), key: 'batchEnabled', type: 'bool' },
  { title: t('wms.material.serialManaged'), key: 'serialEnabled', type: 'bool' },
  { title: t('wms.material.iqc'), key: 'iqcRequired', type: 'bool' },
  { title: t('wms.stock.safetyStock'), key: 'safetyStock', type: 'number' },
  { title: t('common.field.remark'), key: 'remark' }
]

const columns = [
  { title: t('wms.common.material'), key: 'name', width: 200 },
  { title: t('wms.inout.spec'), dataIndex: 'spec', width: 150, ellipsis: true },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 70 },
  { title: t('wms.units.barcode'), key: 'barcode', width: 140, ellipsis: true },
  { title: t('wms.inout.category'), key: 'category', width: 100, dict: 'wms_material_category' },
  { title: t('wms.material.tracking'), key: 'tracking', width: 120 },
  { title: t('wms.material.supplyType'), key: 'supplyType', width: 110 },
  { title: t('wms.material.planParams'), key: 'planRule', width: 150 },
  { title: t('wms.stock.safetyStock'), key: 'safetyStock', width: 120, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 270, fixed: 'right' }
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
// 自定义字段：表单中填写，勾选「列表显示」的作为列
const cf = useCustomFields('wms_material')
const cfFields = cf.fields
const { columns: cfColumns, cell: cfCell } = useCustomColumns('wms_material', list)
const tableColumns = computed(() => {
  const cols = visibleColumns.value
  const action = cols.findIndex((c) => c.key === 'action')
  return action < 0 ? [...cols, ...cfColumns.value] : [...cols.slice(0, action), ...cfColumns.value, ...cols.slice(action)]
})

function resetQuery() {
  Object.assign(query, { keyword: undefined, category: undefined, status: undefined })
  search()
}

onMounted(load)

// ---------------------------------------------------------------- 新增 / 编辑
const subColumns = [
  { title: t('wms.material.substitute'), key: 'substitute', width: 290 },
  { title: t('wms.material.ratio'), key: 'ratio', width: 120 },
  { title: t('wms.material.priority'), key: 'priority', width: 90 },
  { title: t('common.field.remark'), key: 'remark', width: 180 },
  { title: '', key: 'action', width: 50 }
]
const subScrollX = subColumns.reduce((sum, c) => sum + c.width, 0)
const rules = {
  code: [
    { required: true, message: t('wms.material.inputCode') },
    { pattern: /^[A-Za-z0-9_.-]{1,64}$/, message: t('wms.material.codeRule') }
  ],
  name: [{ required: true, whitespace: true, message: t('wms.material.inputName') }]
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

const drawer = useFormDrawer(
  cf.wrap({
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
      costMethod: CostMethod.WEIGHTED,
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
      return id
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
              f.supplyType === SupplyType.PHANTOM
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
)
const { formRef, form } = drawer

function addSubstitute() {
  form.value.substitutes.push({ key: rowKey(), substituteId: undefined, label: undefined, ratio: 1, priority: form.value.substitutes.length + 1, remark: '' })
}

async function remove(ids) {
  await deleteMaterial(ids)
  message.success(t('common.msg.deleteSuccess'))
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
}

// ---------------------------------------------------------------- 详情
const usageColumns = [
  { title: t('wms.material.woProduct'), key: 'wo', width: 260 },
  { title: t('wms.material.planStart'), dataIndex: 'planStart', width: 110 },
  { title: t('wms.material.required'), key: 'requiredQty', width: 90, align: 'right' },
  { title: t('wms.material.issued'), key: 'issuedQty', width: 90, align: 'right' },
  { title: t('wms.material.toIssue'), key: 'toIssueQty', width: 90, align: 'right' },
  { title: t('wms.material.allocated'), key: 'allocatedQty', width: 100, align: 'right' },
  { title: t('wms.material.shortage'), key: 'shortageQty', width: 90, align: 'right' }
]
const whereUsedColumns = [
  { title: t('wms.material.product'), key: 'product', width: 260 },
  { title: t('wms.material.bomVersion'), key: 'version', width: 160 },
  { title: t('wms.material.usageQty'), key: 'qty', width: 100, align: 'right' },
  { title: t('wms.material.lossRate'), key: 'lossRate', width: 100, align: 'right' }
]
const subViewColumns = [
  { title: t('wms.material.priority'), dataIndex: 'priority', width: 80 },
  { title: t('wms.material.substitute'), key: 'substitute', width: 260 },
  { title: t('wms.material.mainToSub'), key: 'ratio', width: 140 },
  { title: t('wms.variant.stock'), key: 'onHandQty', width: 120, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
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
