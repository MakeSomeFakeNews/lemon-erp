<template>
  <a-select
    :value="value ?? undefined"
    show-search
    :allow-clear="allowClear"
    option-filter-prop="label"
    :placeholder="placeholder"
    :options="options"
    :loading="loading"
    @change="onChange"
  />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { projectOptions } from '@/api/pm'

/**
 * 项目下拉：未完成的项目；mine 只列出我参与的项目（填报工时）
 * - @select(project)：选中后返回项目
 */
const props = defineProps({
  value: Number,
  mine: Boolean,
  allowClear: { type: Boolean, default: true },
  placeholder: { type: String, default: '选择项目' }
})
const emit = defineEmits(['update:value', 'select'])

const list = ref([])
const options = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    list.value = await projectOptions(props.mine)
    options.value = list.value.map((p) => ({ value: p.id, label: `${p.code} ${p.name}` }))
  } finally {
    loading.value = false
  }
})

function onChange(v) {
  emit('update:value', v ?? null)
  emit('select', list.value.find((p) => p.id === v))
}
</script>
