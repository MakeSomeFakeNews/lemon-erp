<template>
  <div class="editor">
    <div class="config scroll-hover">
      <div class="editor-head">
        <span class="editor-title">{{ type.name }}</span>
        <a-tag v-if="saved" color="blue" :bordered="false">已修改</a-tag>
        <span class="spacer" />
        <span v-perm="'sys:printTemplate:edit'">
          <a-popconfirm v-if="saved" title="确定恢复默认样式？" @confirm="reset">
            <a-button><template #icon><UndoOutlined /></template>恢复默认</a-button>
          </a-popconfirm>
        </span>
        <a-button v-perm="'sys:printTemplate:edit'" type="primary" :loading="saving" :disabled="!dirty" @click="save">保存</a-button>
      </div>
      <div class="section-title">标签纸</div>
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="8">
            <a-form-item label="宽">
              <a-input-number v-model:value="cfg.width" :min="20" :max="150" addon-after="mm" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="高">
              <a-input-number v-model:value="cfg.height" :min="10" :max="150" addon-after="mm" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="字号">
              <a-input-number v-model:value="cfg.fontSize" :min="6" :max="20" addon-after="pt" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="16">
            <a-form-item label="条码">
              <a-radio-group v-model:value="cfg.codeType" button-style="solid" size="small">
                <a-radio-button value="CODE128">条形码（Code 128）</a-radio-button>
                <a-radio-button value="QR">二维码</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="条码下显示内容">
              <a-switch v-model:checked="cfg.showText" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <div class="section-title">显示字段 <span class="section-hint">勾选显示，可加前缀（如「编码」），调整顺序；第一行加粗</span></div>
      <div class="rows">
        <div v-for="(row, i) in cfg.fields" :key="row.key" :class="['row', { hidden: row.hidden }]">
          <a-checkbox :checked="!row.hidden" @update:checked="(v) => (row.hidden = !v)" />
          <span class="row-name">{{ row.label }}</span>
          <a-input v-model:value="row.alias" size="small" placeholder="前缀" :maxlength="10" class="row-alias" />
          <span class="row-move">
            <a-button type="text" size="small" :disabled="i === 0" @click="move(i, -1)"><ArrowUpOutlined /></a-button>
            <a-button type="text" size="small" :disabled="i === cfg.fields.length - 1" @click="move(i, 1)"><ArrowDownOutlined /></a-button>
          </span>
        </div>
      </div>
    </div>
    <div class="preview">
      <div class="preview-head">预览（示例数据）</div>
      <iframe class="preview-frame" :srcdoc="previewHtml" title="标签预览" />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { ArrowDownOutlined, ArrowUpOutlined, UndoOutlined } from '@ant-design/icons-vue'
import { resetPrintTemplate, savePrintTemplate } from '@/api/system/printTemplate'
import { LABEL_TYPE_MAP, labelConfig, renderLabelsHtml } from '@/utils/labels'

/** 标签模板：标签纸尺寸、条码类型、显示的字段，右侧按示例数据预览。保存后触发 saved */
const props = defineProps({
  typeKey: { type: String, required: true },
  saved: Object
})
const emit = defineEmits(['saved'])

const type = computed(() => LABEL_TYPE_MAP[props.typeKey])
const cfg = ref(labelConfig(props.typeKey, props.saved))
const snapshot = ref('')
const payload = () => ({
  width: cfg.value.width, height: cfg.value.height, codeType: cfg.value.codeType, showText: cfg.value.showText, fontSize: cfg.value.fontSize,
  fields: cfg.value.fields.map((f) => ({ key: f.key, ...(f.alias?.trim() ? { alias: f.alias.trim() } : {}), ...(f.hidden ? { hidden: true } : {}) }))
})
const dirty = computed(() => JSON.stringify(payload()) !== snapshot.value)
watch(() => [props.typeKey, props.saved], () => {
  cfg.value = labelConfig(props.typeKey, props.saved)
  snapshot.value = JSON.stringify(payload())
}, { immediate: true })

function move(i, delta) {
  const [row] = cfg.value.fields.splice(i, 1)
  cfg.value.fields.splice(i + delta, 0, row)
}

const previewHtml = ref('')
watch(cfg, async () => {
  previewHtml.value = await renderLabelsHtml(props.typeKey, [type.value.sample, type.value.sample], cfg.value)
}, { deep: true, immediate: true })

const saving = ref(false)
async function save() {
  saving.value = true
  try {
    await savePrintTemplate(props.typeKey, payload())
    snapshot.value = JSON.stringify(payload())
    message.success('已保存，之后打印即按新样式')
    emit('saved')
  } finally {
    saving.value = false
  }
}
async function reset() {
  await resetPrintTemplate(props.typeKey)
  message.success('已恢复默认样式')
  emit('saved')
}
</script>

<style scoped>
.editor {
  display: flex;
  gap: 16px;
  height: 100%;
  min-height: 0;
}
.config {
  flex: 1;
  min-width: 0;
  overflow: auto;
}
.preview {
  flex: 0 0 340px;
  display: flex;
  flex-direction: column;
}
.preview-head {
  margin-bottom: 8px;
  color: var(--app-text-3);
}
.preview-frame {
  flex: 1;
  width: 100%;
  min-height: 360px;
  border: 1px solid var(--app-border, #eee);
  border-radius: 6px;
  background: #f0f0f0;
}
.editor-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.editor-title {
  font-size: 16px;
  font-weight: 600;
}
.spacer {
  flex: 1;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
.section-hint {
  font-weight: normal;
  font-size: 12px;
  color: var(--app-text-3);
}
.rows {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.row.hidden .row-name {
  color: var(--app-text-3);
  text-decoration: line-through;
}
.row-name {
  width: 100px;
}
.row-alias {
  width: 120px;
}
.full {
  width: 100%;
}
@media (max-width: 900px) {
  .editor {
    flex-direction: column;
  }
  .preview {
    flex-basis: auto;
  }
}
</style>
