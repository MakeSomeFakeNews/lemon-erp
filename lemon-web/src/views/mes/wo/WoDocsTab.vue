<template>
  <a-table row-key="id" size="small" :columns="columns" :data-source="docs" :pagination="false">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'kind'"><DictTag type="mes_wo_doc_kind" :value="record.kind" /></template>
      <template v-else-if="column.key === 'totalQty'">{{ formatQty(record.totalQty) }}</template>
    </template>
  </a-table>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { formatQty } from '@/components/wms/wms'

/** 工单详情 - 单据（领料、退料、补料、报废、完工入库） */
defineProps({ docs: { type: Array, default: () => [] } })
const { t } = useI18n()

const columns = [
  { title: t('mes.wo.docNo'), dataIndex: 'docNo', width: 170 },
  { title: t('common.field.type'), key: 'kind', width: 130, dict: 'mes_wo_doc_kind' },
  { title: t('mes.wo.warehouse'), dataIndex: 'warehouseName', width: 130 },
  { title: t('mes.wo.bizDate'), dataIndex: 'bizDate', width: 120 },
  { title: t('common.field.quantity'), key: 'totalQty', width: 90, align: 'right' },
  { title: t('mes.wo.wmsDoc'), dataIndex: 'wmsDocNo', width: 170 },
  { title: t('mes.wo.preparedByCol'), dataIndex: 'createByName', width: 110 }
]
</script>
