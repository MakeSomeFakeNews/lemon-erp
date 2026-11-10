<template>
  <view v-if="notice" class="nd">
    <view class="nd__head">
      <view class="nd__title">{{ notice.title }}</view>
      <view class="nd__meta">
        <l-tag v-if="notice.pinned" :text="t('notice.pinned')" color="red" />
        <l-tag v-if="notice.type" :text="dictText(dict.sys_notice_type, notice.type)" :color="dictColor(dict.sys_notice_type, notice.type)" />
        <text class="l-muted">{{ shortTime(notice.publishTime) }}</text>
        <text v-if="notice.publishByName" class="l-muted">{{ t('notice.publishBy', { name: notice.publishByName }) }}</text>
      </view>
      <view v-if="notice.expireDate" class="l-muted nd__expire">{{ t('notice.expireUntil', { date: notice.expireDate }) }}</view>
    </view>
    <view class="nd__body">
      <rich-text v-if="html" :nodes="html" />
      <text v-else class="l-muted">{{ t('notice.noContent') }}</text>
    </view>
    <l-card v-if="files.length" :title="t('notice.attachments')">
      <l-cell v-for="f in files" :key="f.id" :label="f.fileName" arrow @click="openFile(f)" />
    </l-card>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { getNotice, noticeAttachments, readNotice } from '@/api/notice'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { getToken } from '@/utils/auth'
import { BASE_URL } from '@/utils/request'
import { shortTime } from '@/utils/format'
import { renderNotice } from './markdown'

/** 公告详情：打开即标记已读（对应的站内消息一并已读）；附件下载后用系统能力打开（图片预览、文档 openDocument） */
const { t } = useI18n()
useTitle('notice.detailTitle')
const dict = useDict('sys_notice_type')
const notice = ref(null)
const files = ref([])
const html = computed(() => renderNotice(notice.value?.content))

onLoad(async (query) => {
  const id = query?.id
  if (!id) return
  try {
    notice.value = await getNotice(id)
  } catch {
    // 不存在、已撤回或已过期：request 已提示
    setTimeout(() => uni.navigateBack(), 1500)
    return
  }
  if (!notice.value.read) readNotice(id).catch(() => {})
  noticeAttachments(id).then((list) => (files.value = list || [])).catch(() => {})
})

const isImage = (f) => (f.contentType || '').startsWith('image/') || /\.(jpe?g|png|gif|webp)$/i.test(f.fileName || '')

function openFile(f) {
  uni.showLoading({ title: t('common.loading'), mask: true })
  uni.downloadFile({
    url: `${BASE_URL}/system/attachment/${f.id}/download`,
    header: { Authorization: `Bearer ${getToken()}` },
    success: (r) => {
      uni.hideLoading()
      if (r.statusCode !== 200) return uni.showToast({ title: t('notice.openFailed'), icon: 'none' })
      if (isImage(f)) return uni.previewImage({ urls: [r.tempFilePath] })
      uni.openDocument({ filePath: r.tempFilePath, showMenu: true, fail: () => uni.showToast({ title: t('notice.openFailed'), icon: 'none' }) })
    },
    fail: () => {
      uni.hideLoading()
      uni.showToast({ title: t('notice.openFailed'), icon: 'none' })
    }
  })
}
</script>

<style lang="scss">
.nd {
  &__head {
    padding: 32rpx 32rpx 24rpx;
    background: #fff;
  }
  &__title {
    font-size: 36rpx;
    font-weight: 600;
    line-height: 1.4;
    word-break: break-all;
  }
  &__meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12rpx 16rpx;
    margin-top: 16rpx;
    font-size: 24rpx;
  }
  &__expire {
    margin-top: 8rpx;
    font-size: 24rpx;
  }
  &__body {
    margin-top: 16rpx;
    padding: 24rpx 32rpx;
    background: #fff;
    font-size: 30rpx;
    line-height: 1.75;
    word-break: break-all;
  }
}
</style>
