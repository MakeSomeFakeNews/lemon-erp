<template>
  <a-drawer
    :open="open"
    :title="$t('layout.settings.title')"
    placement="right"
    :width="340"
    class="setting-drawer"
    @close="emit('update:open', false)"
  >
    <section class="section">
      <div class="section-title">{{ $t('layout.settings.themeMode') }}</div>
      <a-segmented :value="settings.themeMode" :options="modeOptions" block @change="(v) => set('themeMode', v)" />
    </section>

    <section class="section">
      <div class="section-title">{{ $t('layout.settings.primaryColor') }}</div>
      <div class="swatches">
        <a-tooltip v-for="c in PRESET_COLORS" :key="c.key" :title="$t(`layout.settings.colors.${c.key}`)">
          <button
            type="button"
            class="swatch"
            :style="{ background: c.color }"
            :aria-label="$t(`layout.settings.colors.${c.key}`)"
            @click="set('primaryColor', c.color)"
          >
            <CheckOutlined v-if="settings.primaryColor === c.color" />
          </button>
        </a-tooltip>
        <!-- 自定义颜色：原生取色器，拖动时实时预览 -->
        <a-tooltip :title="$t('layout.settings.customColor')">
          <label :class="['swatch', 'swatch-custom', { active: isCustom }]" :style="isCustom ? { background: settings.primaryColor } : null">
            <CheckOutlined v-if="isCustom" />
            <BgColorsOutlined v-else />
            <input type="color" :value="settings.primaryColor" :aria-label="$t('layout.settings.customColor')" @input="(e) => set('primaryColor', e.target.value)" />
          </label>
        </a-tooltip>
      </div>
      <div class="color-value">{{ colorName }}<span class="hex">{{ settings.primaryColor }}</span></div>
    </section>

    <section class="section">
      <div class="section-title">{{ $t('layout.settings.layout') }}</div>
      <div class="layouts">
        <button
          v-for="l in LAYOUTS"
          :key="l"
          type="button"
          :class="['layout-option', { active: settings.layout === l }]"
          @click="set('layout', l)"
        >
          <LayoutThumb :layout="l" />
          <span class="layout-name">{{ $t(`layout.settings.layouts.${l}`) }}</span>
        </button>
      </div>
      <div class="tip mobile-only">{{ $t('layout.settings.mobileLayoutTip') }}</div>
    </section>

    <section class="section">
      <div class="section-title">{{ $t('layout.settings.display') }}</div>
      <div class="row">
        <span>{{ $t('layout.settings.tabs') }}</span>
        <a-switch :checked="settings.tabs" size="small" @change="(v) => set('tabs', v)" />
      </div>
      <div class="row">
        <span>
          {{ $t('layout.settings.compact') }}
          <a-tooltip :title="$t('layout.settings.compactTip')"><QuestionCircleOutlined class="help" /></a-tooltip>
        </span>
        <a-switch :checked="settings.compact" size="small" @change="(v) => set('compact', v)" />
      </div>
      <div class="row">
        <span>{{ $t('layout.settings.language') }}</span>
        <a-select :value="currentLocale()" :options="localeOptions" size="small" class="lang-select" @change="(v) => appStore.changeLocale(v)" />
      </div>
    </section>

    <div class="tip">{{ configStore.demoMode ? $t('layout.settings.localTip') : $t('layout.settings.savedTip') }}</div>

    <template #footer>
      <a-button block @click="reset">
        <template #icon><UndoOutlined /></template>
        {{ $t('layout.settings.reset') }}
      </a-button>
    </template>
  </a-drawer>
</template>

<script setup>
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { BgColorsOutlined, CheckOutlined, QuestionCircleOutlined, UndoOutlined } from '@ant-design/icons-vue'
import { LOCALES, currentLocale } from '@/locales'
import { useAppStore } from '@/store/app'
import { useConfigStore } from '@/store/config'
import { LAYOUTS, PRESET_COLORS, ThemeMode } from '@/utils/uiSettings'
import LayoutThumb from './LayoutThumb.vue'

/** 界面设置抽屉：修改立即生效（实时预览），由 store/app.js 写入本地缓存并防抖保存到服务端 */
defineProps({ open: Boolean })
const emit = defineEmits(['update:open'])

const { t } = useI18n()
const appStore = useAppStore()
const configStore = useConfigStore()
const settings = computed(() => appStore.settings)
const set = (key, value) => appStore.setSetting(key, value)

const modeOptions = computed(() => [
  { value: ThemeMode.LIGHT, label: t('layout.settings.light') },
  { value: ThemeMode.DARK, label: t('layout.settings.dark') },
  { value: ThemeMode.SYSTEM, label: t('layout.settings.system') }
])
const localeOptions = LOCALES.map((l) => ({ value: l.value, label: l.label }))

const preset = computed(() => PRESET_COLORS.find((c) => c.color === settings.value.primaryColor))
const isCustom = computed(() => !preset.value)
const colorName = computed(() => (preset.value ? t(`layout.settings.colors.${preset.value.key}`) : t('layout.settings.customColor')))

function reset() {
  appStore.resetSettings()
  message.success(t('layout.settings.resetDone'))
}
</script>

<style scoped>
.section + .section {
  margin-top: 24px;
}
.section-title {
  margin-bottom: 12px;
  font-weight: 500;
  color: var(--app-text);
}
.swatches {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.swatch {
  position: relative;
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  border-radius: 6px;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: transform 0.15s;
}
.swatch:hover {
  transform: scale(1.12);
}
.swatch-custom {
  border: 1px dashed var(--app-text-3);
  background: transparent;
  color: var(--app-text-2);
  overflow: hidden;
}
.swatch-custom.active {
  border-style: solid;
  border-color: transparent;
  color: #fff;
}
/* 取色器铺满色块，点击色块即打开 */
.swatch-custom input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}
.color-value {
  margin-top: 10px;
  font-size: 12px;
  color: var(--app-text-2);
}
.hex {
  margin-left: 8px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  color: var(--app-text-3);
  text-transform: uppercase;
}
.layouts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.layout-option {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: transparent;
  color: var(--app-text-2);
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.layout-option:hover {
  border-color: var(--app-primary-border);
}
.layout-option.active {
  border-color: var(--app-primary);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--app-primary) 18%, transparent);
  color: var(--app-primary);
  font-weight: 500;
}
.layout-name {
  line-height: 1.4;
  text-align: center;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 36px;
}
.help {
  margin-left: 4px;
  color: var(--app-text-3);
}
.lang-select {
  width: 120px;
}
.tip {
  margin-top: 20px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--app-text-3);
}
.tip.mobile-only {
  display: none;
  margin-top: 8px;
}
@media (max-width: 767px) {
  .tip.mobile-only {
    display: block;
  }
}
</style>
