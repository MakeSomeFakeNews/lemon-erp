<template>
  <view>
    <!-- 选择设备：扫码或搜索 -->
    <template v-if="picking">
      <l-scan-input :placeholder="t('mes.maintenance.searchEquipment')" focus @scan="onScan" />
      <view v-for="e in filtered" :key="e.id" class="l-item" @click="pick(e)">
        <view class="l-item__head">
          <text class="l-item__title">{{ e.name }}</text>
          <l-tag :text="dictText(dict.mes_equipment_status, e.status)" :color="dictColor(dict.mes_equipment_status, e.status)" />
        </view>
        <view class="l-item__desc">{{ e.code }}</view>
      </view>
      <l-empty v-if="!filtered.length" />
    </template>

    <template v-else>
      <l-card>
        <l-field :label="t('mes.maintenance.equipment')" required>
          <view class="ma__row" @click="startPick">
            <text v-if="equipment">{{ equipment.code }} {{ equipment.name }}</text>
            <text v-else class="l-placeholder">{{ t('mes.maintenance.selectEquipment') }}</text>
            <text class="ma__link">{{ equipment ? t('mes.common.change') : t('common.pleaseSelect') }}</text>
          </view>
        </l-field>
        <l-field :label="t('mes.maintenance.type')" required>
          <picker :range="dict.mes_maintain_type" range-key="label" @change="(e) => (form.type = dict.mes_maintain_type[e.detail.value].value)">
            <view class="l-input">{{ dictText(dict.mes_maintain_type, form.type) || t('common.pleaseSelect') }}</view>
          </picker>
        </l-field>
        <l-field :label="t('mes.maintenance.content')" required>
          <textarea v-model="form.content" class="l-textarea" maxlength="255" :placeholder="t('mes.maintenance.contentPh')" placeholder-class="l-placeholder" />
        </l-field>
        <l-field :label="t('mes.maintenance.planDate')">
          <picker mode="date" :value="form.planDate" @change="(e) => (form.planDate = e.detail.value)">
            <view class="l-input">{{ form.planDate }}</view>
          </picker>
        </l-field>
        <l-field :label="t('mes.maintenance.handler')">
          <input v-model="form.handler" class="l-input" maxlength="64" :placeholder="t('mes.common.optional')" placeholder-class="l-placeholder" />
        </l-field>
      </l-card>
      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn" :loading="busy" :disabled="busy" @click="submit">{{ t('common.submit') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { EquipmentStatus, MaintenanceType } from '@web/constants/enums'
import { addMaintenance, equipmentOptions } from '@/api/mes'
import { vibrate } from '@/composables/useAction'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { today } from '@/utils/format'

/** 报修 / 登记保养：从设备详情进入时带出设备，否则扫码或搜索选择（设备选项不含已报废的） */
const { t } = useI18n()
useTitle('mes.maintenance.addTitle')
const dict = useDict('mes_maintain_type', 'mes_equipment_status')
const options = ref([])
const equipment = ref(null)
const picking = ref(false)
const keyword = ref('')
const busy = ref(false)
const form = reactive({ type: MaintenanceType.REPAIR, content: '', planDate: today(), handler: '' })

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return options.value.filter((e) => !k || e.code.toLowerCase().includes(k) || e.name.toLowerCase().includes(k)).slice(0, 100)
})

onLoad(async (q) => {
  if (q.type && Object.values(MaintenanceType).includes(q.type)) form.type = q.type
  options.value = (await equipmentOptions().catch(() => [])) || []
  if (q.equipmentId) equipment.value = options.value.find((e) => String(e.id) === String(q.equipmentId)) || null
})

function startPick() {
  keyword.value = ''
  picking.value = true
}

function onScan(code) {
  const exact = options.value.find((e) => e.code === code)
  if (exact) return pick(exact)
  keyword.value = code
  if (!filtered.value.length) vibrate(false)
}

function pick(e) {
  vibrate(true)
  equipment.value = e
  picking.value = false
}

async function submit() {
  if (!equipment.value) return toast(t('common.selectRequired', { name: t('mes.maintenance.equipment') }))
  if (equipment.value.status === EquipmentStatus.SCRAPPED) return toast(t('mes.maintenance.scrapped'))
  if (!form.content.trim()) return toast(t('common.required', { name: t('mes.maintenance.content') }))
  busy.value = true
  try {
    await addMaintenance({
      equipmentId: equipment.value.id,
      type: form.type,
      content: form.content.trim(),
      planDate: form.planDate || null,
      handler: form.handler.trim() || null
    })
  } catch {
    return
  } finally {
    busy.value = false
  }
  uni.showToast({ title: t('mes.maintenance.created'), icon: 'success' })
  setTimeout(() => uni.navigateBack(), 800)
}

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}
</script>

<style lang="scss">
.ma {
  &__row {
    display: flex;
    align-items: center;
    min-height: 72rpx;
  }
  &__link {
    margin-left: auto;
    padding-left: 16rpx;
    font-size: 26rpx;
    color: $l-primary;
  }
}
</style>
