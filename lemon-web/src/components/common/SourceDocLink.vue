<template>
  <!-- 来源单号：点击打开来源单据（出入库单、工单、项目在当前页面打开详情，其他跳转到对应页面打开），不能打开时只显示单号 -->
  <span>
    <a v-if="target" class="source-link" @click.stop="open">{{ no || '-' }}</a>
    <span v-else>{{ no || '' }}</span>
    <component :is="detail" v-if="detail" v-model:open="visible" v-bind="detailProps" />
  </span>
</template>

<script setup>
import { computed, nextTick, ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { sourceTarget } from './sourceDocs'

/** 应收应付单等的来源单据链接：type 为来源类型（BillSource），id 为来源单据 ID，billKind 区分应收 / 应付（返利、订单尾差），项目开票传 projectId */
const props = defineProps({
  type: { type: String, default: undefined },
  id: { type: Number, default: undefined },
  no: { type: String, default: undefined },
  billKind: { type: String, default: undefined },
  projectId: { type: Number, default: undefined }
})

// 详情组件按需加载：列表中每行一个链接，只有点击后才加载、创建
const loaders = {
  trade: () => import('@/components/scm/TradeDocDetail.vue'),
  ticket: () => import('@/components/crm/TicketDetail.vue'),
  project: () => import('@/components/pm/ProjectDetail.vue')
}

const userStore = useUserStore()
const router = useRouter()
const target = computed(() =>
  sourceTarget({ type: props.type, id: props.id, no: props.no, billKind: props.billKind, projectId: props.projectId }, (p) => userStore.hasPerm(p))
)
const detail = shallowRef()
const visible = ref(false)
const detailProps = computed(() => {
  const t = target.value
  if (!t) return {}
  return t.trade ? { kind: t.trade, docId: props.id } : t.ticket ? { ticketId: props.id } : { projectId: props.projectId }
})

async function open() {
  const t = target.value
  if (t.path) {
    router.push({ path: t.path, query: t.byNo ? { keyword: props.no } : { id: props.id } })
    return
  }
  // 详情组件在 open 由 false 变为 true 时加载数据：先创建（关闭状态）再打开
  if (!detail.value) {
    detail.value = (await loaders[t.trade ? 'trade' : t.ticket ? 'ticket' : 'project']()).default
    await nextTick()
  }
  visible.value = true
}
</script>

<style scoped>
.source-link {
  word-break: break-all;
}
</style>
