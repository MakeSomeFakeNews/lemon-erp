<template>
  <a-tag v-if="item" :color="color" :bordered="false">{{ item.label }}</a-tag>
  <span v-else-if="value !== null && value !== undefined && value !== ''">{{ value }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { useDictStore } from '@/store/dict'
import { findDict } from '@/utils/dict'

/** 字典标签：<DictTag type="sys_common_status" :value="record.status" /> */
const props = defineProps({
  type: { type: String, required: true },
  value: [String, Number]
})

const store = useDictStore()
store.load(props.type).catch(() => {})

const item = computed(() => findDict(store.cache[props.type], props.value))
const color = computed(() => (item.value?.tagColor && item.value.tagColor !== 'default' ? item.value.tagColor : undefined))
</script>
