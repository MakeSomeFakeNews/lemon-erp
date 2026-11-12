<template>
  <RemoteSelect
    :value="value"
    :label="label"
    :fetch="fetch"
    :source-key="carrier ? 'carrier' : type"
    :fetch-by-ids="(ids) => partnersByIds(carrier ? 'supplier' : type, ids)"
    :to-label="(p) => `${p.code} ${p.name}`"
    :allow-clear="allowClear"
    :disabled="disabled"
    :placeholder="placeholder || $t(type === 'supplier' ? 'scm.partnerSelect.searchSupplier' : 'scm.partnerSelect.searchCustomer')"
    :dropdown-width="360"
    @update:value="(v) => emit('update:value', v)"
    @select="(p) => emit('select', p)"
  >
    <template v-if="carrier && !marked" #header>
      <div class="carrier-hint">{{ $t('scm.freight.carrierUnmarked') }}</div>
    </template>
  </RemoteSelect>
</template>

<script setup>
import { ref } from 'vue'
import RemoteSelect from '@/components/common/RemoteSelect.vue'
import { carrierOptions, partnerOptions, partnersByIds } from '@/api/scm/partner'

/**
 * 供应商 / 客户下拉：启用的往来单位远程分页搜索（编码、名称、联系人、电话、拼音首字母、去掉分隔符的缩写），
 * 打开时取前 20 条，滚动加载更多。
 * - type：supplier | customer，变化时重新加载选项（已选值由页面清空）
 * - v-model:value：ID；label：当前值的显示名（编辑时回显，不传时按 ID 取名称）
 * - @select(partner)：选中后返回 { id, code, name, contact, phone, address }
 * - carrier：承运商（采购运费、发运单），只列出标记为物流公司的供应商；还没有标记时列出全部供应商并提示去标记
 */
const props = defineProps({
  type: { type: String, required: true },
  value: Number,
  label: String,
  placeholder: String,
  allowClear: Boolean,
  disabled: Boolean,
  carrier: Boolean
})
const emit = defineEmits(['update:value', 'select'])

/** 承运商：是否已有标记为物流公司的供应商 */
const marked = ref(true)

async function fetch({ keyword, current, size }) {
  if (!props.carrier) return partnerOptions(props.type, keyword, { current, size })
  const res = await carrierOptions(keyword, { current, size })
  marked.value = res.marked
  return res.options
}
</script>

<style scoped>
.carrier-hint {
  padding: 4px 12px 8px;
  color: var(--app-text-3);
  font-size: 12px;
  white-space: normal;
}
</style>
