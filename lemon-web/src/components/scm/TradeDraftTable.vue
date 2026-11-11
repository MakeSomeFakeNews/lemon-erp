<template>
  <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
    <slot name="toolbar" />
  </TableToolbar>

  <a-table
    row-key="id"
    :size="size"
    :columns="visibleColumns"
    :data-source="list"
    :loading="loading"
    :pagination="pagination"
    :scroll="{ x: scrollX, y: '100%' }"
    @change="onTableChange"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'docNo'">
        <a v-if="canEdit" @click="emit('edit', record)">{{ record.docNo }}</a>
        <span v-else>{{ record.docNo }}</span>
        <div><a-tag :bordered="false">{{ $t('scm.trade.draftTag') }}</a-tag></div>
      </template>
      <template v-else-if="column.key === 'orderNo'">
        <a-tag v-if="!record.orderId" color="blue" :bordered="false">{{ $t('scm.trade.direct') }}</a-tag>
        <span v-else>{{ record.orderNo }}</span>
      </template>
      <template v-else-if="column.key === 'partner'">
        <div>{{ record.partnerName }}</div>
        <div class="sub-text">{{ record.partnerCode }}</div>
      </template>
      <template v-else-if="column.key === 'totalQty'">{{ formatQty(record.totalQty) }}</template>
      <template v-else-if="column.key === 'totalAmount'">
        ¥{{ formatMoney(record.totalAmount) }}
        <div v-if="Number(record.freight)" class="sub-text">{{ $t('scm.freight.value', { amount: '¥' + formatMoney(record.freight) }) }}</div>
      </template>
      <template v-else-if="column.key === 'action'">
        <TableActions
          :actions="[
            { type: 'edit', perm: `scm:${kind}:add`, onClick: () => emit('edit', record) },
            { type: 'audit', label: $t('scm.trade.post'), perm: `scm:${kind}:post`, onClick: () => post(record) },
            { type: 'delete', perm: `scm:${kind}:add`, onClick: () => remove(record) }
          ]"
        />
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { delTradeDraft, pageTradeDraft, postTradeDraft } from '@/api/scm/trade'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { formatQty } from '@/components/wms/wms'
import { ORDER_KINDS, TRADE_KINDS, formatMoney } from './scm'

/**
 * 出入库草稿列表（业务参数 scm.tradeDoc.draft），在出入库列表页的「草稿」页签中显示，与单据列表共用查询条件。
 * 编辑（点击单号）触发 edit，审核、删除后触发 changed（posted 为是否生成了单据）；工具栏按钮放在 toolbar 插槽
 */
const props = defineProps({
  kind: { type: String, required: true },
  /** 查询条件（与单据列表共用）：docNo、orderNo、partnerId、warehouseId、range */
  query: { type: Object, required: true }
})
const emit = defineEmits(['edit', 'changed'])
const showSearch = defineModel('showSearch', { type: Boolean, default: true })
const { t } = useI18n()
const userStore = useUserStore()
const conf = TRADE_KINDS[props.kind]
const canEdit = computed(() => userStore.hasPerm(`scm:${props.kind}:add`))

const columns = [
  { title: t('scm.trade.docNo'), key: 'docNo', width: 170 },
  { title: t('scm.trade.orderNo'), key: 'orderNo', width: 170 },
  { title: ORDER_KINDS[conf.orderKind].partnerLabel, key: 'partner', width: 200 },
  { title: t('scm.trade.warehouse'), dataIndex: 'warehouseName', width: 130 },
  { title: t('scm.trade.bizDate'), dataIndex: 'bizDate', width: 120 },
  { title: t('common.field.quantity'), key: 'totalQty', width: 100, align: 'right' },
  { title: t('scm.trade.draftAmount'), key: 'totalAmount', width: 140, align: 'right' },
  { title: t('scm.trade.createBy'), dataIndex: 'createByName', width: 120 },
  { title: t('scm.trade.updateTime'), dataIndex: 'updateTime', width: 170 },
  actionColumn(3, [t('common.action.edit'), t('scm.trade.post'), t('common.action.delete')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, `scm-${props.kind}-draft`)

const { list, loading, size, pagination, load, search, onTableChange } = useTable((params) => pageTradeDraft(props.kind, params), props.query, {
  params: ({ range, ...rest }) => ({ ...rest, beginDate: range?.[0], endDate: range?.[1] })
})

const post = useAction((r) => postTradeDraft(props.kind, r.id), {
  confirm: (r) => ({ title: t('scm.trade.postConfirm', { no: r.docNo }), content: t('scm.trade.postConfirmContent', { kind: conf.label }) }),
  successKey: () => t('scm.trade.kindSuccess', { kind: conf.label }),
  reload: () => {
    load()
    emit('changed', { posted: true })
  }
})

const remove = useAction((r) => delTradeDraft(props.kind, r.id), {
  confirm: (r) => ({ title: t('scm.trade.deleteDraftConfirm', { no: r.docNo }), okType: 'danger' }),
  successKey: 'common.msg.deleteSuccess',
  reload: () => {
    load()
    emit('changed', { posted: false })
  }
})

onMounted(load)

defineExpose({ load, search })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
