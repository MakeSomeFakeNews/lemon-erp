<template>
  <a-modal
    :open="formatPicker.open"
    :title="$t('print.format.pickTitle')"
    :width="420"
    :ok-text="$t('print.common.print')"
    centered
    @ok="finish(true)"
    @cancel="finish(false)"
  >
    <div class="pick-doc">{{ formatPicker.title }}</div>
    <a-radio-group v-model:value="formatPicker.value" class="pick-list">
      <a-radio v-for="f in formatPicker.options" :key="f.id" :value="f.id" class="pick-item" @dblclick="finish(true)">
        <span class="pick-name">{{ formatLabel(f.name) }}</span>
        <a-tag v-if="f.isDefault" color="blue" :bordered="false">{{ $t('print.format.isDefault') }}</a-tag>
        <a-tag v-if="f.id === formatPicker.lastUsed" :bordered="false">{{ $t('print.format.lastUsed') }}</a-tag>
      </a-radio>
    </a-radio-group>
    <div v-if="formatPicker.count > 1" class="pick-tip">{{ $t('print.format.pickTip', { n: formatPicker.count }) }}</div>
  </a-modal>
</template>

<script setup>
import { formatPicker } from '@/utils/print'
import { formatLabel } from '@/utils/printTemplates'

/** 打印格式选择：单据有多份启用的格式时由 printDocs 弹出（批量打印只选择一次），选择后按该格式生成 PDF */
function finish(ok) {
  const resolve = formatPicker.resolve
  const picked = ok ? formatPicker.options.find((f) => f.id === formatPicker.value) || null : null
  Object.assign(formatPicker, { open: false, resolve: null })
  resolve?.(picked)
}
</script>

<style scoped>
.pick-doc {
  margin-bottom: 12px;
  color: var(--app-text-3);
}
.pick-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}
.pick-item {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  margin: 0;
  border: 1px solid var(--app-border);
  border-radius: 6px;
}
.pick-item:hover {
  background: var(--app-hover);
}
.pick-name {
  margin-right: 8px;
}
.pick-tip {
  margin-top: 12px;
  font-size: 12px;
  color: var(--app-text-3);
}
</style>
