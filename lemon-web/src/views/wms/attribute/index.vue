<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <span class="sub-text">规格属性用于多规格物料（款式）：在「物料管理」中选择属性值组合，批量生成 SKU</span>
        <a-button v-perm="'wms:attribute:edit'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增规格属性
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
            <a-tag :color="record.status === 0 ? 'default' : 'green'" :bordered="false">{{ record.status === 0 ? '停用' : '启用' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'wms:attribute:edit'" class="table-actions">
              <a-button type="link" size="small" @click="drawer.openEdit(record)"><template #icon><EditOutlined /></template>编辑</a-button>
              <a-popconfirm :title="`删除规格属性「${record.name}」？`" @confirm="remove(record.id)">
                <a-button type="link" danger size="small"><template #icon><DeleteOutlined /></template>删除</a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增规格属性" edit-title="编辑规格属性">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="属性名称" name="name">
              <a-input v-model:value="form.name" :maxlength="32" placeholder="如：颜色、尺码" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="排序" name="sort">
              <a-input-number v-model:value="form.sort" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="状态" name="status">
              <a-switch :checked="form.status !== 0" checked-children="启用" un-checked-children="停用" @change="(v) => (form.status = v ? 1 : 0)" />
            </a-form-item>
          </a-col>
        </a-row>
        <div class="section-title">属性值<span class="sub-text">（编码用于生成 SKU 编码，为空用属性值；已被 SKU 使用的不能删除）</span></div>
        <div v-for="(v, i) in form.values" :key="v.key" class="value-row">
          <a-input v-model:value="v.value" :maxlength="32" placeholder="属性值，如 红色" />
          <a-input v-model:value="v.code" :maxlength="16" placeholder="编码，如 RD" class="code" />
          <a-button :disabled="i === 0" @click="move(i, -1)"><ArrowUpOutlined /></a-button>
          <a-button danger @click="form.values.splice(i, 1)"><DeleteOutlined /></a-button>
        </div>
        <a-button type="dashed" block @click="form.values.push({ key: rowKey(), value: '', code: '' })">
          <template #icon><PlusOutlined /></template>添加属性值
        </a-button>
        <a-form-item label="备注" name="remark" class="remark">
          <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" />
        </a-form-item>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ArrowUpOutlined, DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { addAttribute, attributes, deleteAttribute, updateAttribute } from '@/api/wms/material'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { rowKey } from '@/components/wms/wms'

/** 规格属性：颜色、尺码等及其属性值，多规格物料按属性值组合生成 SKU */
const columns = [
  { title: '属性名称', dataIndex: 'name', width: 140 },
  { title: '属性值', key: 'values' },
  { title: '排序', dataIndex: 'sort', width: 70 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' }
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

const rules = { name: [{ required: true, whitespace: true, message: '请输入属性名称' }] }
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', sort: 0, status: 1, remark: '', values: [] }),
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

async function remove(id) {
  await deleteAttribute(id)
  message.success('删除成功')
  load()
}
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
