<template>
  <a-card :bordered="false" class="page-card">
    <div class="head">
      <span class="section-title">销售预测</span>
      <a-space wrap>
        <a-input-search v-model:value="keyword" placeholder="物料编码或名称" allow-clear style="width: 200px" @search="search" />
        <a-button v-perm="'mes:mrp:run'" type="primary" @click="openAdd">
          <template #icon><PlusOutlined /></template>预测
        </a-button>
      </a-space>
    </div>
    <div class="sub-text tip">MRP 运算时可选择计入未来几个月的预测：预测数量扣减同一月份已审核、未完成的销售订单后作为需求。</div>
    <a-table
      row-key="id"
      size="small"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'material'">
          {{ record.materialCode }} {{ record.materialName }}
          <span class="sub-text">{{ record.spec }}</span>
        </template>
        <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
        <template v-else-if="column.key === 'action'">
          <span v-perm="'mes:mrp:run'">
            <a-button type="link" size="small" @click="openEdit(record)">修改</a-button>
            <a-popconfirm title="确定删除该预测？" @confirm="remove(record.id)">
              <a-button type="link" danger size="small">删除</a-button>
            </a-popconfirm>
          </span>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="open" title="销售预测" :confirm-loading="saving" destroy-on-close @ok="submit">
      <a-form layout="vertical">
        <a-form-item label="物料" required>
          <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" />
        </a-form-item>
        <a-form-item label="月份" required>
          <PeriodPicker v-model:value="form.period" />
        </a-form-item>
        <a-form-item label="预测数量" required>
          <a-input-number v-model:value="form.qty" :min="0.0001" class="full" />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { deleteForecast, pageForecast, saveForecast } from '@/api/mes/plan'
import { currentPeriod } from '@/components/fin/fin'
import { formatQty } from '@/components/wms/wms'
import { useTable } from '@/composables/useTable'

const columns = [
  { title: '月份', dataIndex: 'period', width: 90 },
  { title: '物料', key: 'material' },
  { title: '预测数量', key: 'qty', width: 140, align: 'right' },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 130 }
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
    message.warning('请填写物料、月份和数量')
    return
  }
  saving.value = true
  try {
    await saveForecast({ materialId: f.materialId, period: f.period, qty: f.qty, remark: f.remark || null })
    message.success('保存成功')
    open.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(id) {
  await deleteForecast(id)
  message.success('删除成功')
  load()
}
</script>

<style scoped>
.head {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.tip {
  margin-bottom: 8px;
}
.full {
  width: 100%;
}
</style>
