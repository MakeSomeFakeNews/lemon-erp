import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { t } from '@/locales'

/** 提交时默认去掉的只读字段 */
const READONLY_FIELDS = ['createTime', 'updateTime', 'children']

const omit = (obj, keys) => Object.fromEntries(Object.entries(obj).filter(([k]) => !keys.includes(k)))

/**
 * 新增 / 编辑抽屉表单的通用逻辑，配合 FormDrawer 组件使用：
 * - openCreate / openEdit 打开表单，编辑时可通过 fetch 加载详情
 * - submit 校验并保存，根据是否有 id 调用 create / update
 * - 有未保存的修改时关闭会二次确认；Ctrl / ⌘ + Enter 快捷保存
 *
 * @param {object}   options
 * @param {Function} options.emptyForm  返回空表单
 * @param {Function} [options.beforeOpen] (record | null) => Promise，打开前加载下拉选项等依赖数据
 * @param {Function} [options.fetch]    (record) => Promise<详情>，不传则直接使用表格行数据
 * @param {Function} options.create     (payload) => Promise
 * @param {Function} options.update     (id, payload) => Promise
 * @param {Function} [options.toPayload] (form) => 提交数据，默认去掉 id 和只读字段
 * @param {Function} [options.onSuccess] 保存成功后回调，一般用于刷新列表
 */
export function useFormDrawer(options) {
  const { emptyForm, beforeOpen, fetch, create, update, toPayload, onSuccess, successMessage = t('common.msg.saveSuccess') } = options

  const open = ref(false)
  const loading = ref(false)
  const saving = ref(false)
  const formRef = ref()
  const form = ref(emptyForm())
  const snapshot = ref('')

  const isEdit = computed(() => form.value.id != null)
  const dirty = computed(() => open.value && !loading.value && JSON.stringify(form.value) !== snapshot.value)

  const markClean = () => (snapshot.value = JSON.stringify(form.value))

  /** 打开抽屉并在 loading 状态下准备表单数据；失败时关闭抽屉（错误提示由请求拦截器统一处理） */
  async function show(initial, prepare) {
    form.value = initial
    open.value = true
    loading.value = true
    try {
      await prepare()
    } catch (e) {
      open.value = false
      throw e
    } finally {
      loading.value = false
    }
    markClean()
  }

  function openCreate(defaults = {}) {
    return show({ ...emptyForm(), ...defaults }, async () => {
      await beforeOpen?.(null)
    })
  }

  function openEdit(record) {
    return show({ ...emptyForm(), id: record.id }, async () => {
      const [detail] = await Promise.all([fetch ? fetch(record) : record, beforeOpen?.(record)])
      form.value = { ...emptyForm(), ...omit(detail, READONLY_FIELDS) }
    })
  }

  async function submit() {
    if (saving.value || loading.value) return
    try {
      await formRef.value?.validate()
    } catch {
      return // 校验失败，表单项会显示错误信息
    }
    saving.value = true
    try {
      const { id, ...rest } = form.value
      const payload = toPayload ? toPayload(form.value) : omit(rest, READONLY_FIELDS)
      id != null ? await update(id, payload) : await create(payload)
      message.success(successMessage)
      open.value = false
      onSuccess?.()
    } finally {
      saving.value = false
    }
  }

  function close() {
    if (!dirty.value) {
      open.value = false
      return
    }
    Modal.confirm({
      title: t('system.role.discardTitle'),
      content: t('components.formDrawer.discardContent'),
      okText: t('common.action.discard'),
      okType: 'danger',
      cancelText: t('system.role.keepEditing'),
      onOk: () => (open.value = false)
    })
  }

  function onKeydown(e) {
    if (open.value && e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
      e.preventDefault()
      submit()
    }
  }
  onMounted(() => window.addEventListener('keydown', onKeydown))
  onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))

  return { open, loading, saving, formRef, form, isEdit, dirty, openCreate, openEdit, submit, close }
}
