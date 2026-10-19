<template>
  <view>
    <!-- 选择客户：没有从客户 / 商机进入时 -->
    <template v-if="picking">
      <view class="l-search">
        <input v-model="keyword" :placeholder="t('crm.followup.searchCustomer')" placeholder-class="l-placeholder" confirm-type="search" focus @confirm="customers.refresh()" />
      </view>
      <view v-for="c in customers.list" :key="c.customerId" class="l-item" @click="pick(c)">
        <view class="l-item__head">
          <text class="l-item__title">{{ c.name }}</text>
        </view>
        <view class="l-item__desc">{{ [c.code, c.ownerName].filter(Boolean).join(' · ') }}</view>
      </view>
      <l-empty v-if="customers.loaded && !customers.list.length" />
      <l-load-more v-else-if="customers.list.length" :status="customers.status" />
    </template>

    <template v-else>
      <l-card>
        <l-field :label="t('crm.followup.target')" required>
          <view class="fe__target">
            <text v-if="target.id">{{ t('crm.followup.targetType.' + target.type) }} · {{ target.name }}</text>
            <text v-else class="l-placeholder">{{ t('crm.followup.selectCustomer') }}</text>
            <text v-if="!fixed" class="fe__link" @click="startPick">{{ target.id ? t('crm.followup.change') : t('common.pleaseSelect') }}</text>
          </view>
        </l-field>
        <l-field :label="t('crm.followup.method')" required>
          <picker :range="dict.crm_follow_method" range-key="label" @change="(e) => (form.method = dict.crm_follow_method[e.detail.value].value)">
            <view class="l-input" :class="{ 'l-placeholder': !form.method }">
              {{ form.method ? dictText(dict.crm_follow_method, form.method) : t('common.pleaseSelect') }}
            </view>
          </picker>
        </l-field>
        <l-field :label="t('crm.followup.followTime')" required>
          <view class="fe__dt">
            <picker mode="date" :value="form.followDate" @change="(e) => (form.followDate = e.detail.value)">
              <view class="l-input">{{ form.followDate }}</view>
            </picker>
            <picker mode="time" :value="form.followClock" @change="(e) => (form.followClock = e.detail.value)">
              <view class="l-input">{{ form.followClock }}</view>
            </picker>
          </view>
        </l-field>
        <l-field :label="t('crm.followup.content')" required>
          <textarea v-model="form.content" class="l-textarea" maxlength="1000" :placeholder="t('crm.followup.contentPh')" placeholder-class="l-placeholder" />
        </l-field>
        <l-field :label="t('crm.followup.nextTime')">
          <view class="fe__dt">
            <picker mode="date" :value="form.nextDate" :start="form.followDate" @change="(e) => (form.nextDate = e.detail.value)">
              <view class="l-input" :class="{ 'l-placeholder': !form.nextDate }">{{ form.nextDate || t('common.date') }}</view>
            </picker>
            <picker mode="time" :value="form.nextClock" @change="(e) => (form.nextClock = e.detail.value)">
              <view class="l-input">{{ form.nextClock }}</view>
            </picker>
            <text v-if="form.nextDate" class="fe__link" @click="form.nextDate = ''">{{ t('crm.followup.clear') }}</text>
          </view>
        </l-field>
        <l-field :label="t('crm.followup.nextPlan')">
          <input v-model="form.nextPlan" class="l-input" maxlength="255" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
        </l-field>
        <l-field :label="t('crm.followup.photos', { n: photos.length, max: MAX_PHOTOS })">
          <view class="fe__photos">
            <view v-for="(p, i) in photos" :key="p" class="fe__photo">
              <image :src="p" class="fe__photo-img" mode="aspectFill" @click="previewPhoto(i)" />
              <text class="fe__photo-del" @click="photos.splice(i, 1)">×</text>
            </view>
            <view v-if="photos.length < MAX_PHOTOS" class="fe__photo fe__photo-add" @click="addPhotos">
              <text class="fe__photo-plus">+</text>
              <text class="fe__photo-tip">{{ t('crm.followup.takePhoto') }}</text>
            </view>
          </view>
        </l-field>
      </l-card>
      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn" :loading="busy" :disabled="busy" @click="submit">{{ t('common.save') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { FollowupTarget } from '@web/constants/enums'
import { addFollowup, pageCustomers, uploadFollowupPhoto } from '@/api/crm'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import { dictText, useDict } from '@/store/dict'
import { today } from '@/utils/format'
import { MAX_PHOTOS, choosePhotos } from '../photos'

const { t } = useI18n()
useTitle('crm.followup.editTitle')
const dict = useDict('crm_follow_method')
const DEFAULT_METHOD = 'PHONE'

/** 跟进对象：从客户详情、商机进入时固定，否则选择客户 */
const target = reactive({ type: FollowupTarget.CUSTOMER, id: null, name: '' })
const fixed = ref(false)
const picking = ref(false)
const keyword = ref('')
const busy = ref(false)
const pad = (n) => String(n).padStart(2, '0')
const now = new Date()
const form = reactive({
  method: '',
  followDate: today(),
  followClock: `${pad(now.getHours())}:${pad(now.getMinutes())}`,
  content: '',
  nextDate: '',
  nextClock: '09:00',
  nextPlan: ''
})
/** 现场照片：保存跟进后逐张上传（跟进记录的附件） */
const photos = ref([])
const customers = usePaged((p) => pageCustomers({ ...p, scope: 'team', keyword: keyword.value.trim() }))

// 默认跟进方式为电话（字典加载后）
watch(
  () => dict.crm_follow_method,
  (list) => {
    if (!form.method && list.some((d) => d.value === DEFAULT_METHOD)) form.method = DEFAULT_METHOD
  },
  { immediate: true }
)

onLoad((q) => {
  if (q.targetId) {
    Object.assign(target, { type: q.targetType || FollowupTarget.CUSTOMER, id: Number(q.targetId), name: decodeURIComponent(q.name || '') })
    fixed.value = true
  } else {
    startPick()
  }
})
onReachBottom(() => picking.value && customers.more())

function startPick() {
  picking.value = true
  customers.refresh()
}

function pick(c) {
  Object.assign(target, { type: FollowupTarget.CUSTOMER, id: c.customerId, name: c.name })
  picking.value = false
}

async function submit() {
  if (!target.id) return toast(t('common.selectRequired', { name: t('crm.followup.target') }))
  if (!form.method) return toast(t('common.selectRequired', { name: t('crm.followup.method') }))
  if (!form.content.trim()) return toast(t('common.required', { name: t('crm.followup.content') }))
  const followTime = `${form.followDate} ${form.followClock}:00`
  const nextTime = form.nextDate ? `${form.nextDate} ${form.nextClock}:00` : undefined
  if (nextTime && nextTime <= followTime) return toast(t('crm.followup.nextAfter'))
  busy.value = true
  let id
  try {
    id = await addFollowup({
      targetType: target.type,
      targetId: target.id,
      method: form.method,
      content: form.content.trim(),
      followTime,
      nextTime,
      nextPlan: form.nextPlan.trim() || undefined
    })
  } catch {
    return
  } finally {
    busy.value = false
  }
  // 跟进已保存，照片上传失败时提示，不影响跟进记录
  const failed = await uploadPhotos(id)
  if (failed) uni.showToast({ title: t('crm.followup.photoFailed', { n: failed }), icon: 'none', duration: 2500 })
  else uni.showToast({ title: t('crm.followup.saved'), icon: 'success' })
  setTimeout(() => uni.navigateBack(), failed ? 2000 : 800)
}

/** 逐张上传，返回失败的张数 */
async function uploadPhotos(id) {
  if (!photos.value.length || !id) return 0
  let failed = 0
  for (let i = 0; i < photos.value.length; i++) {
    uni.showLoading({ title: t('crm.followup.uploading', { i: i + 1, n: photos.value.length }), mask: true })
    try {
      await uploadFollowupPhoto(id, photos.value[i])
    } catch {
      failed++
    }
  }
  uni.hideLoading()
  return failed
}

async function addPhotos() {
  const paths = await choosePhotos(MAX_PHOTOS - photos.value.length)
  photos.value = photos.value.concat(paths).slice(0, MAX_PHOTOS)
}

function previewPhoto(i) {
  uni.previewImage({ urls: photos.value, current: photos.value[i] })
}

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}
</script>

<style lang="scss">
.fe {
  &__target {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 72rpx;
  }
  &__link {
    flex-shrink: 0;
    margin-left: 16rpx;
    font-size: 26rpx;
    color: $l-primary;
  }
  &__dt {
    display: flex;
    align-items: center;
    gap: 32rpx;
  }
  &__photos {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
  }
  &__photo {
    position: relative;
    width: 180rpx;
    height: 180rpx;
  }
  &__photo-img {
    width: 180rpx;
    height: 180rpx;
    border-radius: 8rpx;
  }
  &__photo-del {
    position: absolute;
    top: -12rpx;
    right: -12rpx;
    width: 40rpx;
    height: 40rpx;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.55);
    color: #fff;
    font-size: 28rpx;
    line-height: 38rpx;
    text-align: center;
  }
  &__photo-add {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border: 1rpx dashed #d9d9d9;
    border-radius: 8rpx;
    box-sizing: border-box;
    color: $l-text-3;
  }
  &__photo-plus {
    font-size: 56rpx;
    line-height: 1;
  }
  &__photo-tip {
    margin-top: 8rpx;
    font-size: 22rpx;
  }
}
</style>
