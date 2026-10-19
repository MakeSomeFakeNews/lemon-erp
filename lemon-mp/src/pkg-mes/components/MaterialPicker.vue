<template>
  <view>
    <view class="mp" @click="openSearch">
      <view v-if="modelValue" class="mp__value">
        <text class="mp__name">{{ modelValue.name }}</text>
        <text class="mp__sub">{{ [modelValue.code, modelValue.spec].filter(Boolean).join(' · ') }}</text>
      </view>
      <text v-else class="l-placeholder">{{ t('mes.common.selectMaterial') }}</text>
      <text v-if="!disabled" class="mp__link">{{ modelValue ? t('mes.common.change') : t('common.pleaseSelect') }}</text>
    </view>

    <!-- 搜索弹层：扫码或输入编码、名称 -->
    <view v-if="open" class="mp-mask" @click.self="open = false">
      <view class="mp-panel">
        <l-scan-input :placeholder="t('mes.common.searchMaterial')" focus @scan="onScan" />
        <scroll-view scroll-y class="mp-list">
          <view v-for="m in list" :key="m.id" class="mp-row" @click="select(m)">
            <text class="mp__name">{{ m.name }}</text>
            <text class="mp__sub">{{ [m.code, m.spec, m.unit].filter(Boolean).join(' · ') }}</text>
          </view>
          <l-empty v-if="searched && !list.length" />
        </scroll-view>
        <view class="mp-cancel" @click="open = false">{{ t('common.cancel') }}</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from '@/locales'
import { materialOptions } from '@/api/mes'
import { materialByBarcode } from '@/api/wms'
import { vibrate } from '@/composables/useAction'

/**
 * 选择物料：点击后弹出搜索，扫码时先按物料条码 / 包装条码识别，再按编码精确匹配，其余按编码、名称模糊搜索。
 * v-model 为物料 { id, code, name, spec, unit }
 */
const props = defineProps({ modelValue: Object, disabled: Boolean })
const emit = defineEmits(['update:modelValue', 'change'])
const { t } = useI18n()

const open = ref(false)
const list = ref([])
const searched = ref(false)

function openSearch() {
  if (props.disabled) return
  open.value = true
  if (!searched.value) search('')
}

async function search(keyword) {
  list.value = (await materialOptions(keyword).catch(() => [])) || []
  searched.value = true
}

async function onScan(code) {
  const hit = await materialByBarcode(code).catch(() => null)
  if (hit?.material) return select(hit.material)
  await search(code)
  const exact = list.value.find((m) => m.code === code)
  if (exact) return select(exact)
  if (!list.value.length) {
    vibrate(false)
    uni.showToast({ title: t('mes.common.materialNotFound', { code }), icon: 'none' })
  }
}

function select(m) {
  vibrate(true)
  const value = { id: m.id, code: m.code, name: m.name, spec: m.spec, unit: m.unit }
  emit('update:modelValue', value)
  emit('change', value)
  open.value = false
}
</script>

<style lang="scss">
.mp {
  display: flex;
  align-items: center;
  min-height: 72rpx;
  &__value {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
  &__name {
    font-size: 28rpx;
    color: $l-text;
  }
  &__sub {
    margin-top: 4rpx;
    font-size: 24rpx;
    color: $l-text-3;
  }
  &__link {
    margin-left: auto;
    padding-left: 16rpx;
    font-size: 26rpx;
    color: $l-primary;
  }
}

.mp-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 100;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: flex-end;
}

.mp-panel {
  width: 100%;
  height: 80vh;
  display: flex;
  flex-direction: column;
  background: $l-bg;
  border-radius: 24rpx 24rpx 0 0;
  overflow: hidden;
}

.mp-list {
  flex: 1;
  height: 0;
}

.mp-row {
  display: flex;
  flex-direction: column;
  margin: 0 24rpx 16rpx;
  padding: 20rpx 24rpx;
  background: #fff;
  border-radius: $l-radius;
}

.mp-cancel {
  padding: 28rpx 0 calc(28rpx + env(safe-area-inset-bottom));
  text-align: center;
  font-size: 30rpx;
  background: #fff;
}
</style>
