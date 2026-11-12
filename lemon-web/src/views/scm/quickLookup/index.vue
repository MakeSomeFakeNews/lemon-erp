<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="ql-toolbar">
        <a-input-search
          v-model:value="keyword"
          :placeholder="$t('pricing.lookup.keyword')"
          allow-clear
          class="ql-search"
          @search="search"
          @change="onInput"
        />
        <span class="ql-setting">
          {{ $t('pricing.lookup.priceSetting') }}
          <a-select :value="selected" class="ql-select" :loading="saving" @change="changePrice">
            <a-select-opt-group v-for="g in groups" :key="g.key" :label="g.label">
              <a-select-option v-for="o in g.options" :key="o.type" :value="o.type" :disabled="!o.allowed">
                {{ o.label }}<span v-if="!o.allowed" class="sub-text"> ({{ $t('pricing.lookup.noPermission') }})</span>
              </a-select-option>
            </a-select-opt-group>
          </a-select>
          <a-tooltip :title="$t('pricing.lookup.priceSettingTip')"><QuestionCircleOutlined class="sub-text" /></a-tooltip>
        </span>
        <span class="sub-text ql-tip">{{ priceDenied ? $t('pricing.lookup.priceDenied') : $t('pricing.lookup.tip') }}</span>
      </div>

      <a-table
        row-key="materialId"
        size="middle"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: scrollX, y: '100%' }"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'index'">{{ index + 1 }}</template>
          <template v-else-if="column.key === 'name'">
            <div class="ql-name" @click="imageRef.open(record)">
              <img v-if="record.imageUrl" :src="record.imageUrl" class="ql-thumb" alt="" />
              <div>
                <a>{{ record.name }}</a>
                <div class="sub-text">{{ [record.spec, record.code].filter(Boolean).join(' · ') }}</div>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a :class="{ 'ql-zero': !(Number(record.qty) > 0) }" @click="stockRef.open(record)">{{ qty(record.qty) }}</a>
          </template>
          <template v-else-if="column.key === 'price'">
            <a @click="historyRef.open(record, kindOfPrice(shownPrice))">{{ record.price == null ? '-' : price(record.price) }}</a>
          </template>
        </template>
        <template #footer>
          <div class="ql-footer">
            <a-button v-if="hasMore" :loading="loading" @click="load(page + 1)">{{ $t('pricing.lookup.loadMore') }}</a-button>
          </div>
        </template>
      </a-table>
    </a-card>

    <QuickLookupStock ref="stockRef" />
    <QuickLookupImages ref="imageRef" />
    <QuickLookupHistory ref="historyRef" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import { getQuickLookupSetting, quickPriceOptions, saveQuickLookupSetting, searchQuickLookup } from '@/api/scm/quickLookup'
import { QuickPrice } from '@/constants/enums'
import { price, qty } from '@/utils/number'
import { kindOfPrice } from './lookup'
import QuickLookupStock from '@/components/wms/StockDistributionModal.vue'
import QuickLookupImages from './QuickLookupImages.vue'
import QuickLookupHistory from './QuickLookupHistory.vue'

/**
 * 商品速查（issue #97，与小程序共用接口和个人设置 quickLookup）：按名称、规格、编码、简拼搜索商品，显示可用库存和选择的一种价格。
 * 价格由后端按权限过滤（没有权限只显示数量）；点击商品看商品图片（issue #94，有图片的显示主图缩略图），点击库存看各仓库 / 库位 / 批次，
 * 点击价格看历史参考（进货类价格看进价历史，其他看售价历史），可再联查往来明细和价格参考
 */
const { t } = useI18n()
const PAGE_SIZE = 50
const keyword = ref('')
const rows = ref([])
const loading = ref(false)
const hasMore = ref(false)
const options = ref([])
const setting = ref(null)
const shownPrice = ref(null)
const shownLabel = ref('')
const priceDenied = ref(false)
const saving = ref(false)
const stockRef = ref()
const imageRef = ref()
const historyRef = ref()

const selected = computed(() => setting.value || QuickPrice.LAST_SALES)
const SIDES = ['PURCHASE', 'COST', 'SALES', null]
const groups = computed(() =>
  SIDES.map((side) => ({
    key: side || 'none',
    label: t(`pricing.lookup.side${side || 'None'}`),
    options: options.value.filter((o) => (o.side || null) === side)
  })).filter((g) => g.options.length)
)

const columns = computed(() => [
  { title: t('pricing.lookup.index'), key: 'index', width: 60, align: 'center' },
  { title: t('pricing.lookup.product'), key: 'name', width: 320 },
  { title: t('common.field.unit'), dataIndex: 'unit', key: 'unit', width: 80 },
  { title: t('pricing.lookup.stock'), key: 'qty', width: 120, align: 'right' },
  ...(shownPrice.value ? [{ title: shownLabel.value || t('pricing.lookup.price'), key: 'price', width: 140, align: 'right' }] : [])
])
const scrollX = computed(() => columns.value.reduce((s, c) => s + c.width, 0))

let page = 1
let seq = 0
let timer
async function load(p) {
  const current = ++seq
  loading.value = true
  try {
    const res = await searchQuickLookup({ keyword: keyword.value.trim() || undefined, price: selected.value, current: p, size: PAGE_SIZE })
    if (current !== seq) return
    page = p
    const list = res?.rows || []
    rows.value = p === 1 ? list : rows.value.concat(list.filter((r) => !rows.value.some((x) => x.materialId === r.materialId)))
    shownPrice.value = res?.price || null
    shownLabel.value = res?.priceLabel || ''
    priceDenied.value = !!res?.priceDenied
    hasMore.value = !!res?.hasMore
  } finally {
    if (current === seq) loading.value = false
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
onBeforeUnmount(() => clearTimeout(timer))

async function changePrice(type) {
  saving.value = true
  try {
    await saveQuickLookupSetting({ price: type })
    setting.value = type
    message.success(t('pricing.lookup.saved'))
    search()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const [s, opts] = await Promise.all([getQuickLookupSetting().catch(() => null), quickPriceOptions().catch(() => [])])
  setting.value = s?.price || null
  options.value = opts || []
  search()
})
</script>

<style scoped>
.ql-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.ql-search {
  width: 320px;
  max-width: 100%;
}
.ql-setting {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.ql-select {
  width: 180px;
}
.ql-tip {
  flex: 1;
  min-width: 200px;
}
.ql-name {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.ql-thumb {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
}
.ql-zero {
  color: var(--app-danger, #ff4d4f);
}
.ql-footer {
  text-align: center;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
