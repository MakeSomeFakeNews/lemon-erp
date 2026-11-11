import zhPrint from '@web/locales/zh-CN/print.js'
import enPrint from '@web/locales/en-US/print.js'
import { FreightBearer, OrderSource } from '@web/constants/enums'
import { isZh, locale, t } from '@/locales'
import { BASE_URL } from '@/utils/request'
import { getToken } from '@/utils/auth'
import { hasPerm, userState } from '@/store/user'
import { dictText, useDict } from '@/store/dict'
import { money, price, qty } from '@/utils/format'
import { getTradeDoc, printTemplates } from '@/api/trade'

/**
 * 手机打印 / 分享 PDF（销售出库、采购入库）：与 Web 端 TradeDocDetail 的 print() 相同的单据结构，
 * 按「打印模板」的配置调整（隐藏、排序、改名、纸张；上传了 JRXML 的由后端按 JRXML 打印），
 * 交给后端 POST /system/print/pdf（JasperReports）生成 PDF，下载到本地后用 uni.openDocument 打开，
 * 用户在右上角菜单中转发给微信好友 / 文件传输助手，或用其他应用（打印机 App、WPS 等）打开打印。
 *
 * 字段、列、签字栏的匹配键是中文打印名称（取自 Web 端语言包 zh-CN/print.js 的 name，模板配置和 JRXML 都按它匹配），
 * 显示名称按界面语言。打印相关的改动（如以后支持更多格式）集中在本文件。
 */
const N = zhPrint.name
const label = (key) => {
  if (isZh()) return key
  const id = Object.keys(N).find((k) => N[k] === key)
  return (id && enPrint.name[id]) || key
}

/** 可打印的单据：kind → 模板标识、单据名称、往来单位、签字栏 */
const KINDS = {
  salesOut: { type: 'scm.salesOut', title: N.salesOut, partner: N.customer, sign: N.customerSign },
  purchaseIn: { type: 'scm.purchaseIn', title: N.purchaseIn, partner: N.supplier, sign: N.supplierSign }
}

/** 是否可以打印该种类的单据（需要查看出入库单的权限） */
export const canPrint = (kind) => !!KINDS[kind] && hasPerm(`scm:${kind}:list`)

const text = (v) => (v === undefined || v === null || v === '' ? null : String(v))
const item = (key, value) => ({ key, label: label(key), value: text(value) })
const grossPrice = (r) => r.grossPrice ?? r.price
const grossAmount = (r) => Math.round((Number(r.amount) + Number(r.discountShare || 0)) * 100) / 100

/** 组装打印单据（规范化后的结构，与 Web 端 toPrintDoc 的结果一致） */
function buildDoc(kind, d, dict) {
  const c = KINDS[kind]
  const discount = Number(d.discountAmount) || 0
  const grossTotal = Math.round((Number(d.totalAmount || 0) + discount) * 100) / 100
  const lineWarehouse = (d.items || []).some((i) => i.warehouseId)
  const advance = Number(d.freight) && kind !== 'purchaseIn' && d.freightBearer && d.freightBearer !== FreightBearer.COMPANY // 往来单位代垫运费
  const fields = [
    item(N.orderNo, d.orderSource === OrderSource.DIRECT ? `${d.orderNo || ''} (${t('trade.print.direct')})` : d.orderNo),
    item(c.partner, d.partnerName),
    item(N.warehouse, d.warehouseName),
    item(N.bizDate, d.bizDate),
    item(N.wmsDocNo, d.wmsDocNo),
    ...(d.invoiceKind ? [item(N.invoiceKind, dictText(dict.scm_invoice_kind, d.invoiceKind))] : []),
    ...(discount ? [item(N.grossTotal, `¥${money(grossTotal)}`), item(N.wholeDiscount, `¥${money(discount)}`)] : []),
    item(discount ? N.discountedTotal : N.totalAmount, `¥${money(d.totalAmount)}`),
    ...(Number(d.freight) ? [item(N.freight, `¥${money(d.freight)}`), item(N.freightBearer, dictText(dict.scm_freight_bearer, d.freightBearer))] : []),
    ...(advance ? [item(N.advanceTotal, `¥${money(Number(d.totalAmount) + Number(d.freight))}`)] : [])
  ].map((f) => ({ ...f, wide: false }))
  const cols = [
    [N.index, (r, i) => i + 1],
    [N.materialCode, (r) => r.materialCode],
    [N.materialName, (r) => r.materialName],
    [N.spec, (r) => r.spec],
    [N.unit, (r) => r.unit],
    ...(lineWarehouse ? [[N.warehouse, (r) => r.warehouseName]] : []),
    [N.location, (r) => r.locationCode || t('wms.common.unspecified')],
    [N.batch, (r) => r.batchNo],
    [N.quantity, (r) => qty(r.qty), true],
    [N.taxPrice, (r) => price(grossPrice(r)), true],
    [N.taxRate, (r) => `${Number(r.taxRate)}%`, true],
    [N.totalAmount, (r) => money(grossAmount(r)), true],
    [N.serialNo, (r) => r.serialNos]
  ]
  return {
    title: label(c.title),
    no: text(d.docNo),
    companyName: userState.tenantName || label(zhPrint.common.defaultCompany),
    note: text(d.remark),
    slip: false,
    fields,
    columns: cols.map(([key, , right]) => ({ key, title: label(key), right: !!right })),
    rows: (d.items || []).map((r, i) => Object.fromEntries(cols.map(([key, fn]) => [key, text(fn(r, i))]))),
    totals: { [N.totalAmount]: money(grossTotal) },
    signs: [item(N.prepared, d.createByName), item(N.storekeeper, null), item(c.sign, null)],
    sections: []
  }
}

// ---------------------------------------------------------------- 打印模板（与 Web 端 utils/printTemplates.js 一致的精简版）

/** 纸张（mm，纵向宽 × 高） */
const PAPERS = { A4: [210, 297], A5: [148, 210], B5: [176, 250], '241x280': [241, 280], '241x140': [241, 140], '241x93': [241, 93] }

function layoutOf(tpl = {}) {
  let [width, height] = tpl.paper === 'custom' ? [Number(tpl.paperWidth) || 210, Number(tpl.paperHeight) || 297] : PAPERS[tpl.paper] || PAPERS.A4
  if (tpl.orientation === 'landscape') [width, height] = [height, width]
  return { width, height, margin: tpl.margin ?? 10, fontSize: tpl.fontSize || 10 }
}

/** 按配置隐藏、排序、改名（未配置的放末尾），条目按 key 匹配配置中的名称 */
function arrange(items, settings, apply) {
  if (!settings?.length) return items
  const byKey = new Map(items.map((it) => [it.key, it]))
  const result = []
  const used = new Set()
  for (const s of settings) {
    const name = s.label ?? s.title
    const it = byKey.get(name)
    if (!it || used.has(name)) continue
    used.add(name)
    if (!s.hidden) result.push(apply(it, s))
  }
  for (const it of items) if (!used.has(it.key)) result.push(it)
  return result
}

function applyTemplate(doc, tpl, common) {
  const result = { ...doc }
  if (common?.companyName) result.companyName = common.companyName
  if (!tpl || tpl.jrxml) return result
  if (tpl.title) result.title = tpl.title
  if (tpl.showCompany === false) result.companyName = ''
  if (tpl.showNote === false) result.note = undefined
  result.fields = arrange(doc.fields, tpl.fields, (f, s) => (s.alias ? { ...f, label: s.alias } : f))
  result.columns = arrange(doc.columns, tpl.columns, (c, s) => ({ ...c, ...(s.alias ? { title: s.alias } : {}), ...(s.width ? { width: s.width } : {}) }))
  result.signs = arrange(doc.signs, tpl.signs, (it, s) => (s.alias ? { ...it, label: s.alias } : it))
  return result
}

// ---------------------------------------------------------------- 生成并打开 PDF

function requestPdf(body) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}/system/print/pdf`,
      method: 'POST',
      data: body,
      header: { Authorization: `Bearer ${getToken()}`, 'Accept-Language': locale.value },
      responseType: 'arraybuffer',
      timeout: 60000,
      success: (r) => (r.statusCode === 200 ? resolve(r.data) : reject(new Error(String(r.statusCode)))),
      fail: reject
    })
  })
}

function openPdf(buffer, fileName) {
  // #ifdef H5
  window.open(URL.createObjectURL(new Blob([buffer], { type: 'application/pdf' })))
  return Promise.resolve()
  // #endif
  // #ifndef H5
  return new Promise((resolve, reject) => {
    const path = `${wx.env.USER_DATA_PATH}/${fileName.replace(/[\\/:*?"<>|\s]+/g, '_')}.pdf`
    uni.getFileSystemManager().writeFile({
      filePath: path,
      data: buffer,
      success: () => uni.openDocument({ filePath: path, fileType: 'pdf', showMenu: true, success: resolve, fail: reject }),
      fail: reject
    })
  })
  // #endif
}

/** 生成并打开出入库单的 PDF（打印 / 分享） */
export async function printTradeDoc(kind, id) {
  const c = KINDS[kind]
  const dict = useDict('scm_invoice_kind', 'scm_freight_bearer')
  uni.showLoading({ title: t('trade.print.generating'), mask: true })
  try {
    const [d, templates] = await Promise.all([getTradeDoc(kind, id), printTemplates().catch(() => ({}))])
    const tpl = templates?.[c.type]
    const doc = applyTemplate(buildDoc(kind, d, dict), tpl, templates?._common)
    const overlay = tpl && !tpl.jrxml && tpl.overlay?.enabled ? tpl.overlay : undefined
    const title = `${label(c.title)} ${d.docNo}`
    const buffer = await requestPdf({ type: c.type, title, layout: layoutOf(tpl || {}), docs: [doc], overlay })
    uni.hideLoading()
    await openPdf(buffer, title)
  } catch (e) {
    uni.hideLoading()
    if (!e?.handled) uni.showToast({ title: t('trade.print.failed'), icon: 'none' })
  }
}

/** 保存 / 审核成功后询问是否打印（没有查看单据的权限时不询问） */
export function askPrint(kind, id) {
  if (!id || !canPrint(kind)) return
  uni.showModal({
    title: t('common.tip'),
    content: t('trade.print.ask'),
    confirmText: t('trade.print.print'),
    cancelText: t('trade.print.later'),
    success: (r) => r.confirm && printTradeDoc(kind, id)
  })
}
