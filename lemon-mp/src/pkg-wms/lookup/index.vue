<template>
  <view class="lk">
    <!-- 顶部：搜索（输入法语音输入，配置了同声传译插件时可按住说话）、查询价格设置 -->
    <view class="lk-top">
      <view class="l-search lk-search">
        <input
          v-model="keyword"
          :placeholder="t('lookup.searchPh')"
          placeholder-class="l-placeholder"
          confirm-type="search"
          @input="onInput"
          @confirm="search"
        />
        <text v-if="keyword" class="lk-clear" @click="clear">×</text>
      </view>
      <view v-if="voice" class="lk-voice" :class="{ 'lk-voice--on': recording }" @touchstart.prevent="voiceStart" @touchend="voiceEnd" @touchcancel="voiceEnd">
        {{ recording ? t('lookup.voiceRelease') : t('lookup.voiceHold') }}
      </view>
    </view>
    <view class="lk-bar">
      <text class="l-muted lk-bar-tip">{{ priceDenied ? t('lookup.priceDenied') : t('lookup.tapTip') }}</text>
      <text class="m-link" @click="openSetting">{{ priceLabel }} ▾</text>
    </view>

    <!-- 列表：序号、商品描述（名称 + 规格）、库存、价格；点击各列联查，长按更多 -->
    <view class="lk-head">
      <text class="lk-c-index">{{ t('lookup.colIndex') }}</text>
      <text class="lk-c-name">{{ t('lookup.colName') }}</text>
      <text class="lk-c-qty">{{ t('lookup.colQty') }}</text>
      <text v-if="showPrice" class="lk-c-price">{{ t('lookup.colPrice') }}</text>
    </view>
    <view v-for="(r, i) in rows" :key="r.materialId" class="lk-row" @longpress="more(r)">
      <text class="lk-c-index l-muted">{{ i + 1 }}</text>
      <view class="lk-c-name" @click="imageRef.open(r)">
        <image v-if="r.imageUrl" class="lk-img" :src="fileUrl(r.imageUrl)" mode="aspectFill" />
        <view class="lk-name-text">
          <text class="lk-name">{{ r.name }}</text>
          <text class="lk-spec">{{ [r.spec, r.code].filter(Boolean).join(' · ') }}</text>
        </view>
      </view>
      <view class="lk-c-qty" @click="stockRef.open(r)">
        <text class="lk-qty" :class="{ 'lk-qty--zero': !(Number(r.qty) > 0) }">{{ qty(r.qty) }}</text>
        <text class="lk-unit">{{ r.unit }}</text>
      </view>
      <view v-if="showPrice" class="lk-c-price" @click="priceRef.open(r, kindOfPrice(shownPrice))">
        <text class="lk-price">{{ price(r.price) }}</text>
      </view>
    </view>
    <l-empty v-if="loaded && !rows.length" />
    <l-load-more v-if="rows.length" :status="status" />

    <LookupSettingSheet ref="settingRef" @change="changePrice" />
    <LookupStockSheet ref="stockRef" />
    <LookupPriceSheet ref="priceRef" />
    <LookupImageSheet ref="imageRef" />
  </view>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { OrderKind, QuickPrice } from '@web/constants/enums'
import { getLookupSetting, lookupPriceOptions, searchLookup } from '@/api/lookup'
import { useTitle } from '@/composables/useTitle'
import { hasPerm } from '@/store/user'
import { ensureLogin } from '@/utils/session'
import { fileUrl } from '@/utils/request'
import { price, qty } from '@/utils/format'
import { canViewKind, kindOfPrice } from './lookup'
import { startVoice, stopVoice, voiceAvailable } from './voice'
import LookupSettingSheet from './LookupSettingSheet.vue'
import LookupStockSheet from './LookupStockSheet.vue'
import LookupPriceSheet from './LookupPriceSheet.vue'
import LookupImageSheet from './LookupImageSheet.vue'

/**
 * 商品速查（issue #97）：给客户报价时快速查商品的库存和价格。
 * 搜索规则同物料下拉（名称、规格、编码、条码、拼音首字母、去掉分隔符的缩写）；价格列显示个人设置 quickLookup 中选择的一种价格
 * （与 Web 端「供应链 - 销售 - 商品速查」共用，后端按权限过滤，没有权限只显示数量）。
 * 有商品图片（issue #94）的显示主图缩略图。小程序没有双击：点击商品看图片、点击库存看各仓库 / 批次分布、点击价格看历史参考（进货类价格看进价历史，其他看售价历史），
 * 长按一行弹出全部联查（含售价 / 进价历史、经营历史）
 */
const { t } = useI18n()
useTitle('lookup.entry')
const keyword = ref('')
const rows = ref([])
const loaded = ref(false)
/** loading | more | noMore */
const status = ref('more')
const options = ref([])
const setting = ref(null)
/** 后端实际显示的价格（没有权限时为空） */
const shownPrice = ref(null)
const shownLabel = ref('')
const priceDenied = ref(false)
const voice = voiceAvailable()
const recording = ref(false)

const settingRef = ref()
const stockRef = ref()
const priceRef = ref()
const imageRef = ref()

const selected = computed(() => setting.value || QuickPrice.LAST_SALES)
const showPrice = computed(() => !!shownPrice.value)
const priceLabel = computed(() => {
  if (selected.value === QuickPrice.NONE) return t('lookup.noPrice')
  return shownLabel.value || options.value.find((o) => o.type === selected.value)?.label || t('lookup.colPrice')
})

onLoad(async () => {
  if (!(await ensureLogin())) return
  const [s, opts] = await Promise.all([getLookupSetting().catch(() => null), lookupPriceOptions().catch(() => [])])
  setting.value = s?.price || null
  options.value = opts || []
  search()
})
onReachBottom(() => {
  if (status.value === 'more') load(page + 1)
})

let page = 1
let seq = 0
let timer
async function load(p) {
  const current = ++seq
  status.value = 'loading'
  try {
    const res = await searchLookup({ keyword: keyword.value.trim() || undefined, price: selected.value, current: p })
    if (current !== seq) return
    page = p
    const list = res?.rows || []
    rows.value = p === 1 ? list : rows.value.concat(list.filter((r) => !rows.value.some((x) => x.materialId === r.materialId)))
    shownPrice.value = res?.price || null
    shownLabel.value = res?.priceLabel || ''
    priceDenied.value = !!res?.priceDenied
    status.value = res?.hasMore ? 'more' : 'noMore'
  } catch (e) {
    if (current === seq) status.value = 'more'
  } finally {
    if (current === seq) loaded.value = true
  }
}

function search() {
  clearTimeout(timer)
  load(1)
}

function onInput() {
  clearTimeout(timer)
  timer = setTimeout(search, 400)
}

function clear() {
  keyword.value = ''
  search()
}
onBeforeUnmount(() => clearTimeout(timer))

function openSetting() {
  settingRef.value.open(options.value, selected.value)
}

function changePrice(type) {
  setting.value = type
  search()
}

/** 长按：全部联查 */
function more(r) {
  const actions = [
    { label: t('lookup.actions.image'), run: () => imageRef.value.open(r) },
    { label: t('lookup.actions.stock'), run: () => stockRef.value.open(r) },
    canViewKind(OrderKind.SALES) ? { label: t('lookup.actions.sales'), run: () => priceRef.value.open(r, OrderKind.SALES) } : null,
    canViewKind(OrderKind.PURCHASE) ? { label: t('lookup.actions.purchase'), run: () => priceRef.value.open(r, OrderKind.PURCHASE) } : null,
    hasPerm('wms:stock:list')
      ? { label: t('lookup.actions.history'), run: () => uni.navigateTo({ url: `/pkg-wms/stock/history?materialId=${r.materialId}&name=${encodeURIComponent(r.name)}` }) }
      : null
  ].filter(Boolean)
  uni.showActionSheet({ itemList: actions.map((a) => a.label), success: ({ tapIndex }) => actions[tapIndex]?.run() })
}

// ---------------------------------------------------------------- 语音（同声传译插件，见 voice.js）
function voiceStart() {
  recording.value = startVoice({
    onResult: (text) => {
      recording.value = false
      keyword.value = text
      search()
    },
    onError: (key) => {
      recording.value = false
      uni.showToast({ title: t(key), icon: 'none' })
    }
  })
}

function voiceEnd() {
  if (!recording.value) return
  stopVoice()
}
</script>

<style lang="scss">
@import '../mobile.scss';

.lk {
  padding-bottom: env(safe-area-inset-bottom);
}
.lk-top {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 24rpx 0;
}
.lk-search {
  flex: 1;
  margin: 0;
  display: flex;
  align-items: center;
  input {
    flex: 1;
  }
}
.lk-clear {
  padding: 0 8rpx 0 16rpx;
  font-size: 36rpx;
  color: $l-text-3;
}
.lk-voice {
  flex-shrink: 0;
  padding: 16rpx 20rpx;
  font-size: 24rpx;
  color: #fff;
  background: $l-primary;
  border-radius: $l-radius;
  &--on {
    background: $l-danger;
  }
}
.lk-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 16rpx 24rpx;
}
.lk-bar-tip {
  flex: 1;
  min-width: 0;
  font-size: 22rpx;
}
.lk-head,
.lk-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 18rpx 24rpx;
}
.lk-head {
  font-size: 24rpx;
  color: $l-text-3;
}
.lk-row {
  margin: 0 24rpx 12rpx;
  padding: 20rpx 16rpx;
  background: #fff;
  border-radius: $l-radius;
}
.lk-c-index {
  width: 48rpx;
  flex-shrink: 0;
  text-align: center;
  font-size: 24rpx;
}
.lk-c-name {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.lk-name-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.lk-img {
  flex-shrink: 0;
  width: 80rpx;
  height: 80rpx;
  border-radius: 8rpx;
  background: #f5f5f5;
}
.lk-c-qty {
  width: 140rpx;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
.lk-c-price {
  width: 160rpx;
  flex-shrink: 0;
  text-align: right;
}
.lk-name {
  font-size: 28rpx;
  font-weight: 600;
  word-break: break-all;
}
.lk-spec {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: $l-text-3;
  word-break: break-all;
}
.lk-qty {
  font-size: 30rpx;
  font-weight: 600;
  &--zero {
    color: $l-danger;
  }
}
.lk-unit {
  font-size: 22rpx;
  color: $l-text-3;
}
.lk-price {
  font-size: 30rpx;
  font-weight: 600;
  color: $l-primary;
}
</style>
