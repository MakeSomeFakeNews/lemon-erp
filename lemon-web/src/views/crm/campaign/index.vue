<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="活动" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="活动名称" allow-clear />
        </a-form-item>
        <a-form-item label="类型" name="type">
          <a-select v-model:value="query.type" placeholder="全部" allow-clear :options="toOptions(dict.crm_campaign_type)" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.crm_campaign_status)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'crm:campaign:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增活动
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
            <div class="sub-text">预算 ¥{{ formatMoney(record.budget) }}</div>
          </template>
          <template v-else-if="column.key === 'leads'">
            <div>{{ record.leadCount }}<span v-if="record.targetLeads" class="sub-text"> / 目标 {{ record.targetLeads }}</span></div>
            <a-progress v-if="record.targetLeads" :percent="Math.min(100, Math.round((record.leadCount * 100) / record.targetLeads))" size="small" :show-info="false" />
          </template>
          <template v-else-if="column.key === 'converted'">
            <div>{{ record.convertedCount }}</div>
            <div class="sub-text">{{ record.leadCount ? percent((record.convertedCount * 100) / record.leadCount) : '-' }}</div>
          </template>
          <template v-else-if="column.key === 'won'">¥{{ formatMoney(record.wonAmount) }}</template>
          <template v-else-if="column.key === 'actual'">
            <div>¥{{ formatMoney(record.orderAmount) }}</div>
            <div class="sub-text">回款 ¥{{ formatMoney(record.receivedAmount) }}</div>
          </template>
          <template v-else-if="column.key === 'roi'">
            <span v-if="Number(record.actualCost) > 0" :class="roi(record) >= 1 ? 'good' : 'bad'">{{ roi(record).toFixed(2) }}</span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <router-link v-if="userStore.hasPerm('crm:lead:list')" :to="{ path: '/crm/lead', query: { campaignId: record.id } }">
                <a-button type="link" size="small">线索</a-button>
              </router-link>
              <a-button v-perm="'crm:campaign:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <span v-perm="'crm:campaign:remove'">
                <a-popconfirm :title="`确定删除活动「${record.name}」？`" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增活动" edit-title="编辑活动">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="活动名称" name="name"><a-input v-model:value="form.name" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="活动类型" name="type">
              <a-select v-model:value="form.type" :options="toOptions(dict.crm_campaign_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="状态" name="status">
              <a-select v-model:value="form.status" :options="toOptions(dict.crm_campaign_status)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="开始日期" name="startDate">
              <a-date-picker v-model:value="form.startDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="结束日期" name="endDate">
              <a-date-picker v-model:value="form.endDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="预算" name="budget">
              <a-input-number v-model:value="form.budget" :min="0" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="实际花费" name="actualCost">
              <a-input-number v-model:value="form.actualCost" :min="0" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="目标线索数" name="targetLeads">
              <a-input-number v-model:value="form.targetLeads" :min="0" :precision="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="负责人" name="ownerId">
              <IdentityUserSelect v-model:value="form.ownerId" :label="form.ownerName" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="活动说明" name="description">
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
import { message } from 'ant-design-vue'
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

const userStore = useUserStore()
const dict = useDict('crm_campaign_type', 'crm_campaign_status')

const columns = [
  { title: '活动', key: 'name', width: 240 },
  { title: '类型', key: 'type', width: 100, dict: 'crm_campaign_type' },
  { title: '状态', key: 'status', width: 90, dict: 'crm_campaign_status' },
  { title: '负责人', dataIndex: 'ownerName', width: 100 },
  { title: '实际花费', key: 'cost', width: 150, align: 'right' },
  { title: '线索数', key: 'leads', width: 130 },
  { title: '转化数', key: 'converted', width: 90 },
  { title: '商机数', dataIndex: 'oppCount', width: 80 },
  { title: '赢单金额', key: 'won', width: 140, align: 'right' },
  { title: '实际订单 / 回款', key: 'actual', width: 160, align: 'right' },
  { title: '回款 ROI', key: 'roi', width: 90, align: 'right' },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
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
  name: [{ required: true, whitespace: true, message: '请输入活动名称' }],
  type: [{ required: true, message: '请选择活动类型' }],
  startDate: [{ required: true, message: '请选择开始日期' }],
  endDate: [{ required: true, message: '请选择结束日期' }]
}
const FIELDS = ['name', 'type', 'status', 'startDate', 'endDate', 'budget', 'actualCost', 'targetLeads', 'ownerId', 'description']
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, ownerName: undefined, ...Object.fromEntries(FIELDS.map((f) => [f, undefined])), status: 'PLANNED' }),
  fetch: (record) => getCampaign(record.id),
  create: addCampaign,
  update: updateCampaign,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: load
})
const { formRef, form } = drawer

async function remove(id) {
  await deleteCampaign(id)
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
.good {
  color: #52c41a;
  font-weight: 600;
}
.bad {
  color: #f5222d;
  font-weight: 600;
}
</style>
