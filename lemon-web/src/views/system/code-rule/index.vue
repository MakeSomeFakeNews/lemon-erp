<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-segmented v-model:value="module" :options="moduleOptions" />
        <template #extra>
          <span class="hint">修改后只影响之后新建的单据，已有单号不变</span>
        </template>
      </TableToolbar>

      <a-table
        row-key="key"
        :size="size"
        :columns="visibleColumns"
        :data-source="filtered"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 900, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <span>{{ record.name }}</span>
            <a-tag v-if="record.customized" color="blue" :bordered="false" class="custom-tag">已修改</a-tag>
          </template>
          <template v-else-if="column.key === 'example'">
            <span class="example">{{ record.example }}</span>
          </template>
          <template v-else-if="column.key === 'dateFormat'">{{ dateFormatLabel(record.dateFormat) }}</template>
          <template v-else-if="column.key === 'resetCycle'">{{ resetCycleLabel(record.resetCycle) }}</template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-perm="'sys:codeRule:edit'" type="link" size="small" @click="drawer.openEdit({ ...record, id: record.key })">
                <template #icon><EditOutlined /></template>修改
              </a-button>
              <span v-if="record.customized" v-perm="'sys:codeRule:edit'">
                <a-popconfirm :title="`确定恢复「${record.name}」的默认规则？`" @confirm="reset(record)">
                  <a-button type="link" size="small"><template #icon><UndoOutlined /></template>恢复默认</a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :edit-title="`修改编码规则 - ${form.name || ''}`">
      <a-alert
        type="info"
        show-icon
        class="form-tip"
        message="编号 = 前缀 + 日期 + 流水号。流水号按重置周期从 1 开始计数，日期部分至少要精确到重置周期，否则编号会重复。"
      />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="前缀" name="prefix" extra="大写字母、数字和短横线，最多 8 个字符；不能与其他单据重复">
          <a-input v-model:value="form.prefix" :maxlength="8" @update:value="(v) => (form.prefix = (v || '').toUpperCase())" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="日期格式" name="dateFormat">
              <a-select v-model:value="form.dateFormat" :options="DATE_FORMATS" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="流水重置周期" name="resetCycle">
              <a-select v-model:value="form.resetCycle" :options="RESET_CYCLES" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="流水位数" name="seqLength" extra="超出位数时自动加长，不会重复">
              <a-input-number v-model:value="form.seqLength" :min="3" :max="8" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <div class="preview">
          <div class="preview-label">编号预览</div>
          <div class="preview-value">{{ preview }}</div>
          <div v-if="!dateSupportsCycle" class="preview-warn">
            日期格式不够精确：流水按「{{ resetCycleLabel(form.resetCycle) }}」重置时会产生重复编号
          </div>
        </div>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { EditOutlined, UndoOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { listCodeRules, resetCodeRule, updateCodeRule } from '@/api/system/codeRule'
import { useColumns } from '@/composables/useColumns'
import { useFormDrawer } from '@/composables/useFormDrawer'

/** 与后端 DocNoCatalog.DateFormat 一致；precision 为日期精确到的周期（越小越精确） */
const DATE_FORMATS = [
  { value: 'YYYYMMDD', label: '年月日（20261015）', pattern: 'YYYYMMDD', precision: 0 },
  { value: 'YYMMDD', label: '短年月日（261015）', pattern: 'YYMMDD', precision: 0 },
  { value: 'YYYYMM', label: '年月（202610）', pattern: 'YYYYMM', precision: 1 },
  { value: 'YYMM', label: '短年月（2610）', pattern: 'YYMM', precision: 1 },
  { value: 'YYYY', label: '年（2026）', pattern: 'YYYY', precision: 2 },
  { value: 'NONE', label: '不含日期', pattern: '', precision: 3 }
]
const RESET_CYCLES = [
  { value: 'DAY', label: '每天', order: 0 },
  { value: 'MONTH', label: '每月', order: 1 },
  { value: 'YEAR', label: '每年', order: 2 },
  { value: 'NEVER', label: '不重置', order: 3 }
]
const dateFormatLabel = (v) => DATE_FORMATS.find((d) => d.value === v)?.label.replace(/（.*）/, '') ?? v
const resetCycleLabel = (v) => RESET_CYCLES.find((d) => d.value === v)?.label ?? v

const columns = [
  { title: '单据', key: 'name', width: 200 },
  { title: '模块', dataIndex: 'module', width: 110 },
  { title: '编号示例', key: 'example', width: 200 },
  { title: '前缀', dataIndex: 'prefix', width: 90 },
  { title: '日期格式', key: 'dateFormat', width: 110 },
  { title: '流水位数', dataIndex: 'seqLength', width: 90, align: 'right' },
  { title: '重置周期', key: 'resetCycle', width: 100 },
  { title: '操作', key: 'action', width: 190, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'system-code-rule')
const size = ref('middle')
const list = ref([])
const loading = ref(false)
const module = ref('全部')
const moduleOptions = computed(() => ['全部', ...new Set(list.value.map((r) => r.module))])
const filtered = computed(() => (module.value === '全部' ? list.value : list.value.filter((r) => r.module === module.value)))

async function load() {
  loading.value = true
  try {
    list.value = await listCodeRules()
  } finally {
    loading.value = false
  }
}
onMounted(load)

const rules = {
  prefix: [
    { required: true, message: '请填写前缀' },
    { pattern: /^[A-Z0-9-]{1,8}$/, message: '只能包含大写字母、数字和短横线' }
  ],
  dateFormat: [{ required: true, message: '请选择日期格式' }],
  resetCycle: [{ required: true, message: '请选择重置周期' }],
  seqLength: [{ required: true, message: '请填写流水位数' }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ key: undefined, name: '', prefix: '', dateFormat: 'YYYYMMDD', seqLength: 4, resetCycle: 'DAY' }),
  create: () => Promise.resolve(),
  update: (key, form) => updateCodeRule(key, form),
  toPayload: ({ prefix, dateFormat, seqLength, resetCycle }) => ({ prefix, dateFormat, seqLength, resetCycle }),
  onSuccess: load
})
// useFormDrawer 以 id 区分新增和修改，编码规则以 key 作为 id 打开
const { formRef, form } = drawer

const dateSupportsCycle = computed(() => {
  const d = DATE_FORMATS.find((x) => x.value === form.value.dateFormat)
  const c = RESET_CYCLES.find((x) => x.value === form.value.resetCycle)
  return !d || !c || d.precision <= c.order
})
const preview = computed(() => {
  const d = DATE_FORMATS.find((x) => x.value === form.value.dateFormat)
  const date = d?.pattern ? dayjs().format(d.pattern) : ''
  return `${form.value.prefix || ''}${date}${'1'.padStart(form.value.seqLength || 4, '0')}`
})

async function reset(record) {
  await resetCodeRule(record.key)
  message.success('已恢复默认规则')
  load()
}
</script>

<style scoped>
.hint {
  color: var(--app-text-3);
  font-size: 12px;
}
.custom-tag {
  margin-left: 6px;
}
.example {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}
.form-tip {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.preview {
  padding: 12px 16px;
  border-radius: 8px;
  background: var(--app-hover);
}
.preview-label {
  font-size: 12px;
  color: var(--app-text-3);
}
.preview-value {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 1px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  color: var(--app-text);
}
.preview-warn {
  margin-top: 6px;
  font-size: 12px;
  color: var(--status-critical);
}
</style>
