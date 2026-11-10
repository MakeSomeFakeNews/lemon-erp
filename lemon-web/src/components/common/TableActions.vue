<template>
  <span class="table-actions icon-actions">
    <template v-for="(a, i) in layout.inline" :key="a.key">
      <a-divider v-if="i" type="vertical" class="table-actions-divider" />
      <slot v-if="a.slot" :name="a.slot" :action="a" :text="layout.text" />
      <TableActionButton v-else :action="a" :text="layout.text" />
    </template>
    <template v-if="layout.more.length">
      <a-divider v-if="layout.inline.length" type="vertical" class="table-actions-divider" />
      <a-dropdown placement="bottomRight">
        <a-button type="link" size="small" class="table-action-btn" :aria-label="$t('common.action.more')">
          <template #icon><MoreOutlined /></template>
        </a-button>
        <template #overlay>
          <a-menu @click="onMenu">
            <a-menu-item v-for="a in layout.more" :key="a.key" :danger="a.danger" :disabled="a.disabled">
              <a-tooltip :title="a.tip" placement="left">
                <span class="table-actions-menu-item">
                  <component :is="a.icon" v-if="a.icon" />
                  {{ a.label }}
                </span>
              </a-tooltip>
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </template>
  </span>
</template>

<script setup>
/**
 * 表格操作列（全站统一）：常用操作显示为图标按钮（悬停显示文字），图标之间用竖线分隔，不常用的操作放进「更多」（三个点）下拉；
 * 一行只有一个操作时显示「图标 + 文字」。配合 actionColumn(图标数, [单独出现时的文字]) 定义列（右侧固定、右对齐）。
 *
 *   <TableActions :actions="[
 *     { type: 'view', onClick: () => openDetail(record) },
 *     { type: 'edit', perm: 'scm:sales:edit', show: record.status === OrderStatus.DRAFT, onClick: () => form.openEdit(record) },
 *     { key: 'outbound', label: t('scm.order.outbound'), icon: ExportOutlined, onClick: () => openTrade(record) },
 *     { type: 'delete', perm: 'scm:sales:remove', confirm: t('common.msg.confirmDelete'), onClick: () => remove(record.id), more: true }
 *   ]" />
 *
 * 每个操作：
 * - type：常用操作（view / edit / delete / print / copy / audit / submit 等，见 tableActions.js 的 ACTION_TYPES），决定默认图标、文字、是否红色
 * - key：区分操作（默认 type）；label、icon：文字、图标（覆盖 type 的默认值）
 * - onClick：点击（有 confirm 时在确认后）执行
 * - perm：权限标识（字符串或数组，满足其一），没有权限时不显示；show：为 false 时不显示（按状态显示的操作）
 * - disabled、loading、danger；tip：额外的提示（如禁用原因），显示在悬停提示中
 * - confirm：二次确认，标题文字或 { title, description, okType }（直接显示的操作用气泡确认，「更多」中的用确认框）
 * - more：放进「更多」下拉
 * - slot：自定义渲染（需要包一层弹出确认等组件时），插槽参数 { action, text }，在插槽中用 <TableActionButton :action :text /> 渲染按钮；始终直接显示
 *
 * 不可见的操作不占位；直接显示的图标（含「更多」）最多 MAX_INLINE 个，超过时其余的移进「更多」。
 */
import { computed } from 'vue'
import { Modal } from 'ant-design-vue'
import { MoreOutlined } from '@ant-design/icons-vue'
import TableActionButton from './TableActionButton.vue'
import { ACTION_TYPES, MAX_INLINE, actionLabel, layoutActions } from './tableActions'
import { useUserStore } from '@/store/user'

const props = defineProps({
  actions: { type: Array, default: () => [] },
  max: { type: Number, default: MAX_INLINE }
})

const userStore = useUserStore()

const visible = computed(() =>
  props.actions
    .filter((a) => a && a.show !== false && (!a.perm || userStore.hasPerm(a.perm)))
    .map((a, i) => {
      const def = ACTION_TYPES[a.type] || {}
      return { ...a, key: a.key || a.type || `action-${i}`, label: actionLabel(a), icon: a.icon || def.icon, danger: a.danger ?? def.danger }
    })
)

const layout = computed(() => layoutActions(visible.value, props.max))

function onMenu({ key }) {
  const a = layout.value.more.find((x) => x.key === key)
  if (!a || a.disabled) return
  if (!a.confirm) return a.onClick?.()
  const c = typeof a.confirm === 'string' ? { title: a.confirm } : a.confirm
  Modal.confirm({ title: c.title, content: c.description, okType: c.okType || (a.danger ? 'danger' : 'primary'), onOk: () => a.onClick?.() })
}
</script>
