<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('hr.position.position')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('hr.position.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('hr.position.category')" name="category">
          <a-select v-model:value="query.category" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.hr_position_category)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'hr:position:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('hr.position.add') }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 900, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div>{{ record.name }}</div>
            <div class="sub-text">{{ record.code }}</div>
          </template>
          <template v-else-if="column.key === 'headcount'">
            <span :class="{ over: record.headcount != null && record.employeeCount > record.headcount }">{{ record.employeeCount }}</span>
            <span class="sub-text"> / {{ record.headcount ?? $t('hr.position.unlimited') }}</span>
          </template>
          <template v-else-if="column.key === 'category'">
            <DictTag type="hr_position_category" :value="record.category" />
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changePositionStatus(record.id, s)"
              :disabled="!userStore.hasPerm('hr:position:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-perm="'hr:position:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <span v-perm="'hr:position:remove'">
                <a-popconfirm :title="$t('hr.position.confirmDelete', { name: record.name })" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('hr.position.add')" :edit-title="$t('hr.position.edit')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.position.code')" name="code"><a-input v-model:value="form.code" :maxlength="32" :placeholder="$t('hr.position.codePh')" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.position.name')" name="name"><a-input v-model:value="form.name" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.position.category')" name="category">
              <a-select v-model:value="form.category" allow-clear :options="toOptions(dict.hr_position_category)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="6">
            <a-form-item :label="$t('hr.position.grade')" name="grade"><a-input v-model:value="form.grade" :maxlength="16" :placeholder="$t('hr.position.gradePh')" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="6">
            <a-form-item :label="$t('common.field.sort')" name="sort"><a-input-number v-model:value="form.sort" :min="0" class="full" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.position.headcount')" name="headcount" :extra="$t('hr.position.headcountExtra')">
              <a-input-number v-model:value="form.headcount" :min="0" class="full" :placeholder="$t('hr.position.unlimited')" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('hr.position.duty')" name="remark"><a-textarea v-model:value="form.remark" :rows="3" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { addPosition, changePositionStatus, deletePosition, pagePosition, updatePosition } from '@/api/hr'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { t } from '@/locales'
import { useAction } from '@/composables/useAction'

const userStore = useUserStore()
const dict = useDict('hr_position_category', 'sys_common_status')
const showSearch = ref(true)

const columns = [
  { title: t('hr.position.position'), key: 'name', width: 200 },
  { title: t('hr.position.category'), key: 'category', width: 110, dict: 'hr_position_category' },
  { title: t('hr.position.grade'), dataIndex: 'grade', width: 100 },
  { title: t('hr.position.activeHeadcount'), key: 'headcount', width: 150, align: 'right' },
  { title: t('hr.position.duty'), dataIndex: 'remark', ellipsis: true },
  { title: t('common.field.sort'), dataIndex: 'sort', width: 70 },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.action.operation'), key: 'action', width: 150, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'hr-position')

const emptyQuery = () => ({ keyword: undefined, category: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pagePosition, query)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(load)

const rules = {
  code: [
    { required: true, message: t('hr.position.codeRequired') },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: t('hr.position.codePattern') }
  ],
  name: [{ required: true, whitespace: true, message: t('hr.position.nameRequired') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, code: '', name: '', category: undefined, grade: '', sort: 0, headcount: null, remark: '' }),
  create: addPosition,
  update: updatePosition,
  toPayload: ({ id, employeeCount, status, ...rest }) => rest,
  onSuccess: load
})
const { formRef, form } = drawer

const remove = useAction(deletePosition, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.over {
  color: var(--status-critical);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
