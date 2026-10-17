<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" @refresh="load">
        <a-button v-perm="'hr:social:edit'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('hr.social.add') }}
        </a-button>
        <a-alert
          type="info"
          show-icon
          :message="$t('hr.social.tip')"
        />
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1700, y: '100%' }"
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
            <a-tag :color="record.status === 1 ? 'green' : 'default'" :bordered="false">{{ record.status === 1 ? $t('common.status.enabled') : $t('common.status.disabled') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'hr:social:edit'" class="table-actions">
              <a-button type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <a-popconfirm :title="$t('hr.social.confirmDelete', { name: record.name })" @confirm="remove(record.id)">
                <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer page :drawer="drawer" :create-title="$t('hr.social.createTitle')" :edit-title="$t('hr.social.editTitle')" width="min(720px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.social.name')" name="name"><a-input v-model:value="form.name" :maxlength="64" :placeholder="$t('hr.social.namePh')" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.social.city')" name="city"><a-input v-model:value="form.city" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.social.baseMin')" name="baseMin">
              <a-input-number v-model:value="form.baseMin" :min="0" :precision="2" :addon-after="$t('hr.social.yuan')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.social.baseMax')" name="baseMax" :extra="$t('hr.social.noLimitExtra')">
              <a-input-number v-model:value="form.baseMax" :min="0" :precision="2" :addon-after="$t('hr.social.yuan')" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-divider orientation="left" plain>{{ $t('hr.social.rates') }}</a-divider>
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
              <a-table-summary-cell :index="0"><b>{{ $t('hr.social.total') }}</b></a-table-summary-cell>
              <a-table-summary-cell :index="1"><b>{{ pct(personalTotal(form)) }}</b></a-table-summary-cell>
              <a-table-summary-cell :index="2"><b>{{ pct(companyTotal(form)) }}</b></a-table-summary-cell>
            </a-table-summary-row>
          </template>
        </a-table>
        <a-divider orientation="left" plain>{{ $t('hr.social.housing') }}</a-divider>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.social.housingMin')" name="housingMin">
              <a-input-number v-model:value="form.housingMin" :min="0" :precision="2" :addon-after="$t('hr.social.yuan')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.social.housingMax')" name="housingMax" :extra="$t('hr.social.housingMaxExtra')">
              <a-input-number v-model:value="form.housingMax" :min="0" :precision="2" :addon-after="$t('hr.social.yuan')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.status')" name="status">
              <a-radio-group v-model:value="form.status">
                <a-radio :value="1">{{ $t('common.status.enabled') }}</a-radio>
                <a-radio :value="0">{{ $t('common.status.disabled') }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark"><a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { formatMoney } from '@/components/scm/scm'
import { addSocialScheme, deleteSocialScheme, listSocialScheme, updateSocialScheme } from '@/api/hr'
import { useFormDrawer } from '@/composables/useFormDrawer'

const { t } = useI18n()

const n = (v) => Number(v) || 0
const pct = (v) => `${+n(v).toFixed(2)}%`
const rangeText = (min, max) => `${formatMoney(n(min))} ~ ${n(max) ? formatMoney(max) : t('hr.social.noLimit')}`
const personalTotal = (r) => n(r.pensionPersonal) + n(r.medicalPersonal) + n(r.unemploymentPersonal)
const companyTotal = (r) => n(r.pensionCompany) + n(r.medicalCompany) + n(r.unemploymentCompany) + n(r.injuryCompany)

const columns = [
  { title: t('hr.social.col.scheme'), key: 'name', width: 180, fixed: 'left' },
  { title: t('hr.social.col.base'), key: 'base', width: 190 },
  { title: t('hr.social.col.pension'), key: 'pension', rate: true, width: 170, align: 'right' },
  { title: t('hr.social.col.medical'), key: 'medical', rate: true, width: 170, align: 'right' },
  { title: t('hr.social.col.unemployment'), key: 'unemployment', rate: true, width: 190, align: 'right' },
  { title: t('hr.social.col.injury'), key: 'injury', width: 140, align: 'right' },
  { title: t('hr.social.col.personalTotal'), key: 'personalTotal', width: 120, align: 'right' },
  { title: t('hr.social.col.companyTotal'), key: 'companyTotal', width: 120, align: 'right' },
  { title: t('hr.social.col.housing'), key: 'housing', width: 190 },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 130, fixed: 'right' }
]
const RATE_ITEMS = [
  { key: 'pension', label: t('hr.social.item.pension'), personal: 'pensionPersonal', company: 'pensionCompany' },
  { key: 'medical', label: t('hr.social.item.medical'), personal: 'medicalPersonal', company: 'medicalCompany' },
  { key: 'unemployment', label: t('hr.social.item.unemployment'), personal: 'unemploymentPersonal', company: 'unemploymentCompany' },
  { key: 'injury', label: t('hr.social.item.injury'), company: 'injuryCompany' }
]
const rateColumns = [
  { title: t('hr.social.col.insurance'), dataIndex: 'label', width: 180 },
  { title: t('hr.social.col.personal'), key: 'personal' },
  { title: t('hr.social.col.company'), key: 'company' }
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
  name: [{ required: true, whitespace: true, message: t('hr.social.rule.name') }]
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
  message.success(t('common.msg.deleteSuccess'))
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
