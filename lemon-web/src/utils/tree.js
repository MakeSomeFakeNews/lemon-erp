/** 收集树中所有节点 id */
export function allIds(tree, result = []) {
  for (const node of tree) {
    result.push(node.id)
    if (node.children?.length) allIds(node.children, result)
  }
  return result
}

/** 在树中查找节点 */
export function findNode(tree, id) {
  for (const node of tree) {
    if (node.id === id) return node
    const found = node.children?.length ? findNode(node.children, id) : null
    if (found) return found
  }
  return null
}

/** 节点自身及全部子孙节点的 id */
export function selfAndDescendantIds(tree, id) {
  const node = findNode(tree, id)
  return node ? [node.id, ...allIds(node.children || [])] : []
}

/** id → 父节点 id（根节点为 null） */
export function parentMap(tree, parentId = null, map = new Map()) {
  for (const node of tree) {
    map.set(node.id, parentId)
    if (node.children?.length) parentMap(node.children, node.id, map)
  }
  return map
}

/** 节点的全部祖先 id，由近及远 */
export function ancestorIds(parents, id) {
  const result = []
  for (let p = parents.get(id); p != null; p = parents.get(p)) result.push(p)
  return result
}

/**
 * 过滤树：命中的节点保留其祖先；返回新树，不修改原数据
 * @param {(node) => boolean} predicate
 */
export function filterTree(tree, predicate) {
  return tree
    .map((node) => {
      const children = node.children?.length ? filterTree(node.children, predicate) : []
      if (predicate(node) || children.length) return { ...node, children: children.length ? children : undefined }
      return null
    })
    .filter(Boolean)
}

/** 逐个节点转换，保持树结构；mapper 的返回值不需要处理 children */
export function mapTree(tree, mapper, parent = null) {
  return tree.map((node) => {
    const mapped = mapper(node, parent)
    return {
      ...mapped,
      children: node.children?.length ? mapTree(node.children, mapper, mapped) : undefined
    }
  })
}

/** 从根到节点的名称路径，如「柠檬科技 / 研发部 / 前端组」 */
export function namePath(tree, id, sep = ' / ') {
  const walk = (nodes, path) => {
    for (const n of nodes) {
      const next = [...path, n.name]
      if (n.id === id) return next
      const found = n.children?.length ? walk(n.children, next) : null
      if (found) return found
    }
    return null
  }
  return walk(tree, [])?.join(sep) ?? ''
}
