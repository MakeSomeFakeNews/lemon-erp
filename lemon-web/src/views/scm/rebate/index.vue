<template>
  <RebatePage :kind="RebateKind.SALES" />
</template>

<script setup>
import RebatePage from '@/components/scm/RebatePage.vue'
import { RebateKind } from '@/constants/enums'

/** 销售返利：客户 × 期间的返利协议（阶梯比例或按项目填写金额），结算后生成红字应收或计入返利余额在下次订单抵扣 */
</script>
