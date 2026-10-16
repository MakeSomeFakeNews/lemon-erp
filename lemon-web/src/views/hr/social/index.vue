<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" @refresh="load">
        <a-button v-perm="'hr:social:edit'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增方案
        </a-button>
        <a-alert
          type="info"
          show-icon
          message="在「薪资档案」中为员工选择社保方案；未选择方案的员工按默认比例计算（个人 10.5%、单位 26%）。缴费基数按方案的上下限截取，上限为 0 表示不限。"
        />
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1500, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div>{{ record.name }}</div>
            <div class="sub-text">{{ record.city || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'base'">{{ rangeText(record.baseMin, record.baseMax) }}</template>
          <template v-else-if="column.key === 'housing'">{{ rangeText(record.housingMin, record.housingMax) }}</template>
          <template v-else-if="column.rate">
            <span>{{ pct(record[`${column.key}Personal`]) }}</span>
            <span class="sub-text"> / </span>
            <span>{{ pct(record[`${column.key}Company`]) }}</span>
          </template>
          <template v-else-if="column.key === 'injury'">{{ pct(record.injuryCompany) }}</template>
          <template v-else-if="column.key === 'personalTotal'"><b>{{ pct(personalTotal(record)) }}</b></template>
          <template v-else-if="column.key === 'companyTotal'"><b>{{ pct(companyTotal(record)) }}</b></template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'" :bordered="false">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'hr:social:edit'" class="table-actions">
              <a-button type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <a-popconfirm :title="`确定删除社保方案「${record.name}」？`" @confirm="remove(record.id)">
                <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer page :drawer="drawer" create-title="新增社保方案" edit-title="编辑社保方案" width="min(720px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="方案名称" name="name"><a-input v-model:value="form.name" :maxlength="64" placeholder="如：上海社保 2026" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="参保城市" name="city"><a-input v-model:value="form.city" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="社保基数下限" name="baseMin">
              <a-input-number v-model:value="form.baseMin" :min="0" :precision="2" addon-after="元" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="社保基数上限" name="baseMax" extra="0 表示不限">
              <a-input-number v-model:value="form.baseMax" :min="0" :precision="2" addon-after="元" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-divider orientation="left" plain>缴费比例（%）</a-divider>
        <a-table row-key="key" size="small" bordered :columns="rateColumns" :data-source="RATE_ITEMS" :pagination="false" class="mb">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'personal' || column.key === 'company'">
              <a-input-number
                v-if="record[column.key]"
                v-model:value="form[record[column.key]]"
                :min="0"
                :max="100"
                :precision="2"
                addon-after="%"
                class="full"
              />
              <span v-else class="sub-text">-</span>
            </template>
          </template>
          <template #summary>
            <a-table-summary-row>
              <a-table-summary-cell :index="0"><b>合计</b></a-table-summary-cell>
              <a-table-summary-cell :index="1"><b>{{ pct(personalTotal(form)) }}</b></a-table-summary-cell>
              <a-table-summary-cell :index="2"><b>{{ pct(companyTotal(form)) }}</b></a-table-summary-cell>
            </a-table-summary-row>
          </template>
        </a-table>
        <a-divider orientation="left" plain>公积金</a-divider>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="公积金基数下限" name="housingMin">
              <a-input-number v-model:value="form.housingMin" :min="0" :precision="2" addon-after="元" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="公积金基数上限" name="housingMax" extra="0 表示不限；公积金比例在薪资档案中设置">
              <a-input-number v-model:value="form.housingMax" :min="0" :precision="2" addon-after="元" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="状态" name="status">
              <a-radio-group v-model:value="form.status">
                <a-radio :value="1">启用</a-radio>
                <a-radio :value="0">停用</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark"><a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { formatMoney } from '@/components/scm/scm'
import { addSocialScheme, deleteSocialScheme, listSocialScheme, updateSocialScheme } from '@/api/hr'
import { useFormDrawer } from '@/composables/useFormDrawer'

const n = (v) => Number(v) || 0
const pct = (v) => `${+n(v).toFixed(2)}%`
const rangeText = (min, max) => `${formatMoney(n(min))} ~ ${n(max) ? formatMoney(max) : '不限'}`
const personalTotal = (r) => n(r.pensionPersonal) + n(r.medicalPersonal) + n(r.unemploymentPersonal)
const companyTotal = (r) => n(r.pensionCompany) + n(r.medicalCompany) + n(r.unemploymentCompany) + n(r.injuryCompany)

const columns = [
  { title: '方案', key: 'name', width: 180, fixed: 'left' },
  { title: '社保基数', key: 'base', width: 190 },
  { title: '养老（个人 / 单位）', key: 'pension', rate: true, width: 150, align: 'right' },
  { title: '医疗（个人 / 单位）', key: 'medical', rate: true, width: 150, align: 'right' },
  { title: '失业（个人 / 单位）', key: 'unemployment', rate: true, width: 150, align: 'right' },
  { title: '工伤（单位）', key: 'injury', width: 100, align: 'right' },
  { title: '个人合计', key: 'personalTotal', width: 100, align: 'right' },
  { title: '单位合计', key: 'companyTotal', width: 100, align: 'right' },
  { title: '公积金基数', key: 'housing', width: 190 },
  { title: '状态', key: 'status', width: 80 },
  { title: '备注', dataIndex: 'remark', width: 160, ellipsis: true },
  { title: '操作', key: 'action', width: 130, fixed: 'right' }
]
const RATE_ITEMS = [
  { key: 'pension', label: '养老保险', personal: 'pensionPersonal', company: 'pensionCompany' },
  { key: 'medical', label: '医疗保险', personal: 'medicalPersonal', company: 'medicalCompany' },
  { key: 'unemployment', label: '失业保险', personal: 'unemploymentPersonal', company: 'unemploymentCompany' },
  { key: 'injury', label: '工伤保险', company: 'injuryCompany' }
]
const rateColumns = [
  { title: '险种', dataIndex: 'label', width: 120 },
  { title: '个人', key: 'personal' },
  { title: '单位', key: 'company' }
]

const list = ref([])
const loading = ref(false)
const size = ref('middle')
async function load() {
  loading.value = true
  try {
    list.value = await listSocialScheme()
  } finally {
    loading.value = false
  }
}
onMounted(load)

const FIELDS = ['name', 'city', 'baseMin', 'baseMax', 'pensionPersonal', 'pensionCompany', 'medicalPersonal', 'medicalCompany', 'unemploymentPersonal',
  'unemploymentCompany', 'injuryCompany', 'housingMin', 'housingMax', 'status', 'remark']
const rules = {
  name: [{ required: true, whitespace: true, message: '请输入方案名称' }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    name: undefined,
    city: undefined,
    baseMin: 0,
    baseMax: 0,
    pensionPersonal: 8,
    pensionCompany: 16,
    medicalPersonal: 2,
    medicalCompany: 8,
    unemploymentPersonal: 0.5,
    unemploymentCompany: 0.5,
    injuryCompany: 0.2,
    housingMin: 0,
    housingMax: 0,
    status: 1,
    remark: undefined
  }),
  create: addSocialScheme,
  update: updateSocialScheme,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: load
})
const { formRef, form } = drawer

async function remove(id) {
  await deleteSocialScheme(id)
  message.success('删除成功')
  load()
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.mb {
  margin-bottom: 16px;
}
</style>
