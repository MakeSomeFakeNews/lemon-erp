<template>
  <a-select
    :value="value ?? undefined"
    allow-clear
    show-search
    option-filter-prop="label"
    :placeholder="placeholder"
    :options="options"
    @change="(v) => emit('update:value', v ?? null)"
  />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { positionOptions } from '@/api/hr'
import { cached } from '@/utils/cache'

/** 职位下拉（启用的职位） */
defineProps({
  value: Number,
  placeholder: { type: String, default: '选择职位' }
})
const emit = defineEmits(['update:value'])

const options = ref([])
onMounted(async () => {
  const list = await cached('hr-positions', positionOptions)
  options.value = list.map((p) => ({ value: p.id, label: p.grade ? `${p.name}（${p.grade}）` : p.name }))
})
</script>
