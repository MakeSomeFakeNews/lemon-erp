<template>
  <div class="followups">
    <div class="bar">
      <span class="sub-text">{{ $t('crm.followupTimeline.total', { n: total }) }}</span>
      <a-button v-perm="'crm:followup:add'" type="primary" size="small" @click="modalOpen = true">
        <template #icon><PlusOutlined /></template>{{ $t('crm.common.writeFollowup') }}
      </a-button>
    </div>
    <a-spin :spinning="loading">
      <a-empty v-if="!list.length" :description="$t('crm.followupTimeline.empty')" />
      <a-timeline v-else>
        <a-timeline-item v-for="f in list" :key="f.id">
          <div class="head">
            <DictTag type="crm_follow_method" :value="f.method" />
            <span>{{ f.followTime }}</span>
            <span class="sub-text">{{ f.createByName }}</span>
            <a-tag v-if="showTarget" :color="targetType(f.targetType).color" :bordered="false">
              {{ targetType(f.targetType).label }} · {{ f.targetName }}
            </a-tag>
          </div>
          <div class="content">{{ f.content }}</div>
          <div v-if="f.nextTime" class="sub-text">
            {{ $t('crm.followupTimeline.next', { time: f.nextTime }) }}<template v-if="f.nextPlan">{{ $t('crm.followupTimeline.nextPlan', { plan: f.nextPlan }) }}</template>
          </div>
        </a-timeline-item>
      </a-timeline>
    </a-spin>
    <FollowupModal v-model:open="modalOpen" :target-type="targetTypeValue" :target-id="targetId" @success="onSaved" />
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FollowupModal from './FollowupModal.vue'
import { pageFollowup } from '@/api/crm'
import { targetType } from './crm'

/**
 * 跟进记录时间线：按对象（targetType + targetId）或客户（customerId，含客户商机的跟进）查询，可直接写跟进
 */
const props = defineProps({
  targetTypeValue: String,
  targetId: Number,
  customerId: Number,
  showTarget: Boolean
})
const emit = defineEmits(['saved'])

const list = ref([])
const total = ref(0)
const loading = ref(false)
const modalOpen = ref(false)

async function load() {
  if (!props.targetId && !props.customerId) return
  loading.value = true
  try {
    const params = props.customerId
      ? { customerId: props.customerId }
      : { targetType: props.targetTypeValue, targetId: props.targetId }
    const res = await pageFollowup({ current: 1, size: 50, ...params })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onSaved() {
  load()
  emit('saved')
}

onMounted(load)
watch(() => [props.targetId, props.customerId], load)
defineExpose({ load })
</script>

<style scoped>
.bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.content {
  white-space: pre-wrap;
  margin-bottom: 4px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
