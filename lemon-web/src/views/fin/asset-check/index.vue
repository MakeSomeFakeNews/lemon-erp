<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" placeholder="全部状态" allow-clear :options="toOptions(dict.fin_asset_check_status)" class="filter" @change="search" />
        <template #extra>
          <a-button v-perm="'fin:assetCheck:add'" type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>新建盘点
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
              <a-tag v-if="record.movedCount" color="orange">位置变化 {{ record.movedCount }}</a-tag>
              <a-tag v-if="record.lossCount" color="red">盘亏 {{ record.lossCount }}</a-tag>
              <a-tag v-if="record.gainCount" color="purple">盘盈 {{ record.gainCount }}</a-tag>
              <span v-if="!record.movedCount && !record.lossCount && !record.gainCount" class="sub-text">-</span>
            </a-space>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="fin_asset_check_status" :value="record.status" />
            <div v-if="record.finishTime" class="sub-text">{{ record.finishTime }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ record.status === 'CHECKING' ? '盘点' : '查看' }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="createOpen" title="新建资产盘点" :confirm-loading="saving" @ok="submitCreate">
      <a-alert type="info" show-icon class="block" message="按使用部门、存放地点生成盘点清单（包含所有未处置的资产），都不填时盘点全部资产" />
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="盘点日期" required>
              <a-date-picker v-model:value="createForm.checkDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="使用部门">
              <DeptSelect v-model:value="createForm.deptId" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="存放地点" extra="模糊匹配，如「3 楼」">
              <a-input v-model:value="createForm.location" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注">
              <a-input v-model:value="createForm.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <SubPage :open="detailOpen" :title="detail ? `资产盘点 ${detail.checkNo}` : ''" width="min(1100px, 100vw)" @close="detailOpen = false">
      <template v-if="detail && checking" #extra>
        <a-space>
          <span v-perm="'fin:assetCheck:remove'">
            <a-popconfirm title="删除该盘点单？" @confirm="remove"><a-button danger>删除</a-button></a-popconfirm>
          </span>
          <a-button v-perm="'fin:assetCheck:edit'" @click="openGain">登记盘盈</a-button>
          <span v-perm="'fin:assetCheck:edit'">
            <a-popconfirm :title="finishTip" @confirm="finish">
              <a-button type="primary">完成盘点</a-button>
            </a-popconfirm>
          </span>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="盘点日期">{{ detail.checkDate }}</a-descriptions-item>
          <a-descriptions-item label="范围">{{ scopeText(detail) }}</a-descriptions-item>
          <a-descriptions-item label="状态"><DictTag type="fin_asset_check_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item label="进度">{{ detail.checked }} / {{ detail.total }}</a-descriptions-item>
          <a-descriptions-item label="差异">
            位置变化 {{ detail.movedCount }}，盘亏 {{ detail.lossCount }}，盘盈 {{ detail.gainCount }}
          </a-descriptions-item>
          <a-descriptions-item label="创建">{{ detail.createByName }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="toolbar">
          <a-radio-group v-model:value="filter" button-style="solid" size="small">
            <a-radio-button value="">全部</a-radio-button>
            <a-radio-button v-for="o in dict.fin_asset_check_result" :key="o.value" :value="o.value">{{ o.label }}</a-radio-button>
          </a-radio-group>
          <span v-if="checking" class="sub-text">盘亏的资产在完成盘点时按净值处置；位置变化的资产更新为实际的部门、使用人和存放地点</span>
        </div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="items" :pagination="false" :scroll="{ x: 1050 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'asset'">
              <template v-if="record.assetId">
                {{ record.assetName }}
                <div class="sub-text">{{ record.assetCode }} {{ record.model }}</div>
              </template>
              <template v-else>{{ record.gainName }}<div class="sub-text">账外资产</div></template>
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
                  <a-button type="link" size="small" :disabled="record.result === 'NORMAL'" @click="mark(record, 'NORMAL')">相符</a-button>
                  <a-button type="link" size="small" @click="openRecord(record, 'MOVED')">位置变化</a-button>
                  <a-button type="link" size="small" danger @click="openRecord(record, 'LOSS')">盘亏</a-button>
                </template>
                <a-popconfirm v-else title="删除该盘盈记录？" @confirm="removeGain(record)">
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </span>
            </template>
          </template>
        </a-table>
      </template>
    </SubPage>

    <a-modal v-model:open="recordOpen" :title="`${recordForm.result === 'MOVED' ? '位置变化' : '盘亏'} - ${recordTarget?.assetName || ''}`" @ok="submitRecord">
      <a-form layout="vertical">
        <a-row v-if="recordForm.result === 'MOVED'" :gutter="16">
          <a-col :span="12">
            <a-form-item label="实际使用部门">
              <DeptSelect v-model:value="recordForm.actualDeptId" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="实际使用人">
              <IdentityUserSelect v-model:value="recordForm.actualUserId" :label="recordForm.actualUserName" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="实际存放地点">
              <a-input v-model:value="recordForm.actualLocation" :maxlength="128" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="说明">
          <a-input v-model:value="recordForm.remark" :maxlength="255" :placeholder="recordForm.result === 'LOSS' ? '如：未找到、已损坏丢弃' : ''" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="gainOpen" title="登记盘盈" @ok="submitGain">
      <a-alert type="info" show-icon class="block" message="账外资产：完成盘点后在「固定资产」中登记资产卡片" />
      <a-form layout="vertical">
        <a-form-item label="资产名称" required>
          <a-input v-model:value="gainForm.name" :maxlength="64" />
        </a-form-item>
        <a-form-item label="存放地点">
          <a-input v-model:value="gainForm.location" :maxlength="128" />
        </a-form-item>
        <a-form-item label="说明">
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

/** 资产盘点：按部门、存放地点生成清单，逐项录入结果；完成时更新位置变化的资产、处置盘亏的资产 */
const dict = useDict('fin_asset_check_status', 'fin_asset_check_result')

const columns = [
  { title: '盘点单号', key: 'checkNo', width: 180 },
  { title: '范围', key: 'scope', width: 220 },
  { title: '进度', key: 'progress', width: 160 },
  { title: '差异', key: 'diff', width: 240 },
  { title: '状态', key: 'status', width: 160 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'fin-asset-check')
const query = reactive({ status: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageAssetCheck, query)
onMounted(load)

const scopeText = (c) => [c.deptName, c.location && `地点含「${c.location}」`].filter(Boolean).join('，') || '全部资产'

const createOpen = ref(false)
const saving = ref(false)
const createForm = reactive({ checkDate: undefined, deptId: undefined, location: '', remark: '' })
function openCreate() {
  Object.assign(createForm, { checkDate: today(), deptId: undefined, location: '', remark: '' })
  createOpen.value = true
}
async function submitCreate() {
  if (!createForm.checkDate) return message.warning('请选择盘点日期')
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
  { title: '资产', key: 'asset', width: 220 },
  { title: '账面部门 / 人 / 地点', key: 'book', width: 220 },
  { title: '盘点结果', key: 'result', width: 100 },
  { title: '实际', key: 'actual', width: 220 },
  { title: '盘点人', dataIndex: 'checkByName', width: 100 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]
const detailOpen = ref(false)
const detail = ref()
const filter = ref('')
const checking = computed(() => detail.value?.status === 'CHECKING')
const items = computed(() => (detail.value?.items || []).filter((i) => !filter.value || i.result === filter.value))
const finishTip = computed(() => {
  const d = detail.value
  if (!d) return ''
  return d.lossCount ? `完成后 ${d.lossCount} 项盘亏资产将按净值处置（生成处置凭证），确定？` : '完成后不能再修改，确定？'
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
  if (!gainForm.name.trim()) return message.warning('请填写资产名称')
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
  message.success(disposed ? `盘点完成，已处置 ${disposed} 项盘亏资产` : '盘点完成')
  refresh()
}
async function remove() {
  await deleteAssetCheck(detail.value.id)
  message.success('已删除')
  detailOpen.value = false
  load()
}
</script>

<style scoped>
.filter {
  width: 140px;
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
