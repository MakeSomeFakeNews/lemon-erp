<template>
  <template v-for="sec in sections" :key="sec.key">
    <div class="section-title">
      {{ sec.title }}
      <span class="section-hint">{{ sec.key === 'columns' ? $t('print.template.sectionHintColumns') : $t('print.template.sectionHint') }}</span>
    </div>
    <div class="rows">
      <div v-for="(row, i) in config[sec.key]" :key="row[sec.nameKey]" :class="['row', { hidden: row.hidden }]">
        <a-checkbox :checked="!row.hidden" @update:checked="(v) => (row.hidden = !v)" />
        <span class="row-name" :title="printLabel(row[sec.nameKey])">{{ printLabel(row[sec.nameKey]) }}</span>
        <a-input v-model:value="row.alias" size="small" :placeholder="$t('print.template.alias')" :maxlength="20" class="row-alias" />
        <a-input-number
          v-if="sec.key === 'columns'"
          v-model:value="row.width"
          size="small"
          :min="5"
          :max="200"
          :placeholder="$t('print.template.auto')"
          class="row-width"
        >
          <template #addonAfter>mm</template>
        </a-input-number>
        <span class="row-move">
          <a-button type="text" size="small" :disabled="i === 0" @click="move(sec.key, i, -1)"><ArrowUpOutlined /></a-button>
          <a-button type="text" size="small" :disabled="i === config[sec.key].length - 1" @click="move(sec.key, i, 1)">
            <ArrowDownOutlined />
          </a-button>
        </span>
      </div>
    </div>
  </template>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowDownOutlined, ArrowUpOutlined } from '@ant-design/icons-vue'
import { printLabel } from '@/utils/printTemplates'

/** 打印模板的表头字段、明细列、签字栏：勾选显示、改名、调整顺序，明细列可设列宽（直接修改页面的编辑配置） */
const props = defineProps({
  /** 编辑中的配置（templateConfig.js 的 buildConfig） */
  config: { type: Object, required: true }
})
const { t } = useI18n()

const ALL_SECTIONS = [
  { key: 'fields', title: t('print.overlay.fields'), nameKey: 'label' },
  { key: 'columns', title: t('print.template.columns'), nameKey: 'title' },
  { key: 'signs', title: t('print.overlay.signs'), nameKey: 'label' }
]
const sections = computed(() => ALL_SECTIONS.filter((s) => props.config[s.key]?.length))

function move(key, i, delta) {
  const rows = props.config[key]
  const [row] = rows.splice(i, 1)
  rows.splice(i + delta, 0, row)
}
</script>

<style scoped>
.section-title {
  margin: 16px 0 8px;
  font-weight: 600;
  color: var(--app-text);
}
.section-hint {
  margin-left: 8px;
  font-weight: 400;
  font-size: 12px;
  color: var(--app-text-3);
}
.rows {
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
}
.row + .row {
  border-top: 1px solid var(--app-border);
}
.row.hidden .row-name {
  color: var(--app-text-3);
  text-decoration: line-through;
}
.row-name {
  width: 96px;
  flex: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row-alias {
  flex: 1;
  min-width: 0;
}
.row-width {
  width: 104px;
  flex: none;
}
.row-move {
  flex: none;
  display: flex;
}
</style>
