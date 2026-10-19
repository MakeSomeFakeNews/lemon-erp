<script setup>
import { onLaunch } from '@dcloudio/uni-app'
import { applyTabBar } from '@/utils/tabbar'
import { loadSubscribeConfig } from '@/utils/subscribe'
import { ensureUser } from '@/store/user'

onLaunch(() => {
  applyTabBar()
  loadSubscribeConfig()
  // 尽早加载当前用户：直接打开子页面（分享、消息链接）时按钮权限依赖它
  ensureUser()
})
</script>

<style lang="scss">
/* 全局样式：页面背景、表单控件、按钮、底部操作栏 */
page {
  background: $l-bg;
  color: $l-text;
  font-size: 28rpx;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.l-input {
  width: 100%;
  min-height: 72rpx;
  font-size: 28rpx;
  line-height: 72rpx;
}

.l-textarea {
  width: 100%;
  height: 180rpx;
  font-size: 28rpx;
}

.l-placeholder {
  color: #bfbfbf;
}

.l-btn {
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
  color: #fff;
  background: $l-primary;
  border: none;
  &::after {
    border: none;
  }
  &[disabled] {
    opacity: 0.5;
    color: #fff !important;
    background: $l-primary !important;
  }
  &--ghost {
    color: $l-primary;
    background: #fff;
    border: 1rpx solid $l-primary;
  }
  &--danger {
    background: $l-danger;
  }
  &--default {
    color: $l-text;
    background: #fff;
    border: 1rpx solid #d9d9d9;
  }
}

/* 底部固定操作栏：页面内容要留出 .l-footer-space 的高度 */
.l-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  display: flex;
  gap: 20rpx;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
  .l-btn {
    flex: 1;
  }
}

.l-footer-space {
  height: calc(128rpx + env(safe-area-inset-bottom));
}

/* 列表项：标题 + 右侧标签，下面一行或多行说明 */
.l-item {
  margin: 20rpx 24rpx;
  padding: 24rpx 28rpx;
  background: #fff;
  border-radius: $l-radius;
  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16rpx;
  }
  &__title {
    flex: 1;
    font-size: 30rpx;
    font-weight: 600;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
  &__desc {
    margin-top: 10rpx;
    font-size: 24rpx;
    color: $l-text-3;
    line-height: 1.6;
  }
}

.l-muted {
  color: $l-text-3;
}

.l-search {
  display: flex;
  align-items: center;
  margin: 20rpx 24rpx;
  padding: 0 24rpx;
  height: 72rpx;
  background: #fff;
  border-radius: 36rpx;
  font-size: 26rpx;
  input {
    flex: 1;
    font-size: 26rpx;
  }
}
</style>
