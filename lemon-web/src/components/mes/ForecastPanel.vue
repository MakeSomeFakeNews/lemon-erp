<template>
  <!-- 销售预测（MRP 运算页面的标签页）：MRP 运算时可选择计入 -->
  <div class="forecast-panel">
    <div class="head">
      <a-space wrap>
        <a-button v-perm="'mes:mrp:run'" type="primary" @click="openAdd">
          <template #icon><PlusOutlined /></template>{{ $t('mes.forecast.add') }}
        </a-button>
        <a-input-search v-model:value="keyword" :placeholder="$t('mes.forecast.materialKeyword')" allow-clear class="keyword" @search="search" />
      </a-space>
      <span class="sub-text"><InfoCircleOutlined class="help" /> {{ $t('mes.forecast.tip') }}</span>
    </div>
    <a-table
      row-key="id"
      size="small"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ y: '100%' }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'material'">
          {{ record.materialCode }} {{ record.materialName }}
          <span v-if="record.spec" class="sub-text">{{ record.spec }}</span>
        </template>
        <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
        <template v-else-if="column.key === 'action'">
          <TableActions
            :actions="[
              { type: 'modify', perm: 'mes:mrp:run', onClick: () => openEdit(record) },
              { type: 'delete', perm: 'mes:mrp:run', confirm: $t('mes.forecast.confirmDelete'), onClick: () => remove(record.id) }
            ]"
          />
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="open" :title="$t('mes.forecast.title')" :confirm-loading="saving" destroy-on-close @ok="submit">
      <a-form layout="vertical">
        <a-form-item :label="$t('mes.forecast.material')" required>
          <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" />
        </a-form-item>
        <a-form-item :label="$t('mes.forecast.month')" required>
          <PeriodPicker v-model:value="form.period" />
        </a-form-item>
        <a-form-item :label="$t('mes.forecast.qty')" required>
          <a-input-number v-model:value="form.qty" :min="0.0001" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { InfoCircleOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { deleteForecast, pageForecast, saveForecast } from '@/api/mes/plan'
import { currentPeriod } from '@/components/fin/fin'
import { formatQty } from '@/components/wms/wms'
import { useTable } from '@/composables/useTable'
import { useAction } from '@/composables/useAction'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
const { t } = useI18n()

const columns = [
  { title: t('mes.forecast.month'), dataIndex: 'period', width: 90 },
  { title: t('mes.forecast.material'), key: 'material' },
  { title: t('mes.forecast.qty'), key: 'qty', width: 150, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  actionColumn(2, [], { fixed: undefined })
]
const keyword = ref()
const query = reactive({ beginPeriod: currentPeriod() })
const { list, loading, pagination, load, search, onTableChange } = useTable(
  (p) => pageForecast({ ...p, keyword: keyword.value || undefined }),
  query
)
onMounted(load)

const open = ref(false)
const saving = ref(false)
const form = ref({})
function openAdd() {
  form.value = { materialId: undefined, period: currentPeriod(), qty: undefined, remark: '' }
  open.value = true
}
function openEdit(r) {
  form.value = { materialId: r.materialId, materialLabel: `${r.materialCode} ${r.materialName}`, period: r.period, qty: Number(r.qty), remark: r.remark || '' }
  open.value = true
}
async function submit() {
  const f = form.value
  if (!f.materialId || !f.period || !f.qty) {
    message.warning(t('mes.forecast.required'))
    return
  }
  saving.value = true
  try {
    await saveForecast({ materialId: f.materialId, period: f.period, qty: f.qty, remark: f.remark || null })
    message.success(t('common.msg.saveSuccess'))
    open.value = false
    load()
  } finally {
    saving.value = false
  }
}
const remove = useAction(deleteForecast, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.forecast-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.head {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.keyword {
  width: 220px;
}
.help {
  color: var(--app-text-3);
  cursor: help;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
