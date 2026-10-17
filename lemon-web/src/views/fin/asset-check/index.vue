<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" :placeholder="$t('fin.assetCheck.allStatus')" allow-clear :options="toOptions(dict.fin_asset_check_status)" class="filter" @change="search" />
        <template #extra>
          <a-button v-perm="'fin:assetCheck:add'" type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>{{ $t('fin.assetCheck.newCheck') }}
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
        :scroll="{ x: 1000, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'checkNo'">
            <a @click="openDetail(record.id)">{{ record.checkNo }}</a>
            <div class="sub-text">{{ record.checkDate }}</div>
          </template>
          <template v-else-if="column.key === 'scope'">{{ scopeText(record) }}</template>
          <template v-else-if="column.key === 'progress'">
            <a-progress :percent="record.total ? Math.round((record.checked / record.total) * 100) : 0" size="small" />
            <div class="sub-text">{{ record.checked }} / {{ record.total }}</div>
          </template>
          <template v-else-if="column.key === 'diff'">
            <a-space :size="4" wrap>
              <a-tag v-if="record.movedCount" color="orange">{{ $t('fin.assetCheck.movedN', { n: record.movedCount }) }}</a-tag>
              <a-tag v-if="record.lossCount" color="red">{{ $t('fin.assetCheck.lossN', { n: record.lossCount }) }}</a-tag>
              <a-tag v-if="record.gainCount" color="purple">{{ $t('fin.assetCheck.gainN', { n: record.gainCount }) }}</a-tag>
              <span v-if="!record.movedCount && !record.lossCount && !record.gainCount" class="sub-text">-</span>
            </a-space>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="fin_asset_check_status" :value="record.status" />
            <div v-if="record.finishTime" class="sub-text">{{ record.finishTime }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ record.status === 'CHECKING' ? $t('fin.assetCheck.check') : $t('common.action.view') }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="createOpen" :title="$t('fin.assetCheck.createTitle')" :confirm-loading="saving" @ok="submitCreate">
      <a-alert type="info" show-icon class="block" :message="$t('fin.assetCheck.createTip')" />
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('fin.assetCheck.checkDate')" required>
              <a-date-picker v-model:value="createForm.checkDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('fin.assetCheck.useDept')">
              <DeptSelect v-model:value="createForm.deptId" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('fin.assetCheck.location')" :extra="$t('fin.assetCheck.locationExtra')">
              <a-input v-model:value="createForm.location" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')">
              <a-input v-model:value="createForm.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <SubPage :open="detailOpen" :title="detail ? $t('fin.assetCheck.detailTitle', { no: detail.checkNo }) : ''" width="min(1100px, 100vw)" @close="detailOpen = false">
      <template v-if="detail && checking" #extra>
        <a-space>
          <span v-perm="'fin:assetCheck:remove'">
            <a-popconfirm :title="$t('fin.assetCheck.confirmDelete')" @confirm="remove"><a-button danger>{{ $t('common.action.delete') }}</a-button></a-popconfirm>
          </span>
          <a-button v-perm="'fin:assetCheck:edit'" @click="openGain">{{ $t('fin.assetCheck.registerGain') }}</a-button>
          <span v-perm="'fin:assetCheck:edit'">
            <a-popconfirm :title="finishTip" @confirm="finish">
              <a-button type="primary">{{ $t('fin.assetCheck.finish') }}</a-button>
            </a-popconfirm>
          </span>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('fin.assetCheck.checkDate')">{{ detail.checkDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.assetCheck.scope')">{{ scopeText(detail) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="fin_asset_check_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('fin.assetCheck.progress')">{{ detail.checked }} / {{ detail.total }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.assetCheck.diff')">
            {{ $t('fin.assetCheck.diffSummary', { moved: detail.movedCount, loss: detail.lossCount, gain: detail.gainCount }) }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('fin.assetCheck.createdBy')">{{ detail.createByName }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="toolbar">
          <a-radio-group v-model:value="filter" button-style="solid" size="small">
            <a-radio-button value="">{{ $t('common.status.all') }}</a-radio-button>
            <a-radio-button v-for="o in dict.fin_asset_check_result" :key="o.value" :value="o.value">{{ o.label }}</a-radio-button>
          </a-radio-group>
          <span v-if="checking" class="sub-text">{{ $t('fin.assetCheck.checkingHint') }}</span>
        </div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="items" :pagination="false" :scroll="{ x: 1050 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'asset'">
              <template v-if="record.assetId">
                {{ record.assetName }}
                <div class="sub-text">{{ record.assetCode }} {{ record.model }}</div>
              </template>
              <template v-else>{{ record.gainName }}<div class="sub-text">{{ $t('fin.assetCheck.offBook') }}</div></template>
            </template>
            <template v-else-if="column.key === 'book'">
              <template v-if="record.assetId">
                {{ record.bookDeptName || '-' }}<span v-if="record.bookUserName"> · {{ record.bookUserName }}</span>
                <div class="sub-text">{{ record.bookLocation }}</div>
              </template>
              <span v-else class="sub-text">-</span>
            </template>
            <template v-else-if="column.key === 'result'"><DictTag type="fin_asset_check_result" :value="record.result" /></template>
            <template v-else-if="column.key === 'actual'">
              <template v-if="record.result === 'MOVED'">
                {{ record.actualDeptName || '-' }}<span v-if="record.actualUserName"> · {{ record.actualUserName }}</span>
                <div class="sub-text">{{ record.actualLocation }}</div>
              </template>
              <template v-else-if="record.result === 'GAIN' && record.actualLocation">{{ record.actualLocation }}</template>
              <span v-else class="sub-text">-</span>
              <div v-if="record.remark" class="sub-text">{{ record.remark }}</div>
            </template>
            <template v-else-if="column.key === 'action'">
              <span v-if="checking" v-perm="'fin:assetCheck:edit'" class="table-actions">
                <template v-if="record.assetId">
                  <a-button type="link" size="small" :disabled="record.result === 'NORMAL'" @click="mark(record, 'NORMAL')">{{ $t('fin.assetCheck.normal') }}</a-button>
                  <a-button type="link" size="small" @click="openRecord(record, 'MOVED')">{{ $t('fin.assetCheck.moved') }}</a-button>
                  <a-button type="link" size="small" danger @click="openRecord(record, 'LOSS')">{{ $t('fin.assetCheck.loss') }}</a-button>
                </template>
                <a-popconfirm v-else :title="$t('fin.assetCheck.confirmDeleteGain')" @confirm="removeGain(record)">
                  <a-button type="link" size="small" danger>{{ $t('common.action.delete') }}</a-button>
                </a-popconfirm>
              </span>
            </template>
          </template>
        </a-table>
      </template>
    </SubPage>

    <a-modal v-model:open="recordOpen" :title="`${recordForm.result === 'MOVED' ? $t('fin.assetCheck.moved') : $t('fin.assetCheck.loss')} - ${recordTarget?.assetName || ''}`" @ok="submitRecord">
      <a-form layout="vertical">
        <a-row v-if="recordForm.result === 'MOVED'" :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('fin.assetCheck.actualDept')">
              <DeptSelect v-model:value="recordForm.actualDeptId" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('fin.assetCheck.actualUser')">
              <IdentityUserSelect v-model:value="recordForm.actualUserId" :label="recordForm.actualUserName" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('fin.assetCheck.actualLocation')">
              <a-input v-model:value="recordForm.actualLocation" :maxlength="128" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('common.field.description')">
          <a-input v-model:value="recordForm.remark" :maxlength="255" :placeholder="recordForm.result === 'LOSS' ? $t('fin.assetCheck.lossPlaceholder') : ''" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="gainOpen" :title="$t('fin.assetCheck.registerGain')" @ok="submitGain">
      <a-alert type="info" show-icon class="block" :message="$t('fin.assetCheck.gainTip')" />
      <a-form layout="vertical">
        <a-form-item :label="$t('fin.assetCheck.assetName')" required>
          <a-input v-model:value="gainForm.name" :maxlength="64" />
        </a-form-item>
        <a-form-item :label="$t('fin.assetCheck.location')">
          <a-input v-model:value="gainForm.location" :maxlength="128" />
        </a-form-item>
        <a-form-item :label="$t('common.field.description')">
          <a-input v-model:value="gainForm.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import DeptSelect from '@/components/fin/DeptSelect.vue'
import { addAssetCheck, addAssetGain, deleteAssetCheck, finishAssetCheck, getAssetCheck, pageAssetCheck, recordAssetCheck, removeAssetGain } from '@/api/fin'
import { today } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useI18n } from 'vue-i18n'

/** 资产盘点：按部门、存放地点生成清单，逐项录入结果；完成时更新位置变化的资产、处置盘亏的资产 */
const { t } = useI18n()
const dict = useDict('fin_asset_check_status', 'fin_asset_check_result')

const columns = [
  { title: t('fin.assetCheck.checkNo'), key: 'checkNo', width: 180 },
  { title: t('fin.assetCheck.scope'), key: 'scope', width: 220 },
  { title: t('fin.assetCheck.progress'), key: 'progress', width: 160 },
  { title: t('fin.assetCheck.diff'), key: 'diff', width: 240 },
  { title: t('common.field.status'), key: 'status', width: 160 },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'fin-asset-check')
const query = reactive({ status: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageAssetCheck, query)
onMounted(load)

const scopeText = (c) =>
  [c.deptName, c.location && t('fin.assetCheck.locationContains', { location: c.location })].filter(Boolean).join(t('fin.assetCheck.sep')) ||
  t('fin.assetCheck.allAssets')

const createOpen = ref(false)
const saving = ref(false)
const createForm = reactive({ checkDate: undefined, deptId: undefined, location: '', remark: '' })
function openCreate() {
  Object.assign(createForm, { checkDate: today(), deptId: undefined, location: '', remark: '' })
  createOpen.value = true
}
async function submitCreate() {
  if (!createForm.checkDate) return message.warning(t('fin.assetCheck.checkDateRequired'))
  saving.value = true
  try {
    const id = await addAssetCheck({ ...createForm, location: createForm.location || null, remark: createForm.remark || null })
    createOpen.value = false
    load()
    openDetail(id)
  } finally {
    saving.value = false
  }
}

const itemColumns = [
  { title: t('fin.assetCheck.asset'), key: 'asset', width: 220 },
  { title: t('fin.assetCheck.bookInfo'), key: 'book', width: 220 },
  { title: t('fin.assetCheck.result'), key: 'result', width: 120 },
  { title: t('fin.assetCheck.actual'), key: 'actual', width: 220 },
  { title: t('fin.assetCheck.checkBy'), dataIndex: 'checkByName', width: 110 },
  { title: t('common.action.operation'), key: 'action', width: 240, fixed: 'right' }
]
const detailOpen = ref(false)
const detail = ref()
const filter = ref('')
const checking = computed(() => detail.value?.status === 'CHECKING')
const items = computed(() => (detail.value?.items || []).filter((i) => !filter.value || i.result === filter.value))
const finishTip = computed(() => {
  const d = detail.value
  if (!d) return ''
  return d.lossCount ? t('fin.assetCheck.finishLossTip', { n: d.lossCount }) : t('fin.assetCheck.finishTip')
})
async function openDetail(id) {
  detail.value = await getAssetCheck(id)
  detailOpen.value = true
}
async function refresh() {
  await openDetail(detail.value.id)
  load()
}
async function mark(item, result) {
  await recordAssetCheck(detail.value.id, item.id, { result })
  refresh()
}

const recordOpen = ref(false)
const recordTarget = ref()
const recordForm = reactive({})
function openRecord(item, result) {
  recordTarget.value = item
  const moved = item.result === 'MOVED'
  Object.assign(recordForm, {
    result,
    actualDeptId: moved ? item.actualDeptId : item.bookDeptId,
    actualUserId: moved ? item.actualUserId : item.bookUserId,
    actualUserName: moved ? item.actualUserName : item.bookUserName,
    actualLocation: (moved ? item.actualLocation : item.bookLocation) || '',
    remark: item.remark || ''
  })
  recordOpen.value = true
}
async function submitRecord() {
  const { actualUserName, ...data } = recordForm
  if (data.result === 'LOSS') Object.assign(data, { actualDeptId: null, actualUserId: null, actualLocation: null })
  await recordAssetCheck(detail.value.id, recordTarget.value.id, { ...data, remark: data.remark || null })
  recordOpen.value = false
  refresh()
}

const gainOpen = ref(false)
const gainForm = reactive({ name: '', location: '', remark: '' })
function openGain() {
  Object.assign(gainForm, { name: '', location: detail.value.location || '', remark: '' })
  gainOpen.value = true
}
async function submitGain() {
  if (!gainForm.name.trim()) return message.warning(t('fin.assetCheck.assetNameRequired'))
  await addAssetGain(detail.value.id, { ...gainForm, location: gainForm.location || null, remark: gainForm.remark || null })
  gainOpen.value = false
  refresh()
}
async function removeGain(item) {
  await removeAssetGain(detail.value.id, item.id)
  refresh()
}

async function finish() {
  const disposed = await finishAssetCheck(detail.value.id)
  message.success(disposed ? t('fin.assetCheck.finishedDisposed', { n: disposed }) : t('fin.assetCheck.finished'))
  refresh()
}
async function remove() {
  await deleteAssetCheck(detail.value.id)
  message.success(t('common.msg.deleted'))
  detailOpen.value = false
  load()
}
</script>

<style scoped>
.filter {
  width: 160px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
