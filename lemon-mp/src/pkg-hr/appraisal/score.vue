<template>
  <view v-if="a">
    <l-card>
      <l-cell :label="t('hr.perf.plan')" :value="a.planName" />
      <l-cell :label="t('hr.perf.appraisee')" :value="[a.empNo, a.employeeName].filter(Boolean).join(' ')" />
      <l-cell :label="t('hr.perf.reviewer')" :value="a.reviewerName" />
      <l-cell :label="t('common.status')">
        <l-tag :text="dictText(dict.hr_appraisal_status, a.status)" :color="dictColor(dict.hr_appraisal_status, a.status)" />
      </l-cell>
      <template v-if="mode === 'view'">
        <l-cell :label="t('hr.perf.selfScore')" :value="a.selfScore" />
        <l-cell :label="t('hr.perf.score')" :value="a.score" />
        <l-cell :label="t('hr.perf.grade')">
          <l-tag v-if="a.grade" :text="dictText(dict.hr_perf_grade, a.grade)" :color="dictColor(dict.hr_perf_grade, a.grade)" />
          <text v-else>-</text>
        </l-cell>
      </template>
    </l-card>

    <l-card>
      <view v-for="r in rows" :key="r.indicatorId" class="score__row">
        <view class="score__info">
          <view class="score__name">{{ r.indicatorName }}</view>
          <view v-if="r.description" class="score__desc">{{ r.description }}</view>
          <view class="score__meta">
            <text>{{ t('hr.perf.weight', { n: Number(r.weight) }) }}</text>
            <text v-if="mode !== 'self' && r.selfScore !== null && r.selfScore !== undefined">{{ t('hr.perf.selfOf', { n: r.selfScore }) }}</text>
          </view>
        </view>
        <input
          v-if="editable"
          v-model="r.input"
          class="score__input"
          type="digit"
          :placeholder="t('hr.perf.scorePh')"
          placeholder-class="l-placeholder"
        />
        <text v-else class="score__value">{{ r.score ?? '-' }}</text>
      </view>
      <view v-if="editable" class="score__total">
        <text>{{ t('hr.perf.weightedScore') }}</text>
        <text class="score__total-value">{{ total }}</text>
        <l-tag v-if="mode === 'review'" :text="grade" :color="gradeColor" />
      </view>
    </l-card>

    <l-card v-if="mode !== 'self' && a.selfComment" :title="t('hr.perf.selfComment')">
      <view class="score__text">{{ a.selfComment }}</view>
    </l-card>
    <l-card v-if="mode === 'view' && a.reviewComment" :title="t('hr.perf.comment')">
      <view class="score__text">{{ a.reviewComment }}</view>
    </l-card>
    <l-card v-if="editable">
      <textarea
        v-model="comment"
        class="l-textarea"
        maxlength="500"
        :placeholder="mode === 'self' ? t('hr.perf.selfSummaryPh') : t('hr.perf.commentPh')"
        placeholder-class="l-placeholder"
      />
    </l-card>

    <template v-if="editable">
      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn" @click="submit">{{ t('common.submit') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { getAppraisal, reviewAppraisal, selfEvaluate } from '@/api/hr'
import { runAction } from '@/composables/useAction'
import { dictColor, dictText, useDict } from '@/store/dict'

/** 绩效自评 / 评分 / 查看结果：mode = self | review | view；每个指标 0-100 分，按权重加权 */
const { t } = useI18n()
const dict = useDict('hr_appraisal_status', 'hr_perf_grade')
const mode = ref('view')
const id = ref(null)
const a = ref(null)
const rows = ref([])
const comment = ref('')
const editable = computed(() => mode.value === 'self' || mode.value === 'review')

const total = computed(() => (rows.value.reduce((s, r) => s + (Number(r.input) || 0) * Number(r.weight), 0) / 100).toFixed(1))
const grade = computed(() => {
  const s = Number(total.value)
  return s >= 90 ? 'S' : s >= 80 ? 'A' : s >= 70 ? 'B' : s >= 60 ? 'C' : 'D'
})
const gradeColor = computed(() => ({ S: 'gold', A: 'green', B: 'blue', C: 'orange', D: 'red' })[grade.value])

onLoad(async (q) => {
  id.value = q.id
  mode.value = ['self', 'review'].includes(q.mode) ? q.mode : 'view'
  uni.setNavigationBarTitle({ title: t(mode.value === 'self' ? 'hr.title.selfScore' : mode.value === 'review' ? 'hr.title.review' : 'hr.title.appraisal') })
  try {
    const data = await getAppraisal(id.value)
    a.value = data
    comment.value = (mode.value === 'self' ? data.selfComment : data.reviewComment) || ''
    rows.value = (data.scores || []).map((s) => {
      const v = mode.value === 'self' ? s.selfScore : (s.score ?? s.selfScore)
      return { ...s, input: v === null || v === undefined ? '' : String(v) }
    })
  } catch {
    // 错误已提示
  }
})

function valid(v) {
  if (v === '' || v === null || v === undefined) return false
  const n = Number(v)
  return Number.isFinite(n) && n >= 0 && n <= 100
}

function submit() {
  if (rows.value.some((r) => !valid(r.input))) {
    uni.showToast({ title: t('hr.perf.scoreAll'), icon: 'none' })
    return
  }
  const data = { scores: rows.value.map((r) => ({ indicatorId: r.indicatorId, score: Number(r.input) })), comment: comment.value.trim() || null }
  const self = mode.value === 'self'
  runAction(() => (self ? selfEvaluate(id.value, data) : reviewAppraisal(id.value, data)), {
    confirm: self ? t('hr.perf.confirmSubmit') : undefined,
    success: self ? t('hr.perf.selfSubmitted') : t('hr.perf.reviewSaved'),
    then: () => setTimeout(() => uni.navigateBack(), 800)
  })
}
</script>

<style lang="scss">
.score {
  &__row {
    display: flex;
    align-items: center;
    gap: 24rpx;
    padding: 20rpx 0;
    border-bottom: 1rpx solid $l-border;
  }
  &__info {
    flex: 1;
  }
  &__name {
    font-size: 28rpx;
    font-weight: 600;
  }
  &__desc {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: $l-text-3;
    line-height: 1.5;
  }
  &__meta {
    display: flex;
    gap: 20rpx;
    margin-top: 6rpx;
    font-size: 22rpx;
    color: $l-text-2;
  }
  &__input {
    width: 140rpx;
    height: 68rpx;
    padding: 0 16rpx;
    border: 1rpx solid #d9d9d9;
    border-radius: 12rpx;
    text-align: center;
    font-size: 30rpx;
  }
  &__value {
    font-size: 32rpx;
    font-weight: 600;
  }
  &__total {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 16rpx;
    padding-top: 20rpx;
    font-size: 26rpx;
    color: $l-text-2;
  }
  &__total-value {
    font-size: 36rpx;
    font-weight: 600;
    color: $l-primary;
  }
  &__text {
    font-size: 26rpx;
    line-height: 1.6;
    color: $l-text-2;
    white-space: pre-wrap;
  }
}
</style>
