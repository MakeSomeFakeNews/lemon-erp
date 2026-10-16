<template>
  <a-select
    :value="value ?? undefined"
    show-search
    allow-clear
    :placeholder="placeholder"
    :options="options"
    :loading="loading"
    option-filter-prop="label"
    @change="(v) => emit('update:value', v ?? null)"
  />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { workCenterOptions } from '@/api/mes/base'

/** 工作中心下拉：启用的工作中心 */
defineProps({
  value: Number,
  placeholder: { type: String, default: '请选择工作中心' }
})
const emit = defineEmits(['update:value'])

const list = ref([])
const loading = ref(false)
const options = computed(() => list.value.map((w) => ({ value: w.id, label: `${w.code} ${w.name}` })))

onMounted(async () => {
  loading.value = true
  try {
    list.value = await workCenterOptions()
  } finally {
    loading.value = false
  }
})
</script>
