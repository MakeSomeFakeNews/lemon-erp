<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('crm.campaign.campaign')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('crm.campaign.namePh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.type')" name="type">
          <a-select v-model:value="query.type" :placeholder="$t('crm.common.all')" allow-clear :options="toOptions(dict.crm_campaign_type)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('crm.common.all')" allow-clear :options="toOptions(dict.crm_campaign_status)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'crm:campaign:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('crm.campaign.add') }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1450, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div>{{ record.name }}</div>
            <div class="sub-text">{{ record.startDate }} ~ {{ record.endDate }}</div>
          </template>
          <template v-else-if="column.key === 'type'"><DictTag type="crm_campaign_type" :value="record.type" /></template>
          <template v-else-if="column.key === 'status'"><DictTag type="crm_campaign_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'cost'">
            <div>¥{{ formatMoney(record.actualCost) }}</div>
            <div class="sub-text">{{ $t('crm.campaign.budgetText', { amount: formatMoney(record.budget) }) }}</div>
          </template>
          <template v-else-if="column.key === 'leads'">
            <div>{{ record.leadCount }}<span v-if="record.targetLeads" class="sub-text">{{ $t('crm.campaign.targetText', { n: record.targetLeads }) }}</span></div>
            <a-progress v-if="record.targetLeads" :percent="Math.min(100, Math.round((record.leadCount * 100) / record.targetLeads))" size="small" :show-info="false" />
          </template>
          <template v-else-if="column.key === 'converted'">
            <div>{{ record.convertedCount }}</div>
            <div class="sub-text">{{ record.leadCount ? percent((record.convertedCount * 100) / record.leadCount) : '-' }}</div>
          </template>
          <template v-else-if="column.key === 'won'">¥{{ formatMoney(record.wonAmount) }}</template>
          <template v-else-if="column.key === 'actual'">
            <div>¥{{ formatMoney(record.orderAmount) }}</div>
            <div class="sub-text">{{ $t('crm.campaign.receivedText', { amount: formatMoney(record.receivedAmount) }) }}</div>
          </template>
          <template v-else-if="column.key === 'roi'">
            <span v-if="Number(record.actualCost) > 0" :class="roi(record) >= 1 ? 'good' : 'bad'">{{ roi(record).toFixed(2) }}</span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <router-link v-if="userStore.hasPerm('crm:lead:list')" :to="{ path: '/crm/lead', query: { campaignId: record.id } }">
                <a-button type="link" size="small">{{ $t('crm.campaign.leads') }}</a-button>
              </router-link>
              <a-button v-perm="'crm:campaign:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <span v-perm="'crm:campaign:remove'">
                <a-popconfirm :title="$t('crm.campaign.confirmDelete', { name: record.name })" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('crm.campaign.add')" :edit-title="$t('crm.campaign.editTitle')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item :label="$t('crm.campaign.name')" name="name"><a-input v-model:value="form.name" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('crm.campaign.type')" name="type">
              <a-select v-model:value="form.type" :options="toOptions(dict.crm_campaign_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.status')" name="status">
              <a-select v-model:value="form.status" :options="toOptions(dict.crm_campaign_status)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.beginDate')" name="startDate">
              <a-date-picker v-model:value="form.startDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.endDate')" name="endDate">
              <a-date-picker v-model:value="form.endDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('crm.campaign.budget')" name="budget">
              <a-input-number v-model:value="form.budget" :min="0" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('crm.campaign.actualCost')" name="actualCost">
              <a-input-number v-model:value="form.actualCost" :min="0" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('crm.campaign.targetLeads')" name="targetLeads">
              <a-input-number v-model:value="form.targetLeads" :min="0" :precision="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('crm.common.f.owner')" name="ownerId">
              <IdentityUserSelect v-model:value="form.ownerId" :label="form.ownerName" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('crm.campaign.description')" name="description">
              <a-textarea v-model:value="form.description" :rows="3" :maxlength="1000" show-count />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import { addCampaign, deleteCampaign, getCampaign, pageCampaign, updateCampaign } from '@/api/crm'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { formatMoney } from '@/components/scm/scm'
import { percent } from '@/components/charts/charts'
import { useAction } from '@/composables/useAction'

import { CampaignStatus } from '@/constants/enums'
const userStore = useUserStore()
const { t } = useI18n()
const dict = useDict('crm_campaign_type', 'crm_campaign_status')

const columns = [
  { title: t('crm.campaign.campaign'), key: 'name', width: 240 },
  { title: t('common.field.type'), key: 'type', width: 110, dict: 'crm_campaign_type' },
  { title: t('common.field.status'), key: 'status', width: 100, dict: 'crm_campaign_status' },
  { title: t('crm.common.f.owner'), dataIndex: 'ownerName', width: 100 },
  { title: t('crm.campaign.actualCost'), key: 'cost', width: 150, align: 'right' },
  { title: t('crm.campaign.col.leads'), key: 'leads', width: 130 },
  { title: t('crm.campaign.col.converted'), key: 'converted', width: 100 },
  { title: t('crm.campaign.col.opps'), dataIndex: 'oppCount', width: 120 },
  { title: t('crm.campaign.col.won'), key: 'won', width: 140, align: 'right' },
  { title: t('crm.campaign.col.actual'), key: 'actual', width: 170, align: 'right' },
  { title: t('crm.campaign.col.roi'), key: 'roi', width: 90, align: 'right' },
  { title: t('common.action.operation'), key: 'action', width: 200, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'crm-campaign')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, type: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageCampaign, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(load)

/** 投入产出比 = 赢单金额 / 实际花费 */
/** ROI = 实际回款 / 活动费用（还没有回款时按订单金额） */
const roi = (r) => (Number(r.receivedAmount) || Number(r.orderAmount) || 0) / Number(r.actualCost)

const rules = {
  name: [{ required: true, whitespace: true, message: t('crm.campaign.nameRequired') }],
  type: [{ required: true, message: t('crm.campaign.typeRequired') }],
  startDate: [{ required: true, message: t('crm.campaign.startRequired') }],
  endDate: [{ required: true, message: t('crm.campaign.endRequired') }]
}
const FIELDS = ['name', 'type', 'status', 'startDate', 'endDate', 'budget', 'actualCost', 'targetLeads', 'ownerId', 'description']
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, ownerName: undefined, ...Object.fromEntries(FIELDS.map((f) => [f, undefined])), status: CampaignStatus.PLANNED }),
  fetch: (record) => getCampaign(record.id),
  create: addCampaign,
  update: updateCampaign,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: load
})
const { formRef, form } = drawer

const remove = useAction(deleteCampaign, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.good {
  color: #52c41a;
  font-weight: 600;
}
.bad {
  color: #f5222d;
  font-weight: 600;
}
</style>
