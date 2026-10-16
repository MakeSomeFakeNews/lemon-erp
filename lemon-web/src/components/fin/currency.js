import { ref } from 'vue'
import { currencies, rateOf } from '@/api/fin'
import { cached } from '@/utils/cache'
import { formatMoney } from '@/components/scm/scm'

/** 本位币 */
export const BASE_CURRENCY = 'CNY'

/** 是否外币（空视为本位币） */
export const isForeign = (code) => !!code && code !== BASE_CURRENCY

const FALLBACK_SYMBOLS = { CNY: '¥', USD: '$', EUR: '€', GBP: '£', JPY: 'JP¥', HKD: 'HK$' }
const list = ref([])

/** 币种列表（全局共享，短时缓存），返回 ref */
export function useCurrencies(force) {
  const loader = force ? currencies() : cached('fin-currencies', currencies, 60000)
  loader.then((r) => (list.value = r || [])).catch(() => {})
  return list
}

/** 币种符号，未维护时用常见符号或币种编码 */
export const currencySymbol = (code) => {
  const c = code || BASE_CURRENCY
  return list.value.find((x) => x.code === c)?.symbol || FALLBACK_SYMBOLS[c] || `${c} `
}

/** 带币种符号的金额：$1,234.00 */
export const moneyWith = (code, n) => `${currencySymbol(code)}${formatMoney(n)}`

/** 原币描述：USD 100.00 @7.0 */
export const fcText = (code, amount, rate) => `${code} ${formatMoney(amount)}${rate != null ? ` @${Number(rate)}` : ''}`

/** 启用的币种下拉选项 */
export const currencyOptions = (items) =>
  items.filter((c) => c.status !== 0).map((c) => ({ value: c.code, label: `${c.code} ${c.name || ''}`.trim() }))

/** 某日的记账汇率，未维护或本位币时返回 null / 1 */
export async function fetchRate(currency, date) {
  if (!isForeign(currency)) return 1
  if (!date) return null
  const r = await rateOf(currency, date).catch(() => null)
  return r == null ? null : Number(r)
}
