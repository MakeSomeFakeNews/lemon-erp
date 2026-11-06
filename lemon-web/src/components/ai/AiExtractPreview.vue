<template>
  <div class="ai-preview">
    <div class="head">
      <a-tag color="processing" :bordered="false">{{ result.label }}</a-tag>
      <span class="meta">
        {{ $t(`ai.extract.source.${result.source}`) }} ·
        {{ $t('ai.extract.tokens', { n: (result.promptTokens || 0) + (result.completionTokens || 0) }) }}
      </span>
    </div>

    <a-alert
      v-if="result.warnings.length"
      type="warning"
      show-icon
      class="block"
      :message="$t('ai.extract.warnTitle', { n: result.warnings.length })"
    >
      <template #description>
        <ul class="warn-list">
          <li v-for="(w, i) in result.warnings" :key="i">{{ w.message }}</li>
        </ul>
      </template>
    </a-alert>
    <a-alert v-else type="success" show-icon class="block" :message="$t('ai.extract.noWarn')" />

    <a-descriptions v-if="scalars.length" size="small" bordered :column="{ xs: 1, sm: 2 }" class="block">
      <a-descriptions-item v-for="f in scalars" :key="f.key" :label="label(f.key)">{{ f.value }}</a-descriptions-item>
    </a-descriptions>

    <template v-for="l in lists" :key="l.key">
      <div class="list-title">{{ $t('ai.extract.listTitle', { name: label(l.key), n: l.rows.length }) }}</div>
      <div class="table-wrap">
        <a-table size="small" :columns="l.columns" :data-source="l.rows" :pagination="false" row-key="_i" :scroll="{ x: l.columns.length * 110 }" />
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

/**
 * 智能录入的识别结果预览：目标、来源、用量，需要核对的提示，模型识别出的字段（明细以表格显示）。
 * result 为 runExtract 的返回值
 */
const props = defineProps({
  result: { type: Object, required: true }
})
const { t, te } = useI18n()

const label = (key) => (te(`ai.extract.field.${key}`) ? t(`ai.extract.field.${key}`) : key)
const show = (v) => (v === null || v === undefined || v === '' ? null : typeof v === 'object' ? JSON.stringify(v) : String(v))

const entries = computed(() => Object.entries(props.result.extracted || {}))
const scalars = computed(() =>
  entries.value.filter(([, v]) => !Array.isArray(v) && show(v) !== null).map(([key, v]) => ({ key, value: show(v) }))
)
const lists = computed(() =>
  entries.value
    .filter(([, v]) => Array.isArray(v) && v.length)
    .map(([key, v]) => {
      const keys = []
      v.forEach((row) => Object.keys(row || {}).forEach((k) => !keys.includes(k) && row[k] != null && keys.push(k)))
      return {
        key,
        columns: [
          { title: '#', dataIndex: '_i', width: 48 },
          ...keys.map((k) => ({ title: label(k), dataIndex: k, ellipsis: true, customRender: ({ text }) => show(text) ?? '' }))
        ],
        rows: v.map((row, i) => ({ ...row, _i: i + 1 }))
      }
    })
)
</script>

<style scoped>
.head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.meta {
  color: var(--app-text-3);
  font-size: 12px;
}
.block {
  margin-bottom: 12px;
}
.warn-list {
  margin: 0;
  padding-left: 18px;
}
.list-title {
  font-weight: 500;
  margin: 4px 0 8px;
}
.table-wrap {
  overflow-x: auto;
}
</style>
