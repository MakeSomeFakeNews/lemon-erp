<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="guard(search)" @reset="guard(resetQuery)">
        <a-form-item :label="$t('wms.common.material')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('pricing.goods.keyword')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('pricing.goods.category')" name="category">
          <a-select v-model:value="query.category" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wms_material_category)" />
        </a-form-item>
        <a-form-item :label="$t('pricing.goods.brand')" name="brand">
          <a-select v-model:value="query.brand" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wms_material_brand)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:show-search="showSearch" :columns="columns" @refresh="guard(load)">
        <template v-if="canEdit">
          <a-button type="primary" :disabled="!changedRows.length" :loading="saving" @click="save">
            <template #icon><SaveOutlined /></template>{{ $t('pricing.goods.save') }}{{ changedRows.length ? ` (${changedRows.length})` : '' }}
          </a-button>
          <a-button :disabled="!changedRows.length" @click="discard">{{ $t('pricing.goods.discard') }}</a-button>
          <a-dropdown :disabled="!list.length">
            <a-button>{{ $t('pricing.goods.fill') }} <DownOutlined /></a-button>
            <template #overlay>
              <a-menu @click="({ key }) => openFill(Number(key))">
                <a-menu-item v-for="l in enabledLevels" :key="l.level">{{ l.name }}</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
        <span class="sub-text tip">{{ $t('pricing.goods.tip') }}</span>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="(pag) => guard(() => onTableChange(pag))"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div>{{ record.name }}</div>
            <div class="sub-text">{{ record.code }}<template v-if="record.spec"> · {{ record.spec }}</template></div>
          </template>
          <template v-else-if="column.key === 'category'">
            <DictTag type="wms_material_category" :value="record.category" />
          </template>
          <template v-else-if="column.key === 'brand'">
            <DictTag type="wms_material_brand" :value="record.brand" />
          </template>
          <template v-else-if="column.key === 'refCost'">{{ record.refCost == null ? '-' : price(record.refCost) }}</template>
          <template v-else-if="column.priceLevel != null">
            <a-input-number
              v-if="canEdit"
              v-model:value="record[column.key]"
              :min="0"
              :precision="4"
              size="small"
              class="full"
              :class="{ changed: isChanged(record, column.key) }"
            />
            <span v-else>{{ record[column.key] == null ? '-' : price(record[column.key]) }}</span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="fill.open" :title="$t('pricing.goods.fillTitle', { name: levelName(fill.level) })" @ok="applyFill">
      <a-form layout="vertical">
        <a-form-item :label="$t('pricing.goods.fillMode')">
          <a-radio-group v-model:value="fill.mode">
            <a-radio value="fixed">{{ $t('pricing.goods.fillFixed') }}</a-radio>
            <a-radio v-if="fill.level !== 0" value="retail">{{ $t('pricing.goods.fillFromRetail') }}</a-radio>
            <a-radio value="cost">{{ $t('pricing.goods.fillFromCost') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="fill.mode === 'fixed' ? $t('pricing.goods.fillValue') : $t('pricing.goods.fillPercent')" :extra="$t('pricing.goods.fillHint')">
          <a-input-number v-model:value="fill.value" :min="0" :precision="4" class="full" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { DownOutlined, SaveOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { pageGoodsPrice, saveGoodsPrices } from '@/api/scm/pricing'
import { PRICE_FIELDS, usePriceLevels } from '@/components/wms/priceLevels'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { price, round4 } from '@/utils/number'

/**
 * 商品价格批量维护：表格中直接修改多个商品的零售价、批发价（启用的价格列），修改过的单元格高亮，一次保存。
 * 价格为含税、基本单位、本位币；可按分类、品牌、关键字筛选，可按统一价格 / 零售价比例 / 参考成本加价批量填充当前页
 */
const { t } = useI18n()
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('scm:goodsPrice:edit'))
const dict = useDict('sys_common_status', 'wms_material_category', 'wms_material_brand')
const { enabledLevels, levelName } = usePriceLevels()
const showSearch = ref(true)
const saving = ref(false)

const columns = computed(() => [
  { title: t('wms.common.material'), key: 'name', width: 240, fixed: 'left' },
  { title: t('common.field.unit'), dataIndex: 'unit', key: 'unit', width: 70 },
  { title: t('pricing.goods.category'), key: 'category', width: 100, dict: 'wms_material_category' },
  { title: t('pricing.goods.brand'), key: 'brand', width: 100, dict: 'wms_material_brand' },
  { title: t('pricing.goods.refCost'), key: 'refCost', dataIndex: 'refCost', width: 100, align: 'right' },
  ...enabledLevels.value.map((l) => ({ title: l.name, key: l.field, dataIndex: l.field, priceLevel: l.level, width: 130, align: 'right' }))
])
const scrollX = computed(() => columns.value.reduce((s, c) => s + c.width, 0))

const query = reactive({ keyword: undefined, category: undefined, brand: undefined, status: undefined })
/** 本页各行的原始价格：id → { 字段: 值 } */
const original = new Map()
const toNum = (v) => (v == null ? null : Number(v))
async function fetchPage(params) {
  const page = await pageGoodsPrice(params)
  page.records.forEach((r) => PRICE_FIELDS.forEach((k) => (r[k] = toNum(r[k]))))
  return page
}
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(async (params) => {
  const page = await fetchPage(params)
  original.clear()
  page.records.forEach((r) => original.set(r.id, Object.fromEntries(PRICE_FIELDS.map((k) => [k, r[k]]))))
  return page
}, query)
pagination.pageSize = 50

const isChanged = (record, field) => (original.get(record.id)?.[field] ?? null) !== (record[field] ?? null)
const changedRows = computed(() => list.value.filter((r) => enabledLevels.value.some((l) => isChanged(r, l.field))))

function resetQuery() {
  Object.assign(query, { keyword: undefined, category: undefined, brand: undefined, status: undefined })
  search()
}

/** 有未保存的修改时先确认再执行（翻页、筛选、刷新） */
function guard(action) {
  if (!changedRows.value.length) return action()
  Modal.confirm({ title: t('pricing.goods.leaveConfirm'), onOk: action })
}

function discard() {
  list.value.forEach((r) => Object.assign(r, original.get(r.id)))
}

async function save() {
  // 只提交改过的价格列（后端只更新出现的字段，清空的列显式传 null）
  const rows = changedRows.value.map((r) => ({
    id: r.id,
    ...Object.fromEntries(enabledLevels.value.filter((l) => isChanged(r, l.field)).map((l) => [l.field, r[l.field] ?? null]))
  }))
  if (!rows.length) {
    message.info(t('pricing.goods.noChange'))
    return
  }
  saving.value = true
  try {
    const n = await saveGoodsPrices(rows)
    message.success(t('pricing.goods.saved', { n }))
    await load()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 批量填充（当前页）
const fill = reactive({ open: false, level: 0, mode: 'fixed', value: undefined })
function openFill(level) {
  Object.assign(fill, { open: true, level, mode: 'fixed', value: undefined })
}
function applyFill() {
  if (fill.value == null) return
  const field = PRICE_FIELDS[fill.level]
  const v = Number(fill.value)
  list.value.forEach((r) => {
    if (fill.mode === 'fixed') r[field] = v
    else if (fill.mode === 'retail' && r.retailPrice != null) r[field] = round4((r.retailPrice * v) / 100)
    else if (fill.mode === 'cost' && r.refCost != null) r[field] = round4((Number(r.refCost) * (100 + v)) / 100)
  })
  fill.open = false
}

onMounted(load)
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.tip {
  align-self: center;
}
.full {
  width: 100%;
}
.changed :deep(.ant-input-number-input) {
  color: var(--app-primary);
  font-weight: 600;
}
.changed {
  border-color: var(--app-primary);
}
</style>
