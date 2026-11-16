<template>
  <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('jxc.common.allWarehouses')" class="sgf-s" />
  <MaterialCategorySelect v-model="query.category" :placeholder="$t('jxc.common.allCategories')" class="sgf-s" />
  <a-select v-model:value="query.brand" :options="toOptions(dict.wms_material_brand)" allow-clear show-search option-filter-prop="label"
    :placeholder="$t('jxc.common.allBrands')" class="sgf-s" />
  <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('jxc.overstock.keyword')" class="sgf" @press-enter="emit('search')" />
</template>

<script setup>
import MaterialCategorySelect from '@/components/wms/MaterialCategorySelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'

/**
 * 积压分析、库存分析共用的商品条件：仓库、类别（含下级）、品牌、商品描述（编码 / 名称 / 规格 / 简拼）。
 * query 为页面的 reactive 查询对象，直接修改其中的 warehouseId、category、brand、keyword；回车触发 search
 */
defineProps({ query: { type: Object, required: true } })
const emit = defineEmits(['search'])
const dict = useDict('wms_material_brand')
</script>

<style scoped>
.sgf {
  width: 220px;
}
.sgf-s {
  width: 150px;
}
@media (max-width: 768px) {
  .sgf,
  .sgf-s {
    width: 100%;
  }
}
</style>
