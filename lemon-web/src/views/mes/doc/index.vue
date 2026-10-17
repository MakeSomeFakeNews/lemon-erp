<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.docType" :placeholder="$t('mes.doc.docType')" allow-clear :options="toOptions(dict.mes_doc_type)" class="filter" @change="search" />
        <a-select v-model:value="query.status" :placeholder="$t('mes.doc.allStatus')" allow-clear :options="toOptions(dict.mes_doc_status)" class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('mes.doc.keywordPh')" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'mes:doc:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('mes.doc.add') }}
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
        :scroll="{ x: 1050, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="openDetail(record.id)">{{ record.name }}</a>
            <div class="sub-text">{{ $t('mes.doc.fileCount', { no: record.docNo, n: record.fileCount }) }}</div>
          </template>
          <template v-else-if="column.key === 'type'"><DictTag type="mes_doc_type" :value="record.docType" /></template>
          <template v-else-if="column.key === 'material'">
            <span v-if="record.materialId">{{ record.materialCode }} {{ record.materialName }}</span>
            <span v-else class="sub-text">{{ $t('mes.doc.general') }}</span>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="mes_doc_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'release'">
            {{ record.releaseTime || '-' }}
            <div class="sub-text">{{ record.releaseByName }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('mes.doc.addTitle')" :edit-title="$t('mes.doc.editTitle')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('mes.doc.name')" name="name">
          <a-input v-model:value="form.name" :maxlength="128" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('common.field.type')" name="docType">
              <a-select v-model:value="form.docType" :options="toOptions(dict.mes_doc_type)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.doc.version')" name="version">
              <a-input v-model:value="form.version" :maxlength="16" :placeholder="$t('mes.doc.versionPh')" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('mes.doc.material')" name="materialId" :extra="$t('mes.doc.materialExtra')">
          <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" />
        </a-form-item>
        <a-form-item :label="$t('common.field.description')" name="description">
          <a-textarea v-model:value="form.description" :rows="3" :maxlength="500" />
        </a-form-item>
      </a-form>
      <a-alert v-if="!form.id" type="info" show-icon :message="$t('mes.doc.saveTip')" />
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? `${detail.name} ${detail.version}` : ''" width="min(900px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space>
          <template v-if="detail.status === 'DRAFT'">
            <a-button v-perm="'mes:doc:edit'" @click="drawer.openEdit(detail)">{{ $t('common.action.edit') }}</a-button>
            <span v-perm="'mes:doc:remove'">
              <a-popconfirm :title="$t('mes.doc.deleteVersion')" @confirm="remove"><a-button danger>{{ $t('common.action.delete') }}</a-button></a-popconfirm>
            </span>
            <span v-perm="'mes:doc:release'">
              <a-popconfirm :title="detail.prevId ? $t('mes.doc.releaseConfirmPrev') : $t('mes.doc.releaseConfirm')" @confirm="release">
                <a-button type="primary">{{ $t('mes.doc.release') }}</a-button>
              </a-popconfirm>
            </span>
          </template>
          <a-button v-if="detail.status === 'RELEASED' && isLatest" v-perm="'mes:doc:add'" type="primary" @click="openRevise">{{ $t('mes.doc.revise') }}</a-button>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('mes.doc.docNo')">{{ detail.docNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.type')"><DictTag type="mes_doc_type" :value="detail.docType" /></a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="mes_doc_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('mes.doc.material')">{{ detail.materialId ? `${detail.materialCode} ${detail.materialName}` : $t('mes.doc.general') }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.doc.release')">{{ detail.releaseTime || '-' }} {{ detail.releaseByName || '' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.doc.ecn')">{{ detail.ecnNo || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.description" :label="$t('common.field.description')" :span="3">{{ detail.description }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title">{{ $t('mes.doc.files') }}</div>
        <AttachmentPanel biz-type="mes_doc" :biz-id="detail.id" :editable="detail.status === 'DRAFT' && userStore.hasPerm('mes:doc:edit')" />
        <div class="section-title">{{ $t('mes.doc.versions') }}</div>
        <a-table row-key="id" size="small" :columns="versionColumns" :data-source="versions" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'version'">
              <a v-if="record.id !== detail.id" @click="openDetail(record.id)">{{ record.version }}</a>
              <b v-else>{{ record.version }}</b>
            </template>
            <template v-else-if="column.key === 'status'"><DictTag type="mes_doc_status" :value="record.status" /></template>
          </template>
        </a-table>
      </template>
    </SubPage>

    <a-modal v-model:open="reviseOpen" :title="$t('mes.doc.revise')" @ok="submitRevise">
      <a-form layout="vertical">
        <a-form-item :label="$t('mes.doc.newVersion')" required>
          <a-input v-model:value="revise.version" :maxlength="16" />
        </a-form-item>
        <a-form-item :label="$t('mes.doc.changes')">
          <a-textarea v-model:value="revise.description" :rows="3" :maxlength="500" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { addDoc, deleteDoc, docVersions, getDoc, pageDoc, releaseDoc, reviseDoc, updateDoc } from '@/api/mes/plm'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useUserStore } from '@/store/user'

/** 技术文档：图纸、规格书、作业指导书等，按版本发布，发布新版本时旧版本作废 */
const dict = useDict('mes_doc_type', 'mes_doc_status')
const userStore = useUserStore()
const route = useRoute()
const { t } = useI18n()

const columns = [
  { title: t('mes.doc.doc'), key: 'name', width: 260 },
  { title: t('common.field.type'), key: 'type', width: 110 },
  { title: t('mes.doc.version'), dataIndex: 'version', width: 80 },
  { title: t('mes.doc.material'), key: 'material', width: 220 },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('mes.doc.release'), key: 'release', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 80, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'mes-doc')
const query = reactive({ docType: undefined, status: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageDoc, query)
onMounted(() => {
  load()
  if (route.query.id) openDetail(Number(route.query.id))
})

const rules = {
  name: [{ required: true, whitespace: true, message: t('mes.doc.nameRequired') }],
  docType: [{ required: true, message: t('mes.doc.typeRequired') }],
  version: [{ required: true, whitespace: true, message: t('mes.doc.versionRequired') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', docType: 'DRAWING', version: 'A', materialId: undefined, description: '' }),
  fetch: async (record) => {
    const d = await getDoc(record.id)
    return { ...d, materialLabel: d.materialId ? `${d.materialCode} ${d.materialName}` : undefined }
  },
  create: async (data) => {
    const id = await addDoc(data)
    openDetail(id)
  },
  update: updateDoc,
  toPayload: (f) => ({ name: f.name, docType: f.docType, version: f.version, materialId: f.materialId ?? null, ecnId: f.ecnId ?? null,
    description: f.description || null }),
  onSuccess: () => {
    load()
    if (detailOpen.value) refreshDetail()
  }
})
const { formRef, form } = drawer

const versionColumns = [
  { title: t('mes.doc.version'), key: 'version', width: 80 },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('mes.doc.releaseTime'), dataIndex: 'releaseTime', width: 170 },
  { title: t('mes.doc.releaseBy'), dataIndex: 'releaseByName', width: 110 },
  { title: t('common.field.description'), dataIndex: 'description', ellipsis: true }
]
const detailOpen = ref(false)
const detail = ref()
const versions = ref([])
const isLatest = computed(() => versions.value[0]?.id === detail.value?.id)
async function openDetail(id) {
  const [d, v] = await Promise.all([getDoc(id), docVersions(id)])
  detail.value = d
  versions.value = v
  detailOpen.value = true
}
async function refreshDetail() {
  if (detail.value) await openDetail(detail.value.id)
}
async function release() {
  await releaseDoc(detail.value.id)
  message.success(t('mes.doc.released'))
  refreshDetail()
  load()
}
async function remove() {
  await deleteDoc(detail.value.id)
  message.success(t('common.msg.deleted'))
  detailOpen.value = false
  load()
}
const reviseOpen = ref(false)
const revise = ref({})
function openRevise() {
  const v = detail.value.version
  const next = /^[A-Y]$/.test(v) ? String.fromCharCode(v.charCodeAt(0) + 1) : (/^(.*?)(\d+)$/.exec(v) ? v.replace(/(\d+)$/, (n) => Number(n) + 1) : '')
  revise.value = { version: next, description: '' }
  reviseOpen.value = true
}
async function submitRevise() {
  if (!revise.value.version?.trim()) return message.warning(t('mes.doc.newVersionRequired'))
  const id = await reviseDoc(detail.value.id, revise.value)
  message.success(t('mes.doc.revised'))
  reviseOpen.value = false
  load()
  openDetail(id)
}
</script>

<style scoped>
.filter {
  width: 130px;
}
.filter-wide {
  width: 220px;
}
.block {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
</style>
