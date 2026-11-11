import { computed, ref } from 'vue'
import { listMaterialCategories } from '@/api/wms/materialCategory'
import { tt } from '@/locales'
import { useDictStore } from '@/store/dict'
import { STATUS_ENABLED } from '@/utils/constants'

/**
 * 物料分类（多级）：全部页面共用一份缓存（登录期间只请求一次，维护分类后调用 reload）。
 * 分类按编码关联（物料的 category 等存分类编码），名称按界面语言翻译（内置分类见 locales/<语言>/dict.js 的 wms_material_category）。
 *
 *   const { tree, pathOf } = useMaterialCategories()
 *   tree：[{ id, code, name, dataName, path, status, usable, children }]（name 为译文、dataName 为数据库中的名称；usable：自身和上级都启用，可被新物料选用）
 *   pathOf(code)：完整路径，如「电子元器件 / 电阻」
 * 只需要显示名称时也可以继续用字典：<DictTag type="wms_material_category" :value="code" />（字典由后端按分类提供，显示完整路径）
 */
const list = ref([])
const loading = ref(false)
let pending = null

function load(force = false) {
  if (pending && !force) return pending
  loading.value = true
  pending = listMaterialCategories()
    .then((rows) => {
      list.value = rows || []
      return list.value
    })
    .catch((e) => {
      pending = null
      throw e
    })
    .finally(() => (loading.value = false))
  return pending
}

const nameOf = (c) => tt(`dict.wms_material_category.${c.code}`, c.name)

const tree = computed(() => {
  const byParent = new Map()
  list.value.forEach((c) => {
    const key = c.parentId || 0
    if (!byParent.has(key)) byParent.set(key, [])
    byParent.get(key).push(c)
  })
  const ids = new Set(list.value.map((c) => c.id))
  const build = (parentId, parentPath, parentUsable) =>
    (byParent.get(parentId) || []).map((c) => {
      const name = nameOf(c)
      const path = parentPath ? `${parentPath} / ${name}` : name
      const usable = parentUsable && c.status === STATUS_ENABLED
      const children = build(c.id, path, usable)
      return { ...c, dataName: c.name, name, path, usable, children: children.length ? children : undefined }
    })
  // 上级不存在（数据异常）的作为一级分类
  const roots = list.value.filter((c) => !c.parentId || !ids.has(c.parentId)).map((c) => c.parentId || 0)
  return [...new Set([0, ...roots])].flatMap((p) => build(p, '', true))
})

/** 编码 → 节点 */
const byCode = computed(() => {
  const map = new Map()
  const walk = (nodes) =>
    nodes.forEach((n) => {
      map.set(n.code, n)
      if (n.children) walk(n.children)
    })
  walk(tree.value)
  return map
})

/** 退出登录时清空（换账号、租户后重新读取） */
export function clearMaterialCategories() {
  pending = null
  list.value = []
}

export function useMaterialCategories() {
  load().catch(() => {})
  return {
    list,
    tree,
    loading,
    /** 完整路径，编码不存在时返回编码本身 */
    pathOf: (code) => (code ? byCode.value.get(code)?.path || code : ''),
    nodeOf: (code) => byCode.value.get(code),
    /** 分类有变化后重新加载（同时清理字典缓存中的物料分类） */
    reload: () => {
      useDictStore().clear('wms_material_category')
      return load(true)
    }
  }
}
