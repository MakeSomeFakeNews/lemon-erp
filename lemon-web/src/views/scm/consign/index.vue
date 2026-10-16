<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-alert
        v-if="!canSettle"
        type="warning"
        show-icon
        class="tip"
        message="没有寄售结算权限（scm:consign:settle），只能查看寄售仓库存"
      />
      <a-tabs v-model:active-key="tab" destroy-inactive-tab-pane>
        <a-tab-pane key="customer" tab="客户寄售结算">
          <ConsignSettlePanel mode="customer" />
        </a-tab-pane>
        <a-tab-pane key="supplier" tab="供应商寄售结算（VMI）">
          <ConsignSettlePanel mode="supplier" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import ConsignSettlePanel from '@/components/scm/ConsignSettlePanel.vue'
import { useUserStore } from '@/store/user'

/** 寄售结算：客户寄售（售出结算）与供应商寄售 / VMI（领用结算） */
const userStore = useUserStore()
const canSettle = computed(() => userStore.hasPerm('scm:consign:settle'))
const tab = ref('customer')
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
</style>
