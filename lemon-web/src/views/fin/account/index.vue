<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <a-space wrap>
          <a-input-search v-model:value="keyword" :placeholder="$t('fin.account.keywordPh')" allow-clear class="search" @search="load" />
          <a-button @click="toggleExpand">{{ expandedKeys.length ? $t('fin.account.collapseAll') : $t('fin.account.expandAll') }}</a-button>
        </a-space>
        <a-button v-perm="'fin:account:add'" type="primary" @click="openForm()">
          <template #icon><PlusOutlined /></template>{{ $t('fin.account.addTop') }}
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
        :scroll="{ x: scrollX, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            {{ record.name }}
            <a-tag v-if="!record.children" :bordered="false" class="leaf-tag">{{ $t('fin.account.leaf') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'category'">
            <DictTag type="fin_account_category" :value="record.category" />
          </template>
          <template v-else-if="column.key === 'direction'">{{ record.direction === AccountDirection.DEBIT ? $t('fin.account.dr') : $t('fin.account.cr') }}</template>
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
              <a-button v-perm="'fin:account:add'" type="link" size="small" @click="openForm(null, record)">{{ $t('fin.account.addChild') }}</a-button>
              <a-button v-perm="'fin:account:edit'" type="link" size="small" @click="openForm(record)">{{ $t('common.action.edit') }}</a-button>
              <span v-if="!record.children" v-perm="'fin:account:remove'">
                <a-popconfirm :title="$t('fin.account.confirmDelete', { name: `${record.code} ${record.name}` })" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formOpen" :title="form.id ? $t('fin.account.edit') : $t('fin.account.add')" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item v-if="form.parent" :label="$t('fin.account.parent')">
          <a-input :value="`${form.parent.code} ${form.parent.name}`" disabled />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="10">
            <a-form-item :label="$t('fin.account.code')" name="code" :extra="form.parent ? $t('fin.account.codeExtraChild', { code: form.parent.code }) : $t('fin.account.codeExtraTop')">
              <a-input v-model:value="form.code" :maxlength="20" />
            </a-form-item>
          </a-col>
          <a-col :span="14">
            <a-form-item :label="$t('fin.account.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('fin.account.category')" name="category">
              <a-select v-model:value="form.category" :options="toOptions(dict.fin_account_category)" :disabled="!!form.parent" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('fin.account.direction')" name="direction">
              <a-radio-group v-model:value="form.direction">
                <a-radio value="DEBIT">{{ $t('fin.account.dr') }}</a-radio>
                <a-radio value="CREDIT">{{ $t('fin.account.cr') }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('fin.account.aux')" name="auxItems" :extra="$t('fin.account.auxExtra')">
          <a-checkbox-group v-model:value="form.auxItems" :options="AUX_ITEMS" />
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import { accountTree, addAccount, changeAccountStatus, deleteAccount, updateAccount } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { AUX_ITEMS, auxItemsOf } from '@/components/fin/fin'
import { useAction } from '@/composables/useAction'

import { AccountDirection } from '@/constants/enums'
const { t } = useI18n()
const userStore = useUserStore()
const auxLabels = (record) => auxItemsOf(record).map((v) => AUX_ITEMS.find((a) => a.value === v)?.label).filter(Boolean)
const dict = useDict('fin_account_category')

const columns = [
  { title: t('fin.account.code'), dataIndex: 'code', width: 180 },
  { title: t('fin.account.name'), key: 'name', width: 240 },
  { title: t('fin.account.category'), key: 'category', width: 120 },
  { title: t('fin.account.direction'), key: 'direction', width: 130 },
  { title: t('fin.account.aux'), key: 'auxItems', width: 200 },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 200, ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 240, fixed: 'right' }
]
const scrollX = columns.reduce((sum, c) => sum + c.width, 0)

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
    { required: true, message: t('fin.account.codeRequired') },
    { pattern: /^\d{4,20}$/, message: t('fin.account.codeDigits') }
  ],
  name: [{ required: true, whitespace: true, message: t('fin.account.nameRequired') }],
  category: [{ required: true, message: t('fin.account.categoryRequired') }],
  direction: [{ required: true, message: t('fin.account.directionRequired') }]
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
    message.success(t('common.msg.saveSuccess'))
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

const remove = useAction(deleteAccount, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
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
