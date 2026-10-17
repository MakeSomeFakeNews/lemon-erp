<template>
  <div class="serial-input">
    <a-select
      :value="list"
      mode="tags"
      :token-separators="[',', '，', ';', '；', ' ', '\n']"
      :options="options"
      :loading="loading"
      :disabled="disabled"
      :max-tag-count="maxTagCount"
      :placeholder="placeholder"
      :not-found-content="source ? (loading ? $t('common.msg.loading') : $t('wms.select.noSerial')) : null"
      @dropdown-visible-change="(open) => open && loadOptions()"
      @change="onChange"
    />
    <span v-if="expected != null" class="serial-count" :class="{ warn: list.length !== expected }">
      {{ list.length }} / {{ expected }}
    </span>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { availableSerials } from '@/api/wms/stock'
import { joinSerials, splitSerials } from './wms'
import { t } from '@/locales'

/**
 * 序列号录入：支持扫码枪逐个录入、粘贴多个（逗号 / 空格 / 换行分隔）。
 * - v-model:value：逗号分隔的序列号字符串（与后端一致）
 * - source：{ warehouseId, locationId, materialId, batchNo }，传入时下拉列出该位置的在库序列号（出库、调拨）
 * - expected：应录入的个数（数量），不一致时提示
 */
const props = defineProps({
  value: String,
  source: Object,
  expected: Number,
  disabled: Boolean,
  maxTagCount: { type: Number, default: 6 },
  placeholder: { type: String, default: () => t('wms.doc.serialInputTip') }
})
const emit = defineEmits(['update:value'])

const list = computed(() => splitSerials(props.value))
const available = ref([])
const loading = ref(false)
let loadedKey = ''

const options = computed(() => available.value.map((sn) => ({ value: sn, label: sn })))

// 库存位置变化后重新加载
watch(
  () => JSON.stringify(props.source || null),
  () => {
    available.value = []
    loadedKey = ''
  }
)

async function loadOptions() {
  if (!props.source?.materialId || !props.source?.warehouseId) return
  const key = JSON.stringify(props.source)
  if (key === loadedKey) return
  loading.value = true
  try {
    available.value = await availableSerials(props.source)
    loadedKey = key
  } finally {
    loading.value = false
  }
}

function onChange(values) {
  // 去重并去掉空白
  const unique = [...new Set(values.flatMap((v) => splitSerials(v)))]
  emit('update:value', joinSerials(unique))
}
</script>

<style scoped>
.serial-input {
  display: flex;
  align-items: center;
  gap: 6px;
}
.serial-input :deep(.ant-select) {
  flex: 1;
  min-width: 0;
}
.serial-count {
  flex: none;
  font-size: 12px;
  color: var(--app-text-3);
}
.serial-count.warn {
  color: #faad14;
}
</style>
