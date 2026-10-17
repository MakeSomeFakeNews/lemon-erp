<template>
  <SubPage
    :open="detailOpen"
    :title="detail ? $t('mes.wo.detailTitle', { no: detail.woNo }) : $t('mes.wo.wo')"
    width="min(1100px, 100vw)"
    destroy-on-close
    @close="detailOpen = false"
  >
    <template #extra>
      <a-space>
        <a-button v-if="detail" size="small" @click="print"><template #icon><PrinterOutlined /></template>{{ $t('common.action.print') }}</a-button>
        <DictTag v-if="detail?.woType === WorkOrderType.REWORK" type="mes_wo_type" :value="detail.woType" />
        <DictTag v-if="detail" type="mes_wo_status" :value="detail.status" />
      </a-space>
    </template>
    <a-spin :spinning="detailLoading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item :label="$t('mes.wo.product')">{{ detail.materialCode }} {{ detail.materialName }}</a-descriptions-item>
          <a-descriptions-item label="BOM">{{ detail.bomVersion }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.wo.finishedOfPlan')">{{ formatQty(detail.finishedQty) }} / {{ formatQty(detail.qty) }} {{ detail.unit }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.wo.planDate')">{{ detail.planStart || '-' }} ~ {{ detail.planEnd || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.wo.finishWarehouse')">{{ detail.warehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.sourceWoId" :label="detail.woType === WorkOrderType.REWORK ? $t('mes.wo.reworkFrom') : $t('mes.wo.splitFrom')">
            <a @click="openDetail(detail.sourceWoId)">{{ sourceWoNo || $t('mes.wo.woById', { id: detail.sourceWoId }) }}</a>
          </a-descriptions-item>
          <a-descriptions-item v-if="scrapReported > 0" :label="$t('mes.wo.scrapReportedIn')">
            {{ formatQty(scrapReported) }} / {{ formatQty(scrapIn) }} {{ detail.unit }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('mes.wo.preparedBy')">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.releaseTime" :label="$t('mes.wo.releaseTime')">{{ detail.releaseTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.finishTime" :label="$t('mes.wo.finishTime')">{{ detail.finishTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <a-tabs size="small">
          <a-tab-pane key="materials" :tab="$t('mes.wo.tab.materials')">
            <WoMaterialsTab :materials="detail.materials" />
          </a-tab-pane>
          <a-tab-pane key="ops" :tab="$t('mes.wo.tab.ops')">
            <WoOpsTab :detail="detail" @report="(op) => reportRef.open(op)" />
          </a-tab-pane>
          <a-tab-pane key="docs" :tab="$t('mes.wo.tab.docs')">
            <WoDocsTab :docs="docs" />
          </a-tab-pane>
          <a-tab-pane key="reports" :tab="$t('mes.wo.tab.reports')">
            <WoReportsTab :reports="reports" />
          </a-tab-pane>
          <a-tab-pane key="attachment" :tab="$t('mes.wo.tab.attachment')">
            <AttachmentPanel biz-type="mes_wo" :biz-id="detail.id" :editable="userStore.hasPerm('mes:wo:edit')" />
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-spin>
    <template v-if="detail && actions.length" #footer>
      <div class="drawer-footer">
        <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">
          {{ a.label }}
        </a-button>
      </div>
    </template>
  </SubPage>
  <WoDocForm v-model:open="docOpen" :kind="docKind" :wo="detail" :scrap-in="scrapIn" @success="refreshDetail" />
  <WoQtyModal ref="qtyRef" :detail="detail" @success="refreshDetail" />
  <WoSplitModal ref="splitRef" :detail="detail" @success="onSplit" />
  <SupplementForm v-model:open="supplementOpen" :wo="detail" />
  <WoReportModal ref="reportRef" :detail="detail" @success="refreshDetail" />
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref } from 'vue'
import { PrinterOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import SupplementForm from '@/components/mes/SupplementForm.vue'
import WoDocForm from '@/components/mes/WoDocForm.vue'
import { scheduleWo } from '@/api/mes/capacity'
import { closeWo, getWo, pageReport, pageWoDoc } from '@/api/mes/wo'
import { useAction } from '@/composables/useAction'
import { useUserStore } from '@/store/user'
import { formatQty } from '@/components/wms/wms'
import { materialColumns as printMaterialColumns, printDocs } from '@/utils/print'
import { printLabel } from '@/utils/printTemplates'
import WoDocsTab from './WoDocsTab.vue'
import WoMaterialsTab from './WoMaterialsTab.vue'
import WoOpsTab from './WoOpsTab.vue'
import WoQtyModal from './WoQtyModal.vue'
import WoReportModal from './WoReportModal.vue'
import WoReportsTab from './WoReportsTab.vue'
import WoSplitModal from './WoSplitModal.vue'
import { offerOpen } from './wo'
import { WoDocKind, WorkOrderStatus, WorkOrderType } from '@/constants/enums'

/**
 * 工单详情：用料、工序、单据、报工，底部为按状态和权限显示的操作。
 * 通过 ref 调用 open(id) / refreshIfOpen()；数据变化后触发 changed（刷新列表），
 * 「下达」「新建返工工单」由列表页处理（触发 release(工单) / rework(工单)）
 */
const emit = defineEmits(['changed', 'release', 'rework'])
const userStore = useUserStore()
const { t } = useI18n()

const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref()

/** 生产任务单：用料（含补料）和工序。名称写中文（打印模板的匹配键），显示时按界面语言翻译（utils/print.js） */
function print() {
  const d = detail.value
  printDocs({
    title: '生产任务单', // i18n-ignore
    no: d.woNo,
    fields: [
      ['产品', `${d.materialCode} ${d.materialName}`], // i18n-ignore
      ['BOM 版本', d.bomVersion], // i18n-ignore
      ['计划数量', `${formatQty(d.qty)} ${d.unit || ''}`], // i18n-ignore
      ['计划开工', d.planStart], // i18n-ignore
      ['计划完工', d.planEnd], // i18n-ignore
      ['完工入库仓库', d.warehouseName] // i18n-ignore
    ],
    columns: [
      ...printMaterialColumns(),
      { title: '单位用量', value: (r) => formatQty(r.unitQty), align: 'right' }, // i18n-ignore
      { title: '需求数量', value: (r) => formatQty(r.requiredQty), align: 'right' }, // i18n-ignore
      { title: '补料', value: (r) => (Number(r.supplementQty) ? formatQty(r.supplementQty) : ''), align: 'right' }, // i18n-ignore
      { title: '已领', value: (r) => formatQty(r.issuedQty), align: 'right' } // i18n-ignore
    ],
    rows: d.materials,
    sections: d.ops?.length
      ? [{
          title: '工序', // i18n-ignore
          columns: [
            { title: '工序号', key: 'seq' }, // i18n-ignore
            { title: '工序', key: 'name' }, // i18n-ignore
            { title: '工作中心', key: 'workCenterName' }, // i18n-ignore
            { title: '合格数', value: () => '' }, // i18n-ignore
            { title: '报废数', value: () => '' }, // i18n-ignore
            { title: '操作员', value: () => '' } // i18n-ignore
          ],
          rows: d.ops
        }]
      : [],
    note: d.remark,
    signs: [['制单', d.createByName], '生产主管', '仓库'] // i18n-ignore
  }, `${printLabel('生产任务单')} ${d.woNo}`, 'mes.workOrder') // i18n-ignore
}
const docs = ref([])
const reports = ref([])
const sourceWoNo = ref()
let detailId

/** 各工序报工报废数合计 */
const scrapReported = computed(() => (detail.value?.ops || []).reduce((sum, o) => sum + Number(o.scrapQty || 0), 0))
/** 已报废入库数量 */
const scrapIn = computed(() => docs.value.filter((d) => d.kind === WoDocKind.SCRAP).reduce((sum, d) => sum + Number(d.totalQty || 0), 0))

async function refreshDetail() {
  detailLoading.value = true
  try {
    const [wo, docPage, reportPage] = await Promise.all([
      getWo(detailId),
      pageWoDoc({ woId: detailId, current: 1, size: 200 }),
      pageReport({ woId: detailId, current: 1, size: 200 })
    ])
    detail.value = wo
    docs.value = docPage.records
    reports.value = reportPage.records
    sourceWoNo.value = wo.sourceWoId ? (await getWo(wo.sourceWoId).catch(() => null))?.woNo : undefined
  } finally {
    detailLoading.value = false
  }
  emit('changed')
}

function openDetail(id) {
  detailId = id
  detail.value = undefined
  detailOpen.value = true
  refreshDetail()
}

function refreshIfOpen() {
  if (detailOpen.value) refreshDetail()
}

const supplementOpen = ref(false)
const docOpen = ref(false)
const docKind = ref('issue')
function openDoc(kind) {
  docKind.value = kind
  docOpen.value = true
}

const qtyRef = ref()
const splitRef = ref()
const reportRef = ref()
const openQty = () => qtyRef.value.open()
const openSplit = () => splitRef.value.open()

const actions = computed(() => {
  const d = detail.value
  if (!d) return []
  const list = []
  const can = (p) => userStore.hasPerm(`mes:wo:${p}`)
  if ([WorkOrderStatus.DRAFT, WorkOrderStatus.RELEASED, WorkOrderStatus.STARTED].includes(d.status)) {
    if (userStore.hasPerm('mes:capacity:edit') || can('edit')) list.push({ key: 'schedule', label: t('mes.wo.schedule'), run: () => schedule(d) })
    if (can('change')) list.push({ key: 'split', label: t('mes.wo.split'), run: openSplit })
  }
  if ([WorkOrderStatus.STARTED, WorkOrderStatus.COMPLETED].includes(d.status) && can('add')) list.push({ key: 'rework', label: t('mes.wo.addRework'), run: () => emit('rework', d) })
  if (d.status === WorkOrderStatus.DRAFT && can('release')) list.push({ key: 'release', label: t('mes.wo.release'), primary: true, run: () => emit('release', d) })
  if ([WorkOrderStatus.RELEASED, WorkOrderStatus.STARTED].includes(d.status)) {
    if (can('change')) list.push({ key: 'qty', label: t('mes.wo.changeQty'), run: openQty })
    if (can('release'))
      list.push({
        key: 'close',
        label: t('common.action.close'),
        danger: true,
        run: () => close(d)
      })
    if (can('issue')) {
      list.push({ key: 'return', label: t('mes.woDocForm.kind.return'), run: () => openDoc('return') })
      list.push({ key: 'issue', label: t('mes.woDocForm.kind.issue'), run: () => openDoc('issue') })
    }
    if (userStore.hasPerm('mes:supplement:add')) list.push({ key: 'supplement', label: t('mes.supplementForm.title'), run: () => (supplementOpen.value = true) })
    if (can('finish')) {
      list.push({ key: 'scrap', label: t('mes.woDocForm.kind.scrap'), run: () => openDoc('scrap') })
      list.push({ key: 'finish', label: t('mes.woDocForm.kind.finish'), primary: true, run: () => openDoc('finish') })
    }
  }
  return list
})

// ---------------------------------------------------------------- 排程、关闭、拆分
const schedule = useAction((d) => scheduleWo(d.id), {
  confirm: (d) => ({ title: t('mes.wo.scheduleConfirm', { no: d.woNo }), content: t('mes.wo.scheduleContent') }),
  successKey: 'mes.wo.scheduled',
  reload: () => refreshDetail()
})

const close = useAction((d) => closeWo(d.id), {
  confirm: { title: t('mes.wo.closeConfirm'), content: t('mes.wo.closeContent') },
  successKey: 'mes.wo.closed',
  reload: () => refreshDetail()
})

function onSplit(id) {
  refreshDetail()
  offerOpen(t('mes.wo.splitSuccess'), t('mes.wo.splitSuccessContent'), () => openDetail(id))
}

defineExpose({ open: openDetail, refreshIfOpen })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
</style>
