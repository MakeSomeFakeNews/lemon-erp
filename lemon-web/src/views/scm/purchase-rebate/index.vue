<template>
  <RebatePage :kind="RebateKind.PURCHASE" />
</template>

<script setup>
import RebatePage from '@/components/scm/RebatePage.vue'
import { RebateKind } from '@/constants/enums'

/** 采购返利：供应商给我方的返利协议，结算后生成红字应付单，付款时勾选即可与欠款冲抵 */
</script>
