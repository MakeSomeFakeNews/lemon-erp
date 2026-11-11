<template>
  <div>
    <div class="filters">
      <a-radio-group v-model:value="direction" button-style="solid">
        <a-radio-button value="">{{ $t('fin.io.allTypes') }}</a-radio-button>
        <a-radio-button :value="FundDirection.IN">{{ $t('fin.io.typeIn') }}</a-radio-button>
        <a-radio-button :value="FundDirection.OUT">{{ $t('fin.io.typeOut') }}</a-radio-button>
      </a-radio-group>
      <span class="sub-text">{{ $t('fin.io.categoryTip') }}</span>
      <a-button v-perm="'fin:io:category'" type="primary" class="add" @click="drawer.openCreate({ direction: direction || FundDirection.OUT })">
        <template #icon><PlusOutlined /></template>{{ $t('fin.io.addCategory') }}
      </a-button>
    </div>
    <a-table row-key="id" size="small" :columns="columns" :data-source="shown" :loading="loading" :pagination="false" :scroll="{ x: 760 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          {{ record.name }}
          <a-tooltip v-if="record.code" :title="$t('fin.io.builtinHint')"><a-tag :bordered="false" class="builtin">{{ $t('fin.io.builtin') }}</a-tag></a-tooltip>
        </template>
        <template v-else-if="column.key === 'direction'"><DictTag type="fin_fund_direction" :value="record.direction" /></template>
        <template v-else-if="column.key === 'account'">
          <span v-if="record.accountCode">{{ record.accountCode }} {{ record.accountName }}</span>
          <span v-else class="warn">{{ $t('fin.io.noAccount') }}</span>
        </template>
        <template v-else-if="column.key === 'status'"><DictTag type="sys_common_status" :value="record.status" /></template>
        <template v-else-if="column.key === 'action'">
          <TableActions
            :actions="[
              { type: 'edit', perm: 'fin:io:category', onClick: () => drawer.openEdit(record) },
              { type: 'delete', show: !record.used && !record.code, perm: 'fin:io:category', confirm: $t('fin.io.confirmDeleteCategory', { name: record.name }), onClick: () => remove(record.id) }
            ]"
          />
        </template>
      </template>
    </a-table>

    <FormDrawer :drawer="drawer" :create-title="$t('fin.io.addCategory')" :edit-title="$t('fin.io.editCategory')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('fin.io.type')" name="direction" :extra="form.code ? $t('fin.io.builtinHint') : form.used ? $t('fin.io.directionLocked') : ''">
          <a-radio-group v-model:value="form.direction" :disabled="form.used || !!form.code">
            <a-radio :value="FundDirection.IN">{{ $t('fin.io.typeIn') }}</a-radio>
            <a-radio :value="FundDirection.OUT">{{ $t('fin.io.typeOut') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('fin.io.categoryName')" name="name">
          <a-input v-model:value="form.name" :maxlength="64" />
        </a-form-item>
        <a-form-item :label="$t('fin.io.categoryAccount')" name="accountId" :extra="form.direction === FundDirection.IN ? $t('fin.io.accountExtraIn') : $t('fin.io.accountExtraOut')">
          <AccountSelect v-model:value="form.accountId" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('common.field.sort')" name="sort">
              <a-input-number v-model:value="form.sort" :min="0" :precision="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.status')" name="status">
              <a-radio-group v-model:value="form.status" :options="toOptions(dict.sys_common_status, true)" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('common.field.remark')" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import { addIoCategory, deleteIoCategory, listIoCategory, updateIoCategory } from '@/api/fin'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { STATUS_ENABLED } from '@/utils/constants'
import { FundDirection } from '@/constants/enums'

/** 收支类别：每个租户一份，一级，标明收 / 支并关联凭证科目；已使用的不能删除、不能改方向，可停用 */
const { t } = useI18n()
const dict = useDict('fin_fund_direction', 'sys_common_status')
const direction = ref('')
const columns = [
  { title: t('fin.io.categoryName'), key: 'name', width: 180 },
  { title: t('fin.io.type'), key: 'direction', width: 80 },
  { title: t('fin.io.categoryAccount'), key: 'account', width: 220 },
  { title: t('common.field.sort'), dataIndex: 'sort', width: 70, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 80 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  actionColumn(2, [t('common.action.edit')], { fixed: undefined })
]
const list = ref([])
const loading = ref(false)
const shown = computed(() => (direction.value ? list.value.filter((c) => c.direction === direction.value) : list.value))
async function load() {
  loading.value = true
  try {
    list.value = await listIoCategory()
  } finally {
    loading.value = false
  }
}
onMounted(load)

const rules = {
  name: [{ required: true, message: t('fin.io.categoryNameRequired') }],
  accountId: [{ required: true, message: t('fin.io.categoryAccountRequired') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', direction: FundDirection.OUT, accountId: undefined, sort: 0, status: STATUS_ENABLED, remark: '', used: false, code: undefined }),
  create: addIoCategory,
  update: updateIoCategory,
  toPayload: (f) => ({ name: f.name, direction: f.direction, accountId: f.accountId, sort: f.sort ?? 0, status: f.status, remark: f.remark || null }),
  onSuccess: load
})
const { formRef, form } = drawer
const remove = useAction(deleteIoCategory, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.builtin {
  margin-left: 4px;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 12px;
}
.add {
  margin-left: auto;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.warn {
  color: var(--status-critical, #cf1322);
}
.full {
  width: 100%;
}
</style>
