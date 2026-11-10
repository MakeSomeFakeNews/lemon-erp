<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <span class="sub-text">{{ $t('wms.attribute.tip') }}</span>
        <a-button v-perm="'wms:attribute:edit'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('wms.attribute.createTitle') }}
        </a-button>
      </div>
      <a-table row-key="id" size="middle" :columns="columns" :data-source="list" :loading="loading" :pagination="false" :scroll="{ x: 800, y: '100%' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'values'">
            <a-tag v-for="v in record.values" :key="v.id" :bordered="false" class="value-tag">
              {{ v.value }}<span v-if="v.code" class="sub-text"> {{ v.code }}</span>
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === STATUS_DISABLED ? 'default' : 'green'" :bordered="false">{{ record.status === STATUS_DISABLED ? $t('common.status.disabled') : $t('common.status.enabled') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'edit', perm: 'wms:attribute:edit', onClick: () => drawer.openEdit(record) },
                { type: 'delete', perm: 'wms:attribute:edit', confirm: $t('wms.attribute.confirmDelete', { name: record.name }), onClick: () => remove(record.id) }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('wms.attribute.createTitle')" :edit-title="$t('wms.attribute.editTitle')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('wms.attribute.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="32" :placeholder="$t('wms.attribute.namePh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('common.field.sort')" name="sort">
              <a-input-number v-model:value="form.sort" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('common.field.status')" name="status">
              <a-switch :checked="form.status !== STATUS_DISABLED" :checked-children="$t('common.status.enabled')" :un-checked-children="$t('common.status.disabled')" @change="(v) => (form.status = v ? STATUS_ENABLED : STATUS_DISABLED)" />
            </a-form-item>
          </a-col>
        </a-row>
        <div class="section-title">{{ $t('wms.attribute.values') }}<span class="sub-text">{{ $t('wms.attribute.valuesTip') }}</span></div>
        <div v-for="(v, i) in form.values" :key="v.key" class="value-row">
          <a-input v-model:value="v.value" :maxlength="32" :placeholder="$t('wms.attribute.valuePh')" />
          <a-input v-model:value="v.code" :maxlength="16" :placeholder="$t('wms.attribute.codePh')" class="code" />
          <a-button :disabled="i === 0" @click="move(i, -1)"><ArrowUpOutlined /></a-button>
          <a-tooltip :title="v.used ? $t('wms.attribute.valueUsed') : undefined">
            <a-button danger :disabled="v.used" @click="form.values.splice(i, 1)"><DeleteOutlined /></a-button>
          </a-tooltip>
        </div>
        <a-button type="dashed" block @click="form.values.push({ key: rowKey(), value: '', code: '' })">
          <template #icon><PlusOutlined /></template>{{ $t('wms.attribute.addValue') }}
        </a-button>
        <a-form-item :label="$t('common.field.remark')" name="remark" class="remark">
          <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" />
        </a-form-item>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowUpOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { addAttribute, attributes, deleteAttribute, updateAttribute } from '@/api/wms/material'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { rowKey } from '@/components/wms/wms'
import { useAction } from '@/composables/useAction'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'

const { t } = useI18n()

/** 规格属性：颜色、尺码等及其属性值，多规格物料按属性值组合生成 SKU */
const columns = [
  { title: t('wms.attribute.name'), dataIndex: 'name', width: 140 },
  { title: t('wms.attribute.values'), key: 'values' },
  { title: t('common.field.sort'), dataIndex: 'sort', width: 70 },
  { title: t('common.field.status'), key: 'status', width: 80 },
  actionColumn(2)
]
const list = ref([])
const loading = ref(false)
async function load() {
  loading.value = true
  try {
    list.value = await attributes()
  } finally {
    loading.value = false
  }
}
onMounted(load)

const rules = { name: [{ required: true, whitespace: true, message: t('wms.attribute.inputName') }] }
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', sort: 0, status: STATUS_ENABLED, remark: '', values: [] }),
  fetch: (record) => ({ ...record, values: record.values.map((v) => ({ ...v, key: rowKey(), code: v.code || '' })) }),
  create: addAttribute,
  update: updateAttribute,
  toPayload: (f) => ({
    name: f.name,
    sort: f.sort,
    status: f.status,
    remark: f.remark,
    values: f.values.filter((v) => v.value?.trim()).map((v) => ({ id: v.id, value: v.value.trim(), code: v.code?.trim() || null }))
  }),
  onSuccess: load
})
const { formRef, form } = drawer

function move(i, d) {
  const arr = form.value.values
  ;[arr[i + d], arr[i]] = [arr[i], arr[i + d]]
}

const remove = useAction(deleteAttribute, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.value-tag {
  margin-bottom: 4px;
}
.section-title {
  font-weight: 500;
  margin-bottom: 8px;
}
.value-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.code {
  width: 140px;
  flex: none;
}
.remark {
  margin-top: 16px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
