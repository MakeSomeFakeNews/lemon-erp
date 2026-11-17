import { reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { t } from '@/locales'
import { useDictStore } from '@/store/dict'
import { findDict } from '@/utils/dict'
import { writeExcel } from '@/utils/excel'
import { useTableSort } from '@/composables/useTableSort'

/** 导出最多读取的页数（每页 500 条） */
const EXPORT_MAX_PAGES = 20

/**
 * 通用分页表格：fetcher 接收 { current, size, ...query }，返回 { records, total }
 * options.params：自定义请求参数（如时间范围拆分为 beginTime/endTime）
 *
 * exportAll(columns)：按当前查询条件导出 Excel（传给 TableToolbar 的 exporter）。列取 dataIndex 或 key 对应的字段，
 * 列定义上 dict: '字典类型' 时导出字典标签，exportValue: (record) => 值 可自定义；操作列不导出。
 * 一列合并显示了几项信息（如「订单 / 交货日期」）时，用 exportColumns: [{ title, dataIndex | exportValue, dict }] 导出为多列，
 * 便于在 Excel 中筛选、计算；金额、数量导出原始数值，不带货币符号和千分位
 *
 * 列头排序（issue #122，服务端排序）：列定义写 sortable: true，并用 useColumns(columns, key, { sorter }) 或 sorter.columns(columns)
 * 生成表格的列；点击列头后带 sortField / sortOrder 回到第一页重新查询，导出按同样的排序（后端须为该接口声明排序白名单）
 */
export function useTable(fetcher, query = {}, options = {}) {
  const route = useRoute()
  const list = ref([])
  const loading = ref(false)
  const size = ref('middle')
  const selectedKeys = ref([])
  const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total) => t('common.table.total', { total })
  })

  const sorter = useTableSort({ remote: true })

  // 请求序号：快速切换筛选条件时，只采用最后一次请求的结果，避免旧数据覆盖新数据
  let requestSeq = 0

  /** 本次查询的参数（不含分页）：查询条件 + 列头排序 */
  const queryParams = () => ({ ...(options.params ? options.params({ ...query }) : { ...query }), ...sorter.params() })

  async function load() {
    const seq = ++requestSeq
    loading.value = true
    try {
      const params = queryParams()
      const res = await fetcher({ current: pagination.current, size: pagination.pageSize, ...params })
      if (seq !== requestSeq) return
      list.value = res.records
      pagination.total = res.total
      // 删除最后一页的全部数据后回到上一页
      if (!res.records.length && pagination.current > 1) {
        pagination.current -= 1
        return load()
      }
    } finally {
      if (seq === requestSeq) loading.value = false
    }
  }

  function search() {
    pagination.current = 1
    load()
  }

  /** 翻页、改每页条数、列头排序（排序变化时回到第一页） */
  function onTableChange(pag, filters, sort) {
    pagination.current = sorter.change(sort) ? 1 : pag.current
    pagination.pageSize = pag.pageSize
    load()
  }

  const rowSelection = reactive({
    selectedRowKeys: selectedKeys,
    onChange: (keys) => (selectedKeys.value = keys)
  })

  async function exportAll(columns, filename) {
    const cols = columns
      .filter((c) => c.key !== 'action' && (c.dataIndex || c.key || c.exportValue || c.exportColumns))
      .flatMap((c) => (c.exportColumns ? c.exportColumns.map((e) => ({ width: c.width, ...e })) : [c]))
    const dictStore = useDictStore()
    const dicts = {}
    await Promise.all(
      [...new Set(cols.map((c) => c.dict).filter(Boolean))].map(async (t) => (dicts[t] = await dictStore.load(t)))
    )
    const params = queryParams()
    const records = []
    let total = 0
    for (let page = 1; page <= EXPORT_MAX_PAGES; page++) {
      const res = await fetcher({ ...params, current: page, size: 500 })
      total = res.total
      records.push(...res.records)
      if (records.length >= total || !res.records.length) break
    }
    if (records.length < total) message.warning(t('components.table.exportLimited', { n: records.length }))
    const valueOf = (c, r) => {
      if (c.exportValue) return c.exportValue(r)
      const field = c.dataIndex || c.key
      let v = Array.isArray(field) ? field.reduce((o, k) => o?.[k], r) : r[field]
      if (c.dict) v = findDict(dicts[c.dict], v)?.label ?? v
      if (Array.isArray(v)) v = v.join(t('components.excel.listSep'))
      return v === null || v === undefined ? '' : typeof v === 'object' ? JSON.stringify(v) : v
    }
    await writeExcel(
      filename || route?.meta?.title || t('common.action.export'),
      cols.map((c) => ({ title: c.title, width: c.width ? Math.round(c.width / 7) : 16 })),
      records.map((r) => cols.map((c) => valueOf(c, r)))
    )
  }

  return { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll, sorter }
}
