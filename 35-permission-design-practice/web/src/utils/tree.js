/**
 * 把后端返回的平铺列表（含 parentId）转成树。
 * 只有存在子节点时才挂 children 字段——el-table 的树形数据
 * 遇到 children: null 会当作有子节点渲染展开箭头。
 */
export function buildTree(list, idKey = 'id', parentKey = 'parentId') {
  const map = new Map()
  list.forEach((item) => map.set(item[idKey], { ...item }))
  const roots = []
  map.forEach((node) => {
    const pid = node[parentKey]
    if (pid === null || pid === undefined || pid === 0 || !map.has(pid)) {
      roots.push(node)
    } else {
      const parent = map.get(pid)
      if (!parent.children) parent.children = []
      parent.children.push(node)
    }
  })
  // 简单按 sortOrder 排序（若存在）
  const sortRec = (arr) => {
    arr.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    arr.forEach((n) => n.children && sortRec(n.children))
  }
  sortRec(roots)
  return roots
}
