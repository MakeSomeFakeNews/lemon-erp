<template>
  <div class="section-title">{{ $t('print.template.basic') }}</div>
  <a-form layout="vertical" class="basic">
    <a-row :gutter="12">
      <a-col :span="14">
        <a-form-item :label="$t('print.overlay.docTitle')">
          <a-input v-model:value="config.title" :placeholder="printLabel(type.name)" :maxlength="32" allow-clear />
        </a-form-item>
      </a-col>
      <a-col :span="10">
        <a-form-item :label="$t('print.template.orientation')">
          <a-radio-group v-model:value="config.orientation" button-style="solid" size="small">
            <a-radio-button value="portrait">{{ $t('print.template.portrait') }}</a-radio-button>
            <a-radio-button value="landscape">{{ $t('print.template.landscape') }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col :span="config.paper === 'custom' ? 10 : 24">
        <a-form-item :label="$t('print.template.paper')">
          <a-select v-model:value="config.paper" :options="PAPERS" />
        </a-form-item>
      </a-col>
      <template v-if="config.paper === 'custom'">
        <a-col :span="7">
          <a-form-item :label="$t('print.template.paperWidth')">
            <a-input-number v-model:value="config.paperWidth" :min="50" :max="1000" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="7">
          <a-form-item :label="$t('print.template.paperHeight')">
            <a-input-number v-model:value="config.paperHeight" :min="50" :max="2000" :precision="0" class="full" />
          </a-form-item>
        </a-col>
      </template>
      <a-col :span="12">
        <a-form-item :label="$t('print.template.margin')">
          <a-input-number v-model:value="config.margin" :min="0" :max="40" :precision="0" class="full">
            <template #addonAfter>mm</template>
          </a-input-number>
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item :label="$t('print.template.fontSize')">
          <a-input-number v-model:value="config.fontSize" :min="6" :max="24" :precision="0" class="full">
            <template #addonAfter>pt</template>
          </a-input-number>
        </a-form-item>
      </a-col>
    </a-row>
    <a-space :size="24">
      <a-checkbox v-if="!type.slip" v-model:checked="config.showCompany">{{ $t('print.template.showCompany') }}</a-checkbox>
      <a-checkbox v-if="type.note" v-model:checked="config.showNote">{{ $t('print.template.showNote') }}</a-checkbox>
    </a-space>
  </a-form>
</template>

<script setup>
import { PAPERS, printLabel } from '@/utils/printTemplates'

/** 打印模板的基本设置：标题、纸张、方向、页边距、字号、是否显示公司名称和备注（直接修改页面的编辑配置） */
defineProps({
  /** 编辑中的配置（templateConfig.js 的 buildConfig） */
  config: { type: Object, required: true },
  /** 单据目录（PRINT_TYPES 中的一项） */
  type: { type: Object, required: true }
})
</script>

<style scoped>
.section-title {
  margin: 16px 0 8px;
  font-weight: 600;
  color: var(--app-text);
}
.basic :deep(.ant-form-item) {
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
</style>
