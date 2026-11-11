<template>
  <!-- MRP 运算：运算结果与销售预测分标签页；运算在「生产计划」中发起 -->
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="mrp-tabs">
        <a-tab-pane key="result" :tab="$t('mes.mrp.tabResults')">
          <MrpResultPanel />
        </a-tab-pane>
        <a-tab-pane key="forecast" :tab="$t('mes.forecast.title')">
          <ForecastPanel />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ForecastPanel from '@/components/mes/ForecastPanel.vue'
import MrpResultPanel from './MrpResultPanel.vue'

const tab = ref('result')
</script>

<style scoped>
.mrp-tabs :deep(> .ant-tabs-nav) {
  margin-bottom: 12px;
}
</style>
