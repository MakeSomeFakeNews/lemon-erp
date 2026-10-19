<template>
  <view>
    <view v-if="m">
      <view class="md__head">
        <view class="md__title">{{ m.equipmentName }}</view>
        <view class="md__sub">
          <text>{{ m.equipmentCode }}</text>
          <l-tag :text="dictText(dict.mes_maintain_type, m.type)" :color="m.type === MaintenanceType.REPAIR ? 'red' : 'blue'" />
          <l-tag :text="dictText(dict.mes_maintain_status, m.status)" :color="dictColor(dict.mes_maintain_status, m.status)" />
        </view>
      </view>
      <l-card>
        <l-cell :label="t('mes.maintenance.orderNo')" :value="m.orderNo" />
        <l-cell :label="t('mes.maintenance.content')" :value="m.content" />
        <l-cell :label="t('mes.maintenance.planDate')" :value="m.planDate" />
        <l-cell :label="t('mes.maintenance.createBy')" :value="[m.createByName, shortTime(m.createTime)].filter(Boolean).join(' ')" />
        <l-cell :label="t('mes.maintenance.handler')" :value="m.handler" />
        <l-cell v-if="m.startTime" :label="t('mes.maintenance.startTime')" :value="shortTime(m.startTime)" />
        <l-cell v-if="m.finishTime" :label="t('mes.maintenance.finishTime')" :value="shortTime(m.finishTime)" />
        <l-cell v-if="m.downtime != null" :label="t('mes.maintenance.downtime')" :value="t('mes.maintenance.downtimeHours', { n: qty(m.downtime) })" />
        <l-cell v-if="m.cost != null" :label="t('mes.maintenance.cost')" :value="money(m.cost)" />
        <l-cell v-if="m.result" :label="t('mes.maintenance.result')" :value="m.result" />
      </l-card>

      <!-- 完成：填写处理结果、费用 -->
      <l-card v-if="finishing" :title="t('mes.maintenance.finishTitle')">
        <l-field :label="t('mes.maintenance.result')" required>
          <textarea v-model="form.result" class="l-textarea" maxlength="255" :placeholder="t('mes.maintenance.resultPh')" placeholder-class="l-placeholder" />
        </l-field>
        <l-field :label="t('mes.maintenance.cost')">
          <input v-model="form.cost" class="l-input" type="digit" :placeholder="t('mes.common.optional')" placeholder-class="l-placeholder" />
        </l-field>
        <l-field :label="t('mes.maintenance.handler')">
          <input v-model="form.handler" class="l-input" maxlength="64" :placeholder="t('mes.maintenance.handlerPh')" placeholder-class="l-placeholder" />
        </l-field>
      </l-card>

      <template v-if="canHandle && m.status !== MaintenanceStatus.DONE">
        <view class="l-footer-space" />
        <view class="l-footer">
          <button v-if="m.status === MaintenanceStatus.PENDING" class="l-btn" @click="start">{{ t('mes.maintenance.start') }}</button>
          <template v-else-if="!finishing">
            <button class="l-btn" @click="finishing = true">{{ t('mes.maintenance.finish') }}</button>
          </template>
          <template v-else>
            <button class="l-btn l-btn--default" @click="finishing = false">{{ t('common.cancel') }}</button>
            <button class="l-btn" @click="finish">{{ t('common.submit') }}</button>
          </template>
        </view>
      </template>
    </view>
    <l-empty v-else-if="notFound" :text="t('mes.maintenance.notFound')" />
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { MaintenanceStatus, MaintenanceType } from '@web/constants/enums'
import { finishMaintenance, pageMaintenance, startMaintenance } from '@/api/mes'
import { runAction } from '@/composables/useAction'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { hasPerm, userState } from '@/store/user'
import { money, qty, shortTime } from '@/utils/format'

/** 维护单详情：开始处理、完成（填写结果、费用）。后端没有单条查询接口，按单号查询后取对应的记录 */
const { t } = useI18n()
useTitle('mes.entry.maintenance')
const dict = useDict('mes_maintain_type', 'mes_maintain_status')
const id = ref(null)
const no = ref('')
const m = ref(null)
const notFound = ref(false)
const finishing = ref(false)
const form = reactive({ result: '', cost: '', handler: '' })
const canHandle = computed(() => hasPerm('mes:maintain:handle'))
const me = () => userState.user?.nickname || userState.user?.username || ''

onLoad((q) => {
  id.value = Number(q.id)
  no.value = decodeURIComponent(q.no || '')
  load()
})

async function load() {
  const page = await pageMaintenance({ keyword: no.value, current: 1, size: 20 }).catch(() => null)
  m.value = (page?.records || []).find((r) => r.id === id.value) || null
  notFound.value = !m.value
  finishing.value = false
  if (m.value) form.handler = m.value.handler || me()
}

function start() {
  runAction(() => startMaintenance(m.value.id, { handler: m.value.handler || me() }), {
    confirm: m.value.type === MaintenanceType.REPAIR ? t('mes.maintenance.startConfirm') : undefined,
    success: t('mes.maintenance.started'),
    then: load
  })
}

function finish() {
  if (!form.result.trim()) return uni.showToast({ title: t('common.required', { name: t('mes.maintenance.result') }), icon: 'none' })
  const cost = form.cost === '' ? null : Number(form.cost)
  if (cost !== null && !(cost >= 0)) return uni.showToast({ title: t('common.required', { name: t('mes.maintenance.cost') }), icon: 'none' })
  runAction(() => finishMaintenance(m.value.id, { result: form.result.trim(), cost, handler: form.handler.trim() || null }), {
    success: t('mes.maintenance.finished'),
    then: load
  })
}
</script>

<style lang="scss">
.md {
  &__head {
    padding: 32rpx 32rpx 0;
  }
  &__title {
    font-size: 36rpx;
    font-weight: 600;
  }
  &__sub {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-top: 8rpx;
    font-size: 26rpx;
    color: $l-text-3;
  }
}
</style>
