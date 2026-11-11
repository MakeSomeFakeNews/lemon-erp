import { computed } from 'vue'
import { useBizRules } from '@/composables/useBizRules'
import { useDict } from '@/composables/useDict'

/** 业务参数「默认税率」（供应链，前端可读） */
export const DEFAULT_TAX_RATE_KEY = 'scm.defaultTaxRate'
/** 业务字典「发票类型」：取值的扩展值（extValue）为税率（%） */
export const INVOICE_KIND_DICT = 'scm_invoice_kind'
/** 与后端业务参数的默认值一致，只在参数读取完成前、读取失败时使用 */
const RULE_DEFAULT = '13'

/** 发票类型字典项的税率，没有设置时为 undefined */
export const kindRate = (d) => (d && d.extValue != null && d.extValue !== '' ? Number(d.extValue) : undefined)

/**
 * 明细税率的默认值与发票类型（issue #82，与后端 ScmTaxRates 一致）：
 *
 *   const tax = useTaxRates()
 *   tax.defaultRate.value          // 业务参数「默认税率」
 *   tax.rateOf(form.invoiceKind)   // 发票类型的税率，没有时 undefined
 *   tax.fallback(form.invoiceKind) // 新明细的税率：发票类型的税率 → 默认税率
 *   tax.sourceRate(kind, p.taxRate) // 价格来源带出的税率：选了发票类型时以发票类型为准
 */
export function useTaxRates() {
  const { rules, ready } = useBizRules({ [DEFAULT_TAX_RATE_KEY]: RULE_DEFAULT })
  const dict = useDict(INVOICE_KIND_DICT)
  const kinds = computed(() => dict[INVOICE_KIND_DICT] || [])
  const defaultRate = computed(() => {
    const n = Number(rules[DEFAULT_TAX_RATE_KEY])
    return Number.isFinite(n) ? n : Number(RULE_DEFAULT)
  })
  const rateOf = (kind) => (kind ? kindRate(kinds.value.find((d) => d.value === kind)) : undefined)
  const fallback = (kind) => rateOf(kind) ?? defaultRate.value
  /** 物料、价目表等带出的税率：单据选了发票类型时统一按发票类型，否则用带出的税率（没有时保持原值，返回 undefined） */
  const sourceRate = (kind, rate) => rateOf(kind) ?? (rate != null ? Number(rate) : undefined)
  return { kinds, defaultRate, rateOf, fallback, sourceRate, ready }
}

/** 批量设置明细税率（赠品行除外），返回调整的行数 */
export function applyTaxRate(rows, rate) {
  let n = 0
  rows.forEach((r) => {
    if (!r.gift) {
      r.taxRate = rate
      n++
    }
  })
  return n
}
