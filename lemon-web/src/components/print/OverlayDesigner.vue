<template>
  <a-modal :open="open" :title="$t('print.overlay.title')" width="min(1280px, 100vw)" :mask-closable="false" :ok-text="$t('common.action.confirm')" @ok="confirm" @cancel="emit('update:open', false)">
    <div class="designer">
      <!-- 左侧：可放置的内容 -->
      <div class="palette scroll-hover">
        <div class="group-title">{{ $t('print.overlay.basic') }}</div>
        <a v-for="k in BASIC" :key="k.key" :class="['chip', { used: used(k.key) }]" @click="add(k.key)">{{ k.label }}</a>
        <div class="group-title">{{ $t('print.overlay.fields') }}</div>
        <a v-for="f in type.fields" :key="f" :class="['chip', { used: used(f) }]" @click="add(f)">{{ printLabel(f) }}</a>
        <template v-if="totalColumns.length">
          <div class="group-title">{{ $t('print.overlay.totals') }}</div>
          <a v-for="c in totalColumns" :key="c" :class="['chip', { used: used(`$total:${c}`) }]" @click="add(`$total:${c}`)">{{ $t('print.overlay.totalOf', { name: printLabel(c) }) }}</a>
        </template>
        <template v-if="type.signs.length">
          <div class="group-title">{{ $t('print.overlay.signs') }}</div>
          <a v-for="s in type.signs" :key="s" :class="['chip', { used: used(`$sign:${s}`) }]" @click="add(`$sign:${s}`)">{{ printLabel(s) }}</a>
        </template>
        <template v-if="type.columns.length">
          <div class="group-title">{{ $t('print.overlay.columns') }}</div>
          <a-checkbox
            v-for="c in type.columns"
            :key="c.title"
            :checked="hasColumn(c.title)"
            class="col-check"
            @update:checked="(v) => toggleColumn(c.title, v)"
          >
            {{ printLabel(c.title) }}
          </a-checkbox>
        </template>
      </div>

      <!-- 中间：纸张 -->
      <div class="canvas-wrap scroll-hover">
        <div class="toolbar">
          <a-upload accept="image/png,image/jpeg" :show-upload-list="false" :before-upload="uploadBackground">
            <a-button size="small" :loading="uploading"><template #icon><PictureOutlined /></template>{{ $t('print.overlay.uploadBg') }}</a-button>
          </a-upload>
          <a-button v-if="background" size="small" @click="removeBackground">{{ $t('print.overlay.removeBg') }}</a-button>
          <span class="hint">{{ $t('print.overlay.bgHint') }}</span>
        </div>
        <div
          ref="pageRef"
          class="page"
          :style="{ width: px(layout.width), height: px(layout.height), backgroundImage: background ? `url(${background})` : 'none' }"
          @pointerdown.self="selected = null"
        >
          <!-- 明细区 -->
          <template v-if="detail.columns.length">
            <div class="detail-area" :style="{ top: px(detail.y), height: px(detail.rowHeight * detail.rows) }" />
            <div
              v-for="(c, i) in detail.columns"
              :key="`col-${c.key}`"
              :class="['item', 'col', { active: selected === `col:${i}` }]"
              :style="{ left: px(c.x), top: px(detail.y), width: px(c.width), height: px(detail.rowHeight) }"
              @pointerdown.stop="startDrag($event, `col:${i}`)"
            >
              {{ printLabel(c.key) }}
            </div>
          </template>
          <div
            v-for="(it, i) in items"
            :key="it.key"
            :class="['item', { active: selected === `item:${i}` }]"
            :style="{ left: px(it.x), top: px(it.y), width: px(it.width), fontSize: `${(it.fontSize || fontSize) * 1.1}px`, textAlign: (it.align || 'LEFT').toLowerCase() }"
            @pointerdown.stop="startDrag($event, `item:${i}`)"
          >
            {{ labelOf(it.key) }}
          </div>
        </div>
      </div>

      <!-- 右侧：属性 -->
      <div class="props scroll-hover">
        <div class="group-title">{{ $t('print.overlay.offset') }}</div>
        <a-row :gutter="8">
          <a-col :span="12"><a-input-number v-model:value="offsetX" :min="-50" :max="50" :step="0.5" addon-before="X" addon-after="mm" size="small" class="full" /></a-col>
          <a-col :span="12"><a-input-number v-model:value="offsetY" :min="-50" :max="50" :step="0.5" addon-before="Y" addon-after="mm" size="small" class="full" /></a-col>
        </a-row>
        <template v-if="current">
          <div class="group-title">{{ current.kind === 'item' ? labelOf(current.obj.key) : $t('print.overlay.columnOf', { name: printLabel(current.obj.key) }) }}</div>
          <a-form layout="vertical" size="small">
            <a-row :gutter="8">
              <a-col :span="12"><a-form-item label="X（mm）"><a-input-number v-model:value="current.obj.x" :min="0" :max="layout.width" :step="0.5" class="full" /></a-form-item></a-col>
              <a-col v-if="current.kind === 'item'" :span="12">
                <a-form-item label="Y（mm）"><a-input-number v-model:value="current.obj.y" :min="0" :max="layout.height" :step="0.5" class="full" /></a-form-item>
              </a-col>
              <a-col :span="12"><a-form-item :label="$t('print.overlay.width')"><a-input-number v-model:value="current.obj.width" :min="5" :max="500" :step="1" class="full" /></a-form-item></a-col>
              <a-col v-if="current.kind === 'item'" :span="12">
                <a-form-item :label="$t('print.overlay.fontSize')"><a-input-number v-model:value="current.obj.fontSize" :min="6" :max="36" :placeholder="String(fontSize)" class="full" /></a-form-item>
              </a-col>
            </a-row>
            <a-form-item :label="$t('print.overlay.align')">
              <a-radio-group v-model:value="current.obj.align" button-style="solid" size="small">
                <a-radio-button value="LEFT">{{ $t('print.overlay.left') }}</a-radio-button>
                <a-radio-button value="CENTER">{{ $t('print.overlay.center') }}</a-radio-button>
                <a-radio-button value="RIGHT">{{ $t('print.overlay.right') }}</a-radio-button>
              </a-radio-group>
            </a-form-item>
            <a-button danger size="small" @click="removeSelected">{{ $t('print.overlay.remove') }}</a-button>
          </a-form>
        </template>
        <template v-if="detail.columns.length">
          <div class="group-title">{{ $t('print.overlay.detail') }}</div>
          <a-form layout="vertical" size="small">
            <a-row :gutter="8">
              <a-col :span="12"><a-form-item :label="$t('print.overlay.detailTop')"><a-input-number v-model:value="detail.y" :min="0" :max="layout.height" :step="0.5" class="full" /></a-form-item></a-col>
              <a-col :span="12"><a-form-item :label="$t('print.overlay.rowHeight')"><a-input-number v-model:value="detail.rowHeight" :min="3" :max="50" :step="0.5" class="full" /></a-form-item></a-col>
              <a-col :span="12"><a-form-item :label="$t('print.overlay.rowsPerPage')"><a-input-number v-model:value="detail.rows" :min="1" :max="100" :precision="0" class="full" /></a-form-item></a-col>
            </a-row>
          </a-form>
        </template>
        <div class="tip">{{ $t('print.overlay.tip') }}</div>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PictureOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'
import { printLabel } from '@/utils/printTemplates'

/**
 * 套打设计：在纸张（可上传扫描的空白单据作底图）上拖放字段，设置明细区的位置、行高和每页行数。
 * 位置单位为 mm，以纸张左上角为原点；结果保存在打印模板配置的 overlay 中，由后端按绝对位置生成 PDF
 */
const props = defineProps({
  open: Boolean,
  /** 打印模板标识 */
  templateKey: String,
  /** 单据目录（PRINT_TYPES 中的一项） */
  type: { type: Object, required: true },
  /** 纸张 { width, height, fontSize } */
  layout: { type: Object, required: true },
  /** 已有的套打设置 */
  value: Object
})
const emit = defineEmits(['update:open', 'confirm'])

const { t } = useI18n()
const SCALE = 3
const BASIC = [
  { key: '$title', label: t('print.overlay.docTitle') },
  { key: '$no', label: t('print.name.docNo') },
  { key: '$company', label: t('print.overlay.company') },
  { key: '$note', label: t('print.name.remark') }
]
const px = (mm) => `${(Number(mm) || 0) * SCALE}px`
const fontSize = computed(() => props.layout.fontSize || 10)
const totalColumns = computed(() => props.type.columns.filter((c) => c.align === 'right').map((c) => c.title))

const items = ref([])
const detail = reactive({ y: 80, rowHeight: 8, rows: 10, columns: [] })
const offsetX = ref(0)
const offsetY = ref(0)
const selected = ref(null)
const background = ref('')
const uploading = ref(false)
const pageRef = ref()

function labelOf(key) {
  const basic = BASIC.find((b) => b.key === key)
  if (basic) return basic.label
  if (key.startsWith('$total:')) return t('print.overlay.totalOf', { name: printLabel(key.slice(7)) })
  if (key.startsWith('$sign:')) return printLabel(key.slice(6))
  return printLabel(key)
}
const used = (key) => items.value.some((it) => it.key === key)
const hasColumn = (key) => detail.columns.some((c) => c.key === key)

function add(key) {
  if (used(key)) {
    selected.value = `item:${items.value.findIndex((it) => it.key === key)}`
    return
  }
  items.value.push({ key, x: 10, y: 10 + (items.value.length % 20) * 8, width: 50, fontSize: null, align: 'LEFT' })
  selected.value = `item:${items.value.length - 1}`
}

function toggleColumn(key, on) {
  if (on && !hasColumn(key)) {
    const last = detail.columns[detail.columns.length - 1]
    detail.columns.push({ key, x: last ? Number(last.x) + Number(last.width) : 10, width: 25, align: 'LEFT' })
  } else if (!on) {
    detail.columns.splice(
      detail.columns.findIndex((c) => c.key === key),
      1
    )
    selected.value = null
  }
}

const current = computed(() => {
  if (!selected.value) return null
  const [kind, index] = selected.value.split(':')
  const obj = kind === 'item' ? items.value[Number(index)] : detail.columns[Number(index)]
  return obj ? { kind, obj } : null
})

function removeSelected() {
  const c = current.value
  if (!c) return
  if (c.kind === 'item') items.value.splice(items.value.indexOf(c.obj), 1)
  else detail.columns.splice(detail.columns.indexOf(c.obj), 1)
  selected.value = null
}

// ---------------------------------------------------------------- 拖动
let drag = null
function startDrag(e, key) {
  selected.value = key
  const c = current.value
  drag = { obj: c.obj, kind: c.kind, startX: e.clientX, startY: e.clientY, x: Number(c.obj.x) || 0, y: Number(c.kind === 'item' ? c.obj.y : detail.y) || 0 }
  window.addEventListener('pointermove', onMove)
  window.addEventListener('pointerup', endDrag)
}
const round = (v) => Math.round(v * 2) / 2
function onMove(e) {
  if (!drag) return
  const dx = (e.clientX - drag.startX) / SCALE
  const dy = (e.clientY - drag.startY) / SCALE
  drag.obj.x = Math.max(0, Math.min(props.layout.width - 1, round(drag.x + dx)))
  // 明细列上下拖动时移动整个明细区
  if (drag.kind === 'item') drag.obj.y = Math.max(0, Math.min(props.layout.height - 1, round(drag.y + dy)))
  else detail.y = Math.max(0, Math.min(props.layout.height - 1, round(drag.y + dy)))
}
function endDrag() {
  drag = null
  window.removeEventListener('pointermove', onMove)
  window.removeEventListener('pointerup', endDrag)
}
onBeforeUnmount(endDrag)

// ---------------------------------------------------------------- 底图
async function loadBackground() {
  background.value = ''
  if (!props.templateKey) return
  background.value = (await request.get(`/system/print-template/${props.templateKey}/background`).catch(() => '')) || ''
}
function uploadBackground(file) {
  if (file.size > 2 * 1024 * 1024) {
    message.warning(t('print.overlay.bgTooLarge'))
    return false
  }
  const reader = new FileReader()
  reader.onload = async () => {
    uploading.value = true
    try {
      await request.put(`/system/print-template/${props.templateKey}/background`, { dataUrl: reader.result })
      background.value = reader.result
      message.success(t('print.overlay.bgUploaded'))
    } finally {
      uploading.value = false
    }
  }
  reader.readAsDataURL(file)
  return false
}
async function removeBackground() {
  await request.put(`/system/print-template/${props.templateKey}/background`, { dataUrl: null })
  background.value = ''
}

watch(
  () => props.open,
  (v) => {
    if (!v) return
    const o = props.value || {}
    items.value = (o.items || []).map((it) => ({ ...it }))
    Object.assign(detail, { y: 80, rowHeight: 8, rows: 10, ...(o.detail || {}), columns: (o.detail?.columns || []).map((c) => ({ ...c })) })
    offsetX.value = o.offsetX || 0
    offsetY.value = o.offsetY || 0
    selected.value = null
    loadBackground()
  }
)

function confirm() {
  emit('confirm', {
    enabled: true,
    offsetX: offsetX.value || 0,
    offsetY: offsetY.value || 0,
    items: items.value.map((it) => ({ ...it, fontSize: it.fontSize || null })),
    detail: detail.columns.length ? { y: detail.y, rowHeight: detail.rowHeight, rows: detail.rows, columns: detail.columns.map((c) => ({ ...c })) } : null
  })
  emit('update:open', false)
}
</script>

<style scoped>
.designer {
  display: flex;
  gap: 12px;
  height: min(72vh, 760px);
}
.palette {
  width: 200px;
  flex: none;
  overflow-y: auto;
}
.props {
  width: 240px;
  flex: none;
  overflow-y: auto;
}
.group-title {
  font-weight: 500;
  margin: 12px 0 6px;
}
.chip {
  display: inline-block;
  margin: 0 6px 6px 0;
  padding: 1px 8px;
  border: 1px solid var(--app-border);
  border-radius: 4px;
  font-size: 12px;
  color: var(--app-text-2);
}
.chip.used {
  border-color: var(--app-primary);
  color: var(--app-primary);
}
.col-check {
  display: flex;
  margin: 0 0 4px;
}
.canvas-wrap {
  flex: 1;
  min-width: 0;
  overflow: auto;
  background: var(--app-bg-2, #f5f5f5);
  padding: 8px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.hint {
  color: var(--app-text-3);
  font-size: 12px;
}
.page {
  position: relative;
  background-color: #fff;
  background-size: 100% 100%;
  background-repeat: no-repeat;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
  user-select: none;
}
.item {
  position: absolute;
  padding: 0 2px;
  border: 1px dashed #1677ff;
  background: rgba(22, 119, 255, 0.08);
  color: #1d39c4;
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  cursor: move;
}
.item.col {
  border-color: #52c41a;
  background: rgba(82, 196, 26, 0.1);
  color: #237804;
}
.item.active {
  border-style: solid;
  box-shadow: 0 0 0 1px currentColor;
}
.detail-area {
  position: absolute;
  left: 0;
  right: 0;
  border-top: 1px dashed #52c41a;
  border-bottom: 1px dashed #52c41a;
  background: rgba(82, 196, 26, 0.04);
  pointer-events: none;
}
.tip {
  margin-top: 12px;
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
