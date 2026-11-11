<template>
  <template v-if="!type.slip">
    <div class="section-title">
      {{ $t('print.template.overlay') }}
      <span class="section-hint">{{ $t('print.template.overlayHint') }}</span>
    </div>
    <div class="overlay-box">
      <a-space wrap>
        <a-switch
          :checked="!!config.overlay?.enabled"
          :checked-children="$t('system.im.enabled')"
          :un-checked-children="$t('system.im.notEnabled')"
          :disabled="custom"
          @change="toggle"
        />
        <a-button size="small" :disabled="custom" @click="open = true">
          <template #icon><DragOutlined /></template>{{ $t('print.template.designOverlay') }}
        </a-button>
        <span v-if="config.overlay" class="section-hint">
          {{ config.overlay.detail
            ? $t('print.template.overlayStatsDetail', { n: config.overlay.items?.length || 0, cols: config.overlay.detail.columns.length })
            : $t('print.template.overlayStats', { n: config.overlay.items?.length || 0 }) }}
        </span>
      </a-space>
      <div class="overlay-hint">{{ $t('print.template.overlayTip') }}</div>
    </div>
  </template>
  <OverlayDesigner
    v-model:open="open"
    :template-key="templateKey"
    :format-id="formatId"
    :type="type"
    :layout="layout"
    :value="config.overlay"
    @confirm="(o) => (config.overlay = o)"
  />
</template>

<script setup>
import { computed, ref } from 'vue'
import { DragOutlined } from '@ant-design/icons-vue'
import OverlayDesigner from '@/components/print/OverlayDesigner.vue'
import { layoutOf } from '@/utils/printTemplates'
import { toPayload } from './templateConfig'

/** 打印模板的套打设置：启用 / 停用、打开套打设计（结果写回编辑配置的 overlay，随页面保存） */
const props = defineProps({
  /** 编辑中的配置（templateConfig.js 的 buildConfig） */
  config: { type: Object, required: true },
  /** 单据目录（PRINT_TYPES 中的一项） */
  type: { type: Object, required: true },
  templateKey: { type: String, required: true },
  /** 当前编辑的格式，为空时为默认格式（套打底图按格式保存） */
  formatId: Number,
  /** 已上传 JRXML：以 JRXML 为准，套打不可用 */
  custom: Boolean
})

const open = ref(false)
const layout = computed(() => layoutOf(toPayload(props.config)))

/** 启用时还没有定位任何内容则先打开设计 */
function toggle(on) {
  const cfg = props.config
  if (on && !cfg.overlay?.items?.length && !cfg.overlay?.detail) {
    open.value = true
    return
  }
  cfg.overlay = { ...(cfg.overlay || { items: [], detail: null, offsetX: 0, offsetY: 0 }), enabled: on }
}
</script>

<style scoped>
.section-title {
  margin: 16px 0 8px;
  font-weight: 600;
  color: var(--app-text);
}
.section-hint {
  margin-left: 8px;
  font-weight: 400;
  font-size: 12px;
  color: var(--app-text-3);
}
.overlay-box {
  border: 1px solid var(--app-border);
  border-radius: 8px;
  padding: 10px 12px;
}
.overlay-hint {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--app-text-3);
}
</style>
