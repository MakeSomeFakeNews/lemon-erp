<template>
  <div class="jxc-assets">
    <div class="page-card header">
      <div>
        <div class="sub-text">{{ data.asOf ? $t('jxc.assets.asOf', { date: data.asOf }) : '' }}</div>
        <div class="sub-text">{{ $t('jxc.assets.tip') }}</div>
      </div>
      <a-button :loading="loading" @click="load">
        <template #icon><ReloadOutlined /></template>{{ $t('common.action.refresh') }}
      </a-button>
    </div>

    <a-spin :spinning="loading">
      <a-row :gutter="[16, 16]">
        <a-col v-for="k in kpis" :key="k.key" :xs="12" :md="8" :xl="6" :xxl="3">
          <div class="page-card kpi" :class="{ main: k.main }">
            <div class="sub-text">{{ k.label }}</div>
            <div class="kpi-value" :class="{ neg: k.neg }">¥{{ money(k.value) }}</div>
            <div v-if="k.extra" class="sub-text">{{ k.extra }}</div>
          </div>
        </a-col>
      </a-row>
      <div class="sub-text formula">{{ $t('jxc.assets.netTip') }}</div>

      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :xl="8">
          <a-card :bordered="false" class="page-card" :title="$t('jxc.assets.funds')">
            <a-table row-key="accountId" size="small" :columns="fundColumns" :data-source="data.funds" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'account'">{{ record.code }} {{ record.name }}</template>
                <template v-else-if="column.key === 'balance'">
                  <span :class="{ neg: Number(record.balance) < 0 }">{{ money(record.balance) }}</span>
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12" :xl="8">
          <a-card :bordered="false" class="page-card" :title="$t('jxc.assets.byWarehouse')">
            <EChart :option="pie(data.inventoryByWarehouse, (r) => r.name)" height="260px" />
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12" :xl="8">
          <a-card :bordered="false" class="page-card" :title="$t('jxc.assets.byCategory')">
            <EChart :option="pie(data.inventoryByCategory, (r) => dictLabel(dict.wms_material_category, r.key) || $t('jxc.common.unset'))" height="260px" />
          </a-card>
        </a-col>
        <a-col :xs="24" :xl="12">
          <a-card :bordered="false" class="page-card" :title="$t('jxc.assets.topAr')">
            <a-table row-key="partnerId" size="small" :columns="partnerColumns" :data-source="data.topReceivables" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'amount'">{{ money(record.amount) }}</template>
              </template>
            </a-table>
          </a-card>
        </a-col>
        <a-col :xs="24" :xl="12">
          <a-card :bordered="false" class="page-card" :title="$t('jxc.assets.topAp')">
            <a-table row-key="partnerId" size="small" :columns="partnerColumns" :data-source="data.topPayables" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'amount'">{{ money(record.amount) }}</template>
              </template>
            </a-table>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import { jxcAssets } from '@/api/bi/jxc'
import { shortMoney } from '@/components/charts/charts'
import { dictLabel } from '@/components/bi/jxc/jxc'
import { useDict } from '@/composables/useDict'
import { money } from '@/utils/number'
import { t } from '@/locales'

const dict = useDict('wms_material_category')
const data = ref({ funds: [], inventoryByWarehouse: [], inventoryByCategory: [], balances: {}, topReceivables: [], topPayables: [] })
const loading = ref(false)
const num = (v) => Number(v || 0)

const kpis = computed(() => {
  const d = data.value
  const b = d.balances || {}
  return [
    { key: 'cash', label: t('jxc.assets.cash'), value: d.cashBalance },
    { key: 'bank', label: t('jxc.assets.bank'), value: d.bankBalance },
    { key: 'inventory', label: t('jxc.assets.inventory'), value: d.inventoryAmount },
    { key: 'ar', label: t('jxc.assets.receivable'), value: b.arBalance,
      extra: num(b.arOverdue) ? t('jxc.assets.overdue', { v: money(b.arOverdue) }) : '' },
    { key: 'prepaid', label: t('jxc.assets.prepaid'), value: b.advancePaid },
    { key: 'ap', label: t('jxc.assets.payable'), value: b.apBalance },
    { key: 'advance', label: t('jxc.assets.advance'), value: b.advanceReceived },
    { key: 'net', label: t('jxc.assets.netAssets'), value: d.netAssets, main: true, neg: num(d.netAssets) < 0 }
  ]
})

const fundColumns = [
  { title: t('jxc.assets.account'), key: 'account', ellipsis: true },
  { title: t('jxc.assets.balance'), key: 'balance', align: 'right', width: 140 }
]
const partnerColumns = [
  { title: t('jxc.assets.partner'), dataIndex: 'partnerName', ellipsis: true },
  { title: t('jxc.assets.amount'), key: 'amount', align: 'right', width: 160 }
]

function pie(list = [], name) {
  return {
    tooltip: { trigger: 'item', valueFormatter: (v) => t('jxc.common.yuan', { v: shortMoney(v) }) },
    legend: { type: 'scroll', bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '66%'], center: ['50%', '44%'],
      data: list.filter((r) => num(r.amount) > 0).map((r) => ({ name: name(r), value: num(r.amount) }))
    }]
  }
}

async function load() {
  loading.value = true
  try {
    data.value = await jxcAssets()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 16px;
}
.kpi {
  padding: 14px 16px;
  height: 100%;
}
.kpi.main {
  background: var(--app-primary-bg);
}
.kpi-value {
  font-size: 18px;
  font-weight: 600;
  margin: 4px 0 2px;
}
.formula {
  margin: 8px 0 16px;
}
.neg {
  color: var(--status-critical, #cf1322);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
