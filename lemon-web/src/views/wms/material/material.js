import { t } from '@/locales'
import { CostMethod, SupplyType } from '@/constants/enums'
import { formatQty, rowKey } from '@/components/wms/wms'
import { PRICE_FIELDS } from '@/components/wms/priceLevels'

/** 计划参数摘要（列表、详情共用） */
export const planRule = (p) =>
  [
    p.leadDays != null ? t('wms.material.rule.lead', { n: p.leadDays }) : null,
    p.minLot != null ? t('wms.material.rule.minLot', { n: formatQty(p.minLot) }) : null,
    p.lotMultiple != null ? t('wms.material.rule.multiple', { n: formatQty(p.lotMultiple) }) : null,
    Number(p.overIssueRate) ? t('wms.material.rule.overIssue', { n: formatQty(p.overIssueRate) }) : null,
    p.substituteCount ? t('wms.material.rule.substitutes', { n: p.substituteCount }) : null
  ].filter(Boolean)

/** 提前期：设置的，或按默认 BOM / 默认供应商 */
export const leadText = (p) => {
  if (p.leadDays != null) return t('wms.material.days', { n: p.leadDays })
  if (p.effectiveSupplyType === SupplyType.MAKE && p.bomLeadDays != null) return t('wms.material.daysBom', { n: p.bomLeadDays })
  if ([SupplyType.BUY, SupplyType.OUTSOURCE].includes(p.effectiveSupplyType) && p.supplierLeadDays != null) return t('wms.material.daysSupplier', { n: p.supplierLeadDays })
  return '-'
}

// ---------------------------------------------------------------- 表单与接口数据的转换

/** 提交到物料接口的字段 */
const MATERIAL_FIELDS = [
  'code',
  'name',
  'spec',
  'unit',
  'category',
  'brand',
  'isVirtual',
  ...PRICE_FIELDS,
  'batchEnabled',
  'serialEnabled',
  'iqcRequired',
  'safetyStock',
  'reorderPoint',
  'reorderQty',
  'barcode',
  'defaultWarehouseId',
  'defaultLocationId',
  'refCost',
  'costMethod',
  'remark'
]
const num = (v) => (v == null ? null : Number(v))

/** 新增物料的空表单（含生产计划属性、替代料） */
export const emptyMaterialForm = () => ({
  id: undefined,
  code: '',
  name: '',
  spec: '',
  unit: undefined,
  category: undefined,
  brand: undefined,
  isVirtual: 0,
  ...Object.fromEntries(PRICE_FIELDS.map((k) => [k, null])),
  batchEnabled: 0,
  serialEnabled: 0,
  iqcRequired: 0,
  safetyStock: null,
  reorderPoint: null,
  reorderQty: null,
  barcode: '',
  defaultWarehouseId: null,
  defaultLocationId: null,
  refCost: null,
  costMethod: CostMethod.WEIGHTED,
  remark: '',
  supplyType: '',
  leadDays: null,
  minLot: null,
  lotMultiple: null,
  overIssueRate: 0,
  fqcRequired: 0,
  planner: '',
  substitutes: [],
  copyFrom: undefined
})

/** 物料与计划属性（没有 mes:material:edit 时为空）→ 表单 */
export function toForm(m, p) {
  const form = { ...m, safetyStock: num(m.safetyStock), reorderPoint: num(m.reorderPoint), reorderQty: num(m.reorderQty), refCost: num(m.refCost), barcode: m.barcode || '' }
  PRICE_FIELDS.forEach((k) => (form[k] = num(m[k])))
  if (p) {
    Object.assign(form, {
      supplyType: p.supplyType || '',
      leadDays: p.leadDays,
      minLot: num(p.minLot),
      lotMultiple: num(p.lotMultiple),
      overIssueRate: Number(p.overIssueRate),
      fqcRequired: p.fqcRequired || 0,
      planner: p.planner || '',
      substitutes: p.substitutes.map((s) => ({
        key: rowKey(),
        substituteId: s.substituteId,
        label: `${s.substituteCode} ${s.substituteName}`,
        ratio: Number(s.ratio),
        priority: s.priority,
        remark: s.remark || ''
      }))
    })
  }
  return form
}

/** 表单 → { copyFrom, material, plan }；虚拟商品不采购、不生产，没有计划属性 */
export const toPayload = (f, canEditPlan) => ({
  copyFrom: f.copyFrom,
  material: {
    ...Object.fromEntries(MATERIAL_FIELDS.map((k) => [k, f[k]])),
    reorderPoint: f.reorderPoint ?? null,
    reorderQty: f.reorderQty ?? null,
    refCost: f.refCost ?? null,
    ...Object.fromEntries(PRICE_FIELDS.map((k) => [k, f[k] ?? null])),
    brand: f.brand || null,
    barcode: f.barcode?.trim() || null,
    defaultLocationId: f.defaultWarehouseId ? f.defaultLocationId || null : null
  },
  // 虚拟商品不采购、不生产，没有计划属性
  plan: canEditPlan && !f.isVirtual
    ? {
        supplyType: f.supplyType || null,
        leadDays: f.leadDays ?? null,
        minLot: f.minLot || null,
        lotMultiple: f.lotMultiple || null,
        overIssueRate: f.overIssueRate ?? 0,
        fqcRequired: f.fqcRequired || 0,
        planner: f.planner || null,
        substitutes:
          f.supplyType === SupplyType.PHANTOM
            ? []
            : f.substitutes.map(({ substituteId, ratio, priority, remark }) => ({ substituteId, ratio, priority, remark: remark || null }))
      }
    : null
})
