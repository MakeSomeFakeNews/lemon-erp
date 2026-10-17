<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-input-search v-model:value="query.keyword" :placeholder="$t('mes.mold.keywordPh')" allow-clear class="filter-wide" @search="search" />
        <a-select v-model:value="query.moldType" :placeholder="$t('common.field.type')" allow-clear :options="toOptions(dict.mes_mold_type)" class="filter" @change="search" />
        <a-select v-model:value="query.status" :placeholder="$t('common.field.status')" allow-clear :options="toOptions(dict.mes_mold_status)" class="filter" @change="search" />
        <a-checkbox v-model:checked="query.warn" @change="search">{{ $t('mes.mold.warnOnly') }}</a-checkbox>
        <template #extra>
          <a-button v-perm="'mes:mold:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('mes.mold.add') }}
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
        :scroll="{ x: 1150, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="openDetail(record.id)">{{ record.code }} {{ record.name }}</a>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'type'"><DictTag type="mes_mold_type" :value="record.moldType" /></template>
          <template v-else-if="column.key === 'products'">
            <span v-if="record.productNames">{{ record.productNames }}</span>
            <span v-else class="sub-text">{{ $t('mes.mold.unlimited') }}</span>
          </template>
          <template v-else-if="column.key === 'life'">
            <template v-if="record.designLife">
              <a-progress :percent="lifePercent(record)" size="small" :status="lifeStatus(record)" />
              <div class="sub-text">{{ $t('mes.mold.lifeLine', { used: record.usedCount, life: record.designLife, cav: record.cavities }) }}</div>
            </template>
            <template v-else>{{ $t('mes.mold.shots', { n: record.usedCount }) }}<div class="sub-text">{{ $t('mes.mold.noLifeLine', { cav: record.cavities }) }}</div></template>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="mes_mold_status" :value="record.status" />
            <div v-if="record.woNo" class="sub-text">{{ record.woNo }}<span v-if="record.equipmentName"> · {{ record.equipmentName }}</span></div>
            <div v-else-if="record.location" class="sub-text">{{ record.location }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
              <a-button v-if="record.status === 'AVAILABLE'" v-perm="'mes:mold:issue'" type="link" size="small" @click="openAction(record, 'issue')">{{ $t('mes.mold.action.issue') }}</a-button>
              <a-button v-if="record.status === 'IN_USE'" v-perm="'mes:mold:issue'" type="link" size="small" @click="openAction(record, 'return')">{{ $t('mes.mold.action.return') }}</a-button>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('mes.mold.addTitle')" :edit-title="$t('mes.mold.editTitle')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('common.field.code')" name="code">
              <a-input v-model:value="form.code" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.type')" name="moldType">
              <a-select v-model:value="form.moldType" :options="toOptions(dict.mes_mold_type)" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.spec')" name="spec">
              <a-input v-model:value="form.spec" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.mold.cavities')" name="cavities" :extra="$t('mes.mold.cavitiesExtra')">
              <a-input-number v-model:value="form.cavities" :min="1" :precision="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.mold.designLife')" name="designLife" :extra="$t('mes.mold.designLifeExtra')">
              <a-input-number v-model:value="form.designLife" :min="1" :precision="0" :addon-after="$t('mes.mold.shotUnit')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.mold.warnRatio')" name="warnRatio" :extra="$t('mes.mold.warnRatioExtra')">
              <a-input-number v-model:value="form.warnRatio" :min="1" :max="100" addon-after="%" class="full" />
            </a-form-item>
          </a-col>
          <a-col v-if="!form.id" :span="12">
            <a-form-item :label="$t('mes.mold.usedCount')" name="usedCount" :extra="$t('mes.mold.usedCountExtra')">
              <a-input-number v-model:value="form.usedCount" :min="0" :precision="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.mold.location')" name="location">
              <a-input v-model:value="form.location" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('mes.mold.products')" name="productIds" :extra="$t('mes.mold.productsExtra')">
              <a-select
                v-model:value="form.productIds"
                mode="multiple"
                show-search
                :filter-option="false"
                :options="productOptions"
                :placeholder="$t('mes.bom.materialKeyword')"
                @search="searchProducts"
                @focus="searchProducts('')"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? `${detail.code} ${detail.name}` : ''" width="min(1000px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space wrap>
          <template v-if="detail.status === 'AVAILABLE'">
            <a-button v-perm="'mes:mold:issue'" type="primary" @click="openAction(detail, 'issue')">{{ $t('mes.mold.action.issue') }}</a-button>
            <a-button v-perm="'mes:mold:edit'" @click="openAction(detail, 'repair')">{{ $t('mes.mold.action.repair') }}</a-button>
          </template>
          <a-button v-if="detail.status === 'IN_USE'" v-perm="'mes:mold:issue'" type="primary" @click="openAction(detail, 'return')">{{ $t('mes.mold.action.return') }}</a-button>
          <a-button v-if="detail.status === 'REPAIR'" v-perm="'mes:mold:edit'" type="primary" @click="openAction(detail, 'repaired')">{{ $t('mes.mold.action.repaired') }}</a-button>
          <a-button v-if="['AVAILABLE', 'REPAIR'].includes(detail.status)" v-perm="'mes:mold:edit'" danger @click="openAction(detail, 'scrap')">{{ $t('mes.mold.action.scrap') }}</a-button>
          <a-button v-if="detail.status !== 'SCRAPPED'" v-perm="'mes:mold:edit'" @click="drawer.openEdit(detail)">{{ $t('common.action.edit') }}</a-button>
          <span v-if="detail.status !== 'IN_USE'" v-perm="'mes:mold:remove'">
            <a-popconfirm :title="$t('mes.mold.deleteConfirm')" @confirm="remove"><a-button danger>{{ $t('common.action.delete') }}</a-button></a-popconfirm>
          </span>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('common.field.type')"><DictTag type="mes_mold_type" :value="detail.moldType" /></a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="mes_mold_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.spec')">{{ detail.spec || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.mold.life')">
            <template v-if="detail.designLife">
              {{ $t('mes.mold.lifeDetail', { used: detail.usedCount, life: detail.designLife, pct: lifePercent(detail), warn: Number(detail.warnRatio) }) }}
            </template>
            <template v-else>{{ $t('mes.mold.lifeUnlimited', { used: detail.usedCount }) }}</template>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('mes.mold.cavities')">{{ detail.cavities }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.mold.location')">{{ detail.location || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.mold.currentUse')">{{ detail.woNo || '-' }}<span v-if="detail.equipmentName"> · {{ detail.equipmentName }}</span></a-descriptions-item>
          <a-descriptions-item :label="$t('mes.mold.products')" :span="2">{{ detail.productNames || $t('mes.mold.unlimited') }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title">{{ $t('mes.mold.history') }}</div>
        <a-table row-key="id" size="small" :columns="logColumns" :data-source="logs" :pagination="{ pageSize: 20 }" :scroll="{ x: 760 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'"><DictTag type="mes_mold_action" :value="record.action" /></template>
            <template v-else-if="column.key === 'wo'">
              {{ record.woNo || '-' }}
              <div v-if="record.equipmentName" class="sub-text">{{ record.equipmentName }}</div>
            </template>
            <template v-else-if="column.key === 'by'">
              {{ record.createByName }}
              <div class="sub-text">{{ record.createTime }}</div>
            </template>
          </template>
        </a-table>
      </template>
    </SubPage>

    <a-modal v-model:open="actionOpen" :title="`${ACTION_TITLES[actionForm.action]} - ${actionTarget?.code || ''} ${actionTarget?.name || ''}`"
      :confirm-loading="saving" @ok="submitAction">
      <a-form layout="vertical">
        <template v-if="actionForm.action === 'issue'">
          <a-form-item :label="$t('mes.wo.wo')" required :extra="$t('mes.mold.woExtra')">
            <a-select v-model:value="actionForm.woId" show-search :filter-option="false" :options="woOptions" :placeholder="$t('mes.mold.woSearchPh')"
              @search="searchWo" @focus="searchWo('')" />
          </a-form-item>
          <a-form-item :label="$t('mes.mold.installEquipment')">
            <a-select v-model:value="actionForm.equipmentId" allow-clear show-search option-filter-prop="label" :options="equipmentChoices" />
          </a-form-item>
        </template>
        <a-form-item :label="$t('common.field.date')" required>
          <a-date-picker v-model:value="actionForm.date" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item v-if="['return', 'repaired'].includes(actionForm.action)" :label="$t('mes.mold.location')" :extra="$t('mes.mold.locationExtra')">
          <a-input v-model:value="actionForm.location" :maxlength="64" />
        </a-form-item>
        <a-form-item :label="$t('common.field.description')">
          <a-input v-model:value="actionForm.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { addMold, deleteMold, equipmentOptions, getMold, moldAction, moldLogs, pageMold, updateMold } from '@/api/mes/base'
import { pageWo } from '@/api/mes/wo'
import { materialOptions } from '@/api/wms/material'
import { today } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

/** 模具工装：寿命按模次计，报工时按 ⌈产出 / 模穴数⌉ 累计；达到预警比例、设计寿命时提醒，达到寿命的不能再领用 */
const dict = useDict('mes_mold_type', 'mes_mold_status', 'mes_mold_action')
const { t } = useI18n()
const ACTION_TITLES = { issue: t('mes.mold.action.issue'), return: t('mes.mold.action.return'), repair: t('mes.mold.action.repair'), repaired: t('mes.mold.action.repaired'), scrap: t('mes.mold.action.scrap') }

const lifePercent = (m) => Math.min(100, Math.round((m.usedCount / m.designLife) * 1000) / 10)
const lifeStatus = (m) => (m.usedCount >= m.designLife ? 'exception' : m.usedCount * 100 >= m.designLife * Number(m.warnRatio) ? 'active' : 'normal')

const columns = [
  { title: t('mes.mold.mold'), key: 'name', width: 240 },
  { title: t('common.field.type'), key: 'type', width: 100 },
  { title: t('mes.mold.products'), key: 'products', width: 220, ellipsis: true },
  { title: t('mes.mold.life'), key: 'life', width: 230 },
  { title: t('common.field.status'), key: 'status', width: 180 },
  { title: t('common.action.operation'), key: 'action', width: 140, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'mes-mold')
const query = reactive({ keyword: undefined, moldType: undefined, status: undefined, warn: false })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageMold, query, {
  params: (q) => ({ ...q, warn: q.warn || undefined })
})
onMounted(load)

const productOptions = ref([])
let productSeq = 0
async function searchProducts(keyword) {
  const seq = ++productSeq
  const res = await materialOptions(keyword || undefined)
  if (seq !== productSeq) return
  const selected = (form.value.productIds || []).map((id) => productOptions.value.find((o) => o.value === id)).filter(Boolean)
  const fresh = res.map((m) => ({ value: m.id, label: `${m.code} ${m.name}` }))
  productOptions.value = [...selected, ...fresh.filter((o) => !selected.some((s) => s.value === o.value))]
}

const rules = {
  code: [{ required: true, message: t('mes.mold.codeRequired') }],
  name: [{ required: true, whitespace: true, message: t('mes.mold.nameRequired') }],
  moldType: [{ required: true, message: t('mes.doc.typeRequired') }],
  cavities: [{ required: true, message: t('mes.mold.cavitiesRequired') }],
  warnRatio: [{ required: true, message: t('mes.mold.warnRatioRequired') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, code: '', name: '', moldType: 'MOLD', spec: '', cavities: 1, designLife: undefined, warnRatio: 90,
    usedCount: 0, location: '', productIds: [], remark: '' }),
  fetch: async (record) => {
    const m = await getMold(record.id)
    const names = m.productNames ? m.productNames.split('、') : []
    productOptions.value = m.productIds.map((id, i) => ({ value: id, label: names[i] }))
    return { ...m, warnRatio: Number(m.warnRatio) }
  },
  create: addMold,
  update: updateMold,
  toPayload: (f) => ({ code: f.code, name: f.name, moldType: f.moldType, spec: f.spec || null, cavities: f.cavities, designLife: f.designLife ?? null,
    warnRatio: f.warnRatio, usedCount: f.usedCount ?? 0, location: f.location || null, productIds: f.productIds, remark: f.remark || null }),
  onSuccess: () => {
    load()
    if (detailOpen.value) openDetail(detail.value.id)
  }
})
const { formRef, form } = drawer

const logColumns = [
  { title: t('common.field.date'), dataIndex: 'logDate', width: 110 },
  { title: t('mes.mold.actionCol'), key: 'action', width: 100 },
  { title: t('mes.mold.woEquipment'), key: 'wo', width: 180 },
  { title: t('mes.mold.shotUnit'), dataIndex: 'shots', width: 80, align: 'right' },
  { title: t('common.field.description'), dataIndex: 'remark', ellipsis: true },
  { title: t('mes.mold.handledBy'), key: 'by', width: 160 }
]
const detailOpen = ref(false)
const detail = ref()
const logs = ref([])
async function openDetail(id) {
  const [m, l] = await Promise.all([getMold(id), moldLogs(id)])
  detail.value = m
  logs.value = l
  detailOpen.value = true
}
async function remove() {
  await deleteMold(detail.value.id)
  message.success(t('common.msg.deleted'))
  detailOpen.value = false
  load()
}

const equipments = ref([])
const equipmentChoices = computed(() => equipments.value.map((e) => ({ value: e.id, label: `${e.code} ${e.name}` })))
const woOptions = ref([])
let woSeq = 0
async function searchWo(keyword) {
  const seq = ++woSeq
  const pages = await Promise.all(['RELEASED', 'STARTED'].map((status) => pageWo({ current: 1, size: 20, status, woNo: keyword || undefined })))
  if (seq !== woSeq) return
  woOptions.value = pages.flatMap((p) => p.records).map((w) => ({ value: w.id, label: `${w.woNo} ${w.materialCode} ${w.materialName}` }))
}

const actionOpen = ref(false)
const actionTarget = ref()
const saving = ref(false)
const actionForm = reactive({})
async function openAction(record, action) {
  actionTarget.value = record
  Object.assign(actionForm, { action, woId: undefined, equipmentId: undefined, date: today(), location: '', remark: '' })
  if (action === 'issue' && !equipments.value.length) equipments.value = await equipmentOptions()
  actionOpen.value = true
}
async function submitAction() {
  if (actionForm.action === 'issue' && !actionForm.woId) return message.warning(t('mes.supplementForm.woRequired'))
  saving.value = true
  try {
    const { action, ...data } = actionForm
    await moldAction(actionTarget.value.id, action, { ...data, woId: data.woId ?? null, equipmentId: data.equipmentId ?? null,
      location: data.location || null, remark: data.remark || null })
    message.success(t('mes.mold.done.' + action))
    actionOpen.value = false
    load()
    if (detailOpen.value) openDetail(actionTarget.value.id)
  } finally {
    saving.value = false
  }
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
