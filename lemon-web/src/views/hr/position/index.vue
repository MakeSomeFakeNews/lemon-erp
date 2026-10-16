<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="职位" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="编码或名称" allow-clear />
        </a-form-item>
        <a-form-item label="类别" name="category">
          <a-select v-model:value="query.category" placeholder="全部" allow-clear :options="toOptions(dict.hr_position_category)" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'hr:position:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增职位
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
            <span class="sub-text"> / {{ record.headcount ?? '不限' }}</span>
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
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <span v-perm="'hr:position:remove'">
                <a-popconfirm :title="`确定删除职位「${record.name}」？`" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增职位" edit-title="编辑职位">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="职位编码" name="code"><a-input v-model:value="form.code" :maxlength="32" placeholder="如：RD-ENG" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="职位名称" name="name"><a-input v-model:value="form.name" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="类别" name="category">
              <a-select v-model:value="form.category" allow-clear :options="toOptions(dict.hr_position_category)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="6">
            <a-form-item label="职级" name="grade"><a-input v-model:value="form.grade" :maxlength="16" placeholder="如：P5" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="6">
            <a-form-item label="排序" name="sort"><a-input-number v-model:value="form.sort" :min="0" class="full" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="编制人数" name="headcount" extra="为空不限；招聘需求审核时校验">
              <a-input-number v-model:value="form.headcount" :min="0" class="full" placeholder="不限" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="职责说明" name="remark"><a-textarea v-model:value="form.remark" :rows="3" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
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

const userStore = useUserStore()
const dict = useDict('hr_position_category', 'sys_common_status')
const showSearch = ref(true)

const columns = [
  { title: '职位', key: 'name', width: 200 },
  { title: '类别', key: 'category', width: 100, dict: 'hr_position_category' },
  { title: '职级', dataIndex: 'grade', width: 80 },
  { title: '在职 / 编制', key: 'headcount', width: 110, align: 'right' },
  { title: '职责说明', dataIndex: 'remark', ellipsis: true },
  { title: '排序', dataIndex: 'sort', width: 70 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
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
    { required: true, message: '请输入职位编码' },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: '只能包含字母、数字、下划线和短横线' }
  ],
  name: [{ required: true, whitespace: true, message: '请输入职位名称' }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, code: '', name: '', category: undefined, grade: '', sort: 0, headcount: null, remark: '' }),
  create: addPosition,
  update: updatePosition,
  toPayload: ({ id, employeeCount, status, ...rest }) => rest,
  onSuccess: load
})
const { formRef, form } = drawer

async function remove(id) {
  await deletePosition(id)
  message.success('删除成功')
  load()
}
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
