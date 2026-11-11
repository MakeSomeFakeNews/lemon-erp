<template>
  <div class="mc-tree">
    <div class="workspace-side-head">
      <span>{{ $t('wms.materialCategory.title') }}</span>
      <span class="mc-head-actions">
        <a-tooltip v-if="canEdit" :title="$t('wms.materialCategory.addTop')">
          <a-button type="text" size="small" @click="openForm({ parentId: 0 })"><template #icon><PlusOutlined /></template></a-button>
        </a-tooltip>
        <a-tooltip :title="expandedKeys.length ? $t('system.menu.collapseAll') : $t('system.menu.expandAll')">
          <a-button type="text" size="small" @click="toggleExpand">
            <template #icon><NodeCollapseOutlined v-if="expandedKeys.length" /><NodeExpandOutlined v-else /></template>
          </a-button>
        </a-tooltip>
      </span>
    </div>
    <a-input v-model:value="keyword" :placeholder="$t('wms.materialCategory.search')" allow-clear>
      <template #prefix><SearchOutlined /></template>
    </a-input>
    <div class="mc-all" :class="{ active: !modelValue }" @click="select(undefined)">
      <AppstoreOutlined />
      <span class="mc-name">{{ $t('wms.materialCategory.all') }}</span>
    </div>
    <div class="mc-body">
      <a-spin :spinning="loading">
        <a-tree
          v-if="treeData.length"
          v-model:expanded-keys="expandedKeys"
          :tree-data="treeData"
          :field-names="{ title: 'name', key: 'id', children: 'children' }"
          :selected-keys="selectedId ? [selectedId] : []"
          :draggable="canEdit && !keyword ? { icon: false } : false"
          block-node
          @select="(keys, e) => select(e.node.dataRef.code)"
          @drop="onDrop"
        >
          <template #title="node">
            <div class="mc-node">
              <HighlightText :text="node.name" :keyword="keyword" :title="node.path" :class="['mc-name', { 'is-disabled': !node.usable }]" />
              <a-tag v-if="node.status === STATUS_DISABLED" :bordered="false" class="mc-tag">{{ $t('common.status.disabled') }}</a-tag>
              <span class="mc-count">{{ totals.get(node.code) || 0 }}</span>
              <span v-if="canEdit" class="mc-actions" @click.stop>
                <a-dropdown :trigger="['click']">
                  <a-button type="text" size="small" class="mc-more"><template #icon><MoreOutlined /></template></a-button>
                  <template #overlay>
                    <a-menu @click="({ key }) => onMenu(key, node.dataRef)">
                      <a-menu-item key="add" :disabled="depthOf(node.dataRef) >= MAX_DEPTH"><PlusOutlined /> {{ $t('wms.materialCategory.addChild') }}</a-menu-item>
                      <a-menu-item key="edit"><EditOutlined /> {{ $t('common.action.edit') }}</a-menu-item>
                      <a-menu-item key="delete" danger><DeleteOutlined /> {{ $t('common.action.delete') }}</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </span>
            </div>
          </template>
        </a-tree>
        <a-empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" :description="keyword ? $t('wms.materialCategory.noMatch') : $t('wms.materialCategory.empty')" />
      </a-spin>
    </div>
    <div v-if="canEdit && treeData.length && !keyword" class="mc-tip sub-text">{{ $t('wms.materialCategory.dragTip') }}</div>

    <a-modal v-model:open="formOpen" :title="form.id ? $t('wms.materialCategory.editTitle') : $t('wms.materialCategory.addTitle')" :confirm-loading="saving" @ok="save">
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ style: { width: '88px' } }" class="mc-form">
        <a-form-item :label="$t('wms.materialCategory.parent')" name="parentId">
          <a-tree-select v-model:value="form.parentId" :tree-data="parentOptions" :field-names="{ label: 'name', value: 'id', children: 'children' }"
            tree-node-label-prop="path" tree-default-expand-all :placeholder="$t('wms.materialCategory.topLevel')" />
        </a-form-item>
        <a-form-item :label="$t('wms.materialCategory.code')" name="code" :extra="form.id ? $t('wms.materialCategory.codeFixed') : $t('wms.materialCategory.codeTip')">
          <a-input v-model:value="form.code" :disabled="!!form.id" :maxlength="16" :placeholder="form.id ? '' : $t('wms.materialCategory.codeAuto')" />
        </a-form-item>
        <a-form-item :label="$t('wms.materialCategory.name')" name="name">
          <a-input v-model:value="form.name" :maxlength="64" />
        </a-form-item>
        <a-form-item :label="$t('common.field.sort')" name="sort">
          <a-input-number v-model:value="form.sort" :min="0" :precision="0" :placeholder="$t('wms.materialCategory.sortAuto')" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status" :extra="$t('wms.materialCategory.disableTip')">
          <a-radio-group v-model:value="form.status" :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Empty, message } from 'ant-design-vue'
import {
  AppstoreOutlined,
  DeleteOutlined,
  EditOutlined,
  MoreOutlined,
  NodeCollapseOutlined,
  NodeExpandOutlined,
  PlusOutlined,
  SearchOutlined
} from '@ant-design/icons-vue'
import HighlightText from '@/components/tree/HighlightText.vue'
import { addMaterialCategory, deleteMaterialCategory, materialCategoryCounts, sortMaterialCategories, updateMaterialCategory } from '@/api/wms/materialCategory'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { useMaterialCategories } from '@/composables/useMaterialCategories'
import { useUserStore } from '@/store/user'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'
import { toOptions } from '@/utils/dict'
import { allIds, filterTree, findNode, mapTree } from '@/utils/tree'

/**
 * 物料管理页左侧的物料分类树：v-model 为选中的分类编码（为空是全部），点击分类列出该分类及其下级分类的物料；
 * 有「物料分类维护」权限（wms:material:category）时可新增、编辑、删除、拖动排序（拖到其他分类上改为其下级）。
 * 节点右侧的数字是该分类及其下级分类的物料数；物料有增删改后调用暴露的 refresh() 更新
 */
const MAX_DEPTH = 5

const props = defineProps({ modelValue: { type: String, default: undefined } })
const emit = defineEmits(['update:modelValue', 'change'])
const { t } = useI18n()
const dict = useDict('sys_common_status')
const canEdit = computed(() => useUserStore().hasPerm('wms:material:category'))

const { tree, loading, reload } = useMaterialCategories()
const keyword = ref('')
const expandedKeys = ref([])
const counts = ref({})

const treeData = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return kw ? filterTree(tree.value, (n) => n.name.toLowerCase().includes(kw) || n.code.toLowerCase().includes(kw)) : tree.value
})
const selectedId = computed(() => (props.modelValue ? findByCode(tree.value, props.modelValue)?.id : undefined))

/** 分类编码 → 自身及下级分类的物料数 */
const totals = computed(() => {
  const map = new Map()
  const sum = (n) => {
    const total = (counts.value[n.code] || 0) + (n.children || []).reduce((s, c) => s + sum(c), 0)
    map.set(n.code, total)
    return total
  }
  tree.value.forEach(sum)
  return map
})

let expandedOnce = false
watch(
  tree,
  (data) => {
    // 首次加载展开一级分类，之后保持用户的展开状态
    if (!expandedOnce && data.length) {
      expandedOnce = true
      expandedKeys.value = data.filter((n) => n.children).map((n) => n.id)
    }
  },
  { immediate: true }
)
watch(keyword, (kw) => {
  if (kw) expandedKeys.value = allIds(treeData.value)
})

function findByCode(nodes, code) {
  for (const n of nodes) {
    if (n.code === code) return n
    const found = n.children ? findByCode(n.children, code) : null
    if (found) return found
  }
  return null
}

function depthOf(node) {
  let depth = 1
  for (let p = findNode(tree.value, node.parentId); p; p = findNode(tree.value, p.parentId)) depth++
  return depth
}

function toggleExpand() {
  expandedKeys.value = expandedKeys.value.length ? [] : allIds(treeData.value)
}

function select(code) {
  // 再次点击已选中的分类时保持选中
  if (code === props.modelValue || (code === undefined && !props.modelValue)) return
  emit('update:modelValue', code)
  emit('change', code)
}

async function refresh() {
  counts.value = await materialCategoryCounts().catch(() => ({}))
}
onMounted(refresh)

// ---------------------------------------------------------------- 维护

const formOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
  code: [{ pattern: /^[A-Za-z0-9_-]{0,16}$/, message: t('wms.materialCategory.codeRule') }],
  name: [{ required: true, whitespace: true, message: t('wms.materialCategory.inputName') }]
}

/** 上级分类的选项：顶级 + 各分类（编辑时排除自身及下级） */
const parentOptions = computed(() => {
  const exclude = form.value.id
  const strip = (nodes) =>
    nodes.filter((n) => n.id !== exclude).map((n) => ({ ...n, children: n.children ? strip(n.children) : undefined }))
  const nodes = mapTree(strip(tree.value), (n) => ({ id: n.id, name: n.name, path: n.path }))
  return [{ id: 0, name: t('wms.materialCategory.topLevel'), path: t('wms.materialCategory.topLevel') }, ...nodes]
})

function openForm(init) {
  form.value = { id: undefined, parentId: 0, code: '', name: '', sort: null, status: STATUS_ENABLED, remark: '', ...init }
  formOpen.value = true
  formRef.value?.clearValidate()
}

function onMenu(key, node) {
  if (key === 'add') openForm({ parentId: node.id })
  else if (key === 'edit') {
    openForm({ id: node.id, parentId: node.parentId || 0, code: node.code, name: node.name, sort: node.sort, status: node.status, remark: node.remark || '' })
    // 内置分类显示的是译文，编辑时用数据库中的名称
    form.value.name = node.dataName ?? node.name
  } else if (key === 'delete') remove(node)
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const data = { ...form.value, code: form.value.code || undefined }
    if (form.value.id) await updateMaterialCategory(form.value.id, data)
    else await addMaterialCategory(data)
    message.success(t('common.msg.saveSuccess'))
    formOpen.value = false
    await reload()
  } finally {
    saving.value = false
  }
}

const remove = useAction((node) => deleteMaterialCategory(node.id), {
  confirm: (node) => t('wms.materialCategory.confirmDelete', { name: node.path }),
  successKey: 'common.msg.deleteSuccess',
  reload: (res, node) => {
    if (props.modelValue === node.code) select(undefined)
    return reload()
  }
})

/** 拖动：放到节点上成为其最后一个下级，放到节点之间按位置排序；提交新上级下的完整顺序 */
async function onDrop(info) {
  const drag = info.dragNode.dataRef
  const target = info.node.dataRef
  let parentId
  let siblings
  let index
  if (!info.dropToGap) {
    parentId = target.id
    siblings = (target.children || []).filter((n) => n.id !== drag.id)
    index = siblings.length
  } else {
    parentId = target.parentId || 0
    const parent = parentId ? findNode(tree.value, parentId) : null
    siblings = (parent ? parent.children || [] : tree.value).filter((n) => n.id !== drag.id)
    const pos = info.node.pos.split('-')
    const relative = info.dropPosition - Number(pos[pos.length - 1])
    index = siblings.findIndex((n) => n.id === target.id) + (relative === -1 ? 0 : 1)
  }
  const ids = siblings.map((n) => n.id)
  ids.splice(index, 0, drag.id)
  await sortMaterialCategories({ parentId, ids })
  message.success(t('common.msg.operationSuccess'))
  if (parentId && !expandedKeys.value.includes(parentId)) expandedKeys.value = [...expandedKeys.value, parentId]
  await reload()
}

defineExpose({ refresh })
</script>

<style scoped>
.mc-tree {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  min-height: 0;
}
.mc-head-actions {
  display: inline-flex;
  gap: 2px;
}
.mc-all {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}
.mc-all:hover {
  background: var(--app-hover);
}
.mc-all.active {
  color: var(--app-primary);
  background: var(--app-primary-bg);
}
.mc-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.mc-node {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}
.mc-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mc-tag {
  margin: 0;
  font-size: 11px;
  line-height: 16px;
  padding: 0 4px;
}
.mc-count {
  flex: none;
  color: var(--app-text-3);
  font-size: 12px;
}
.mc-actions {
  display: none;
  flex: none;
}
.mc-more {
  width: 20px;
  height: 20px;
  min-width: 20px;
}
:deep(.ant-tree-treenode:hover) .mc-actions,
:deep(.ant-tree-treenode-selected) .mc-actions {
  display: inline-flex;
}
.is-disabled {
  color: var(--app-text-3);
}
.mc-tip {
  font-size: 12px;
}
.mc-form {
  margin-top: 16px;
}
.full {
  width: 100%;
}
</style>
