<template>
  <!-- 客户 / 供应商资料合并（issue #95）：选择保留的资料和被合并的资料，预览影响范围后确认合并 -->
  <a-modal
    v-model:open="visible"
    :title="tx.title"
    :width="720"
    :ok-text="$t('scm.partnerMerge.confirm')"
    :ok-button-props="{ danger: true, disabled: !preview }"
    :confirm-loading="saving"
    destroy-on-close
    @ok="submit"
  >
    <a-alert type="warning" show-icon class="block" :message="$t('scm.partnerMerge.tip')">
      <template #description>
        <ul class="rules">
          <li>{{ $t('scm.partnerMerge.ruleKeep') }}</li>
          <li>{{ $t('scm.partnerMerge.ruleDocs') }}</li>
          <li>{{ tx.rulePrice }}</li>
          <li>{{ $t('scm.partnerMerge.ruleLink') }}</li>
        </ul>
      </template>
    </a-alert>
    <a-form layout="vertical">
      <a-form-item :label="$t('scm.partnerMerge.sources')" :extra="$t('scm.partnerMerge.sourcesExtra')" required>
        <div class="sources">
          <a-tag v-for="s in sources" :key="s.id" closable @close.prevent="removeSource(s.id)">{{ s.code }} {{ s.name }}</a-tag>
          <PartnerSelect :key="addKey" :type="type" :placeholder="$t('scm.partnerMerge.addSource')" class="add" @select="addSource" />
        </div>
      </a-form-item>
      <a-form-item :label="$t('scm.partnerMerge.target')" :extra="$t('scm.partnerMerge.targetExtra')" required>
        <PartnerSelect v-model:value="targetId" :type="type" :placeholder="$t('scm.partnerMerge.targetPh')" />
      </a-form-item>
    </a-form>

    <a-spin :spinning="loading">
      <template v-if="preview">
        <div class="sub-title">{{ $t('scm.partnerMerge.impacts') }}</div>
        <a-table row-key="labelKey" size="small" :columns="columns" :data-source="preview.impacts" :pagination="false" :scroll="{ y: 280 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'conflicts'">
              <span v-if="record.conflicts" class="warn">{{ record.conflicts }}</span>
              <span v-else class="sub-text">-</span>
            </template>
          </template>
        </a-table>
        <div v-if="!preview.impacts.length" class="sub-text">{{ $t('scm.partnerMerge.noImpact') }}</div>
        <div v-if="preview.linkedName" class="sub-text link">{{ tx.linked(preview.linkedName) }}</div>
      </template>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { mergePartner, previewPartnerMerge } from '@/api/scm/partner'
import { t } from '@/locales'

/**
 * type：customer | supplier。open(record) 以 record 为被合并的资料打开（可再添加其他重复的资料），选择保留的资料后自动预览影响范围；
 * 合并成功后触发 merged
 */
const props = defineProps({
  type: { type: String, required: true }
})
const emit = defineEmits(['merged'])

const customer = props.type === 'customer'
const tx = {
  title: t(customer ? 'scm.partnerMerge.customerTitle' : 'scm.partnerMerge.supplierTitle'),
  rulePrice: t(customer ? 'scm.partnerMerge.rulePriceCustomer' : 'scm.partnerMerge.rulePriceSupplier'),
  linked: (name) => t(customer ? 'scm.partnerMerge.linkedSupplier' : 'scm.partnerMerge.linkedCustomer', { name })
}
const columns = [
  { title: t('scm.partnerMerge.dataType'), dataIndex: 'label' },
  { title: t('scm.partnerMerge.total'), dataIndex: 'total', width: 110, align: 'right' },
  { title: t('scm.partnerMerge.conflicts'), key: 'conflicts', width: 150, align: 'right' }
]

const visible = ref(false)
const sources = ref([])
const targetId = ref()
const preview = ref(null)
const loading = ref(false)
const saving = ref(false)
// 添加后重置「添加」下拉
const addKey = ref(0)

function open(record) {
  sources.value = record ? [{ id: record.id, code: record.code, name: record.name }] : []
  targetId.value = undefined
  preview.value = null
  visible.value = true
}

function addSource(p) {
  if (p && !sources.value.some((s) => s.id === p.id)) {
    sources.value = [...sources.value, { id: p.id, code: p.code, name: p.name }]
  }
  addKey.value++
}
function removeSource(id) {
  sources.value = sources.value.filter((s) => s.id !== id)
}

const request = computed(() => (targetId.value && sources.value.length ? { targetId: targetId.value, sourceIds: sources.value.map((s) => s.id) } : null))
let seq = 0
watch(request, async (req) => {
  preview.value = null
  if (!req) return
  if (req.sourceIds.includes(req.targetId)) {
    message.warning(t('scm.partnerMerge.sameRecord'))
    return
  }
  const current = ++seq
  loading.value = true
  try {
    const res = await previewPartnerMerge(props.type, req)
    if (current === seq) preview.value = res
  } finally {
    if (current === seq) loading.value = false
  }
})

function submit() {
  const req = request.value
  if (!req || !preview.value) return
  const target = preview.value.target
  Modal.confirm({
    title: t('scm.partnerMerge.confirmTitle'),
    content: t('scm.partnerMerge.confirmContent', { n: req.sourceIds.length, target: `${target.code} ${target.name}` }),
    okType: 'danger',
    async onOk() {
      saving.value = true
      try {
        await mergePartner(props.type, req)
        message.success(t('scm.partnerMerge.done'))
        visible.value = false
        emit('merged')
      } finally {
        saving.value = false
      }
    }
  })
}

defineExpose({ open })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.rules {
  margin: 4px 0 0;
  padding-left: 18px;
}
.sources {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}
.add {
  width: 260px;
}
.sub-title {
  margin-bottom: 8px;
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.link {
  margin-top: 8px;
}
.warn {
  color: #cf1322;
}
</style>
