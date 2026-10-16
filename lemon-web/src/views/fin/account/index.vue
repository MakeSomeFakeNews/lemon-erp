<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <a-space wrap>
          <a-input-search v-model:value="keyword" placeholder="科目编码或名称" allow-clear class="search" @search="load" />
          <a-button @click="toggleExpand">{{ expandedKeys.length ? '全部折叠' : '全部展开' }}</a-button>
        </a-space>
        <a-button v-perm="'fin:account:add'" type="primary" @click="openForm()">
          <template #icon><PlusOutlined /></template>新增一级科目
        </a-button>
      </div>
      <a-table
        v-model:expanded-row-keys="expandedKeys"
        row-key="id"
        size="middle"
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1100, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            {{ record.name }}
            <a-tag v-if="!record.children" :bordered="false" class="leaf-tag">末级</a-tag>
          </template>
          <template v-else-if="column.key === 'category'">
            <DictTag type="fin_account_category" :value="record.category" />
          </template>
          <template v-else-if="column.key === 'direction'">{{ record.direction === 'DEBIT' ? '借' : '贷' }}</template>
          <template v-else-if="column.key === 'auxItems'">
            <a-tag v-for="a in auxLabels(record)" :key="a" :bordered="false" class="aux-tag">{{ a }}</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changeAccountStatus(record.id, s)"
              :disabled="!userStore.hasPerm('fin:account:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-perm="'fin:account:add'" type="link" size="small" @click="openForm(null, record)">新增下级</a-button>
              <a-button v-perm="'fin:account:edit'" type="link" size="small" @click="openForm(record)">编辑</a-button>
              <span v-if="!record.children" v-perm="'fin:account:remove'">
                <a-popconfirm :title="`确定删除科目「${record.code} ${record.name}」？`" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">删除</a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formOpen" :title="form.id ? '编辑科目' : '新增科目'" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item v-if="form.parent" label="上级科目">
          <a-input :value="`${form.parent.code} ${form.parent.name}`" disabled />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="10">
            <a-form-item label="科目编码" name="code" :extra="form.parent ? `上级编码 ${form.parent.code} 后加 2 位` : '一级科目 4 位'">
              <a-input v-model:value="form.code" :maxlength="20" />
            </a-form-item>
          </a-col>
          <a-col :span="14">
            <a-form-item label="科目名称" name="name">
              <a-input v-model:value="form.name" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="类别" name="category">
              <a-select v-model:value="form.category" :options="toOptions(dict.fin_account_category)" :disabled="!!form.parent" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="余额方向" name="direction">
              <a-radio-group v-model:value="form.direction">
                <a-radio value="DEBIT">借</a-radio>
                <a-radio value="CREDIT">贷</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="辅助核算" name="auxItems" extra="可多选。手工录入凭证使用该科目时这些项目必填；科目已使用后也可以修改，只影响之后录入的凭证">
          <a-checkbox-group v-model:value="form.auxItems" :options="AUX_ITEMS" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import { accountTree, addAccount, changeAccountStatus, deleteAccount, updateAccount } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { AUX_ITEMS, auxItemsOf } from '@/components/fin/fin'

const userStore = useUserStore()
const auxLabels = (record) => auxItemsOf(record).map((v) => AUX_ITEMS.find((a) => a.value === v)?.label).filter(Boolean)
const dict = useDict('fin_account_category')

const columns = [
  { title: '科目编码', dataIndex: 'code', width: 180 },
  { title: '科目名称', key: 'name', width: 240 },
  { title: '类别', key: 'category', width: 110 },
  { title: '余额方向', key: 'direction', width: 90 },
  { title: '辅助核算', key: 'auxItems', width: 200 },
  { title: '状态', key: 'status', width: 90 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 210, fixed: 'right' }
]

const keyword = ref('')
const list = ref([])
const loading = ref(false)
const expandedKeys = ref([])

const allParentKeys = (nodes) => nodes.flatMap((n) => (n.children ? [n.id, ...allParentKeys(n.children)] : []))

async function load() {
  loading.value = true
  try {
    list.value = await accountTree({ keyword: keyword.value || undefined })
    expandedKeys.value = allParentKeys(list.value)
  } finally {
    loading.value = false
  }
}
function toggleExpand() {
  expandedKeys.value = expandedKeys.value.length ? [] : allParentKeys(list.value)
}
onMounted(load)

const formOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
  code: [
    { required: true, message: '请填写科目编码' },
    { pattern: /^\d{4,20}$/, message: '科目编码为数字' }
  ],
  name: [{ required: true, whitespace: true, message: '请填写科目名称' }],
  category: [{ required: true, message: '请选择类别' }],
  direction: [{ required: true, message: '请选择余额方向' }]
}

/** record：编辑；parent：新增下级 */
function openForm(record, parent) {
  if (record) {
    form.value = { ...record, parent: undefined, parentId: record.parentId || undefined, auxItems: auxItemsOf(record) }
  } else if (parent) {
    // 下级科目默认沿用上级的辅助核算
    form.value = { code: parent.code, name: '', category: parent.category, direction: parent.direction, remark: '', parent, parentId: parent.id,
      auxItems: auxItemsOf(parent) }
  } else {
    form.value = { code: '', name: '', category: 'ASSET', direction: 'DEBIT', remark: '', auxItems: [] }
  }
  formOpen.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const { id, code, name, parentId, category, direction, remark, auxItems } = form.value
    const data = { code, name, parentId: parentId || null, category, direction, remark, auxItems }
    id ? await updateAccount(id, data) : await addAccount(data)
    message.success('保存成功')
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  await deleteAccount(id)
  message.success('删除成功')
  load()
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.search {
  width: 240px;
}
.aux-tag {
  margin-inline-end: 4px;
}
.leaf-tag {
  margin-left: 6px;
  font-size: 11px;
}
</style>
