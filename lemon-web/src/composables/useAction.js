import { Modal, message } from 'ant-design-vue'
import { t } from '@/locales'

const call = (v, args) => (typeof v === 'function' ? v(...args) : v)

/**
 * 单条操作的通用流程：（可选确认）→ 调接口 → 提示成功 → 刷新。
 * 返回一个函数，参数原样传给 api，可直接用在模板事件或 a-popconfirm 的 @confirm 上：
 *
 *   const remove = useAction(deleteProject, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
 *   <a-popconfirm @confirm="remove(record.id)">
 *
 *   const close = useAction((d) => closeWo(d.id), {
 *     confirm: (d) => ({ title: t('mes.wo.closeConfirm'), content: t('mes.wo.closeContent') }),
 *     successKey: 'mes.wo.closed',
 *     reload: () => refreshDetail()
 *   })
 *
 * 失败时错误提示由请求拦截器统一处理，函数返回的 Promise 会 reject（有确认框时不 reject，确认框保持打开由 Modal 处理）。
 *
 * @param {Function} api  (...args) => Promise，调用的接口
 * @param {object}   [options]
 * @param {string|object|Function} [options.confirm] 先弹出确认框：标题文字、Modal.confirm 的参数，或 (...args) => 前两者（返回空时不确认）
 * @param {string|Function|false}  [options.successKey='common.msg.operationSuccess'] 成功提示的消息键，或 (结果, ...args) => 提示文字（返回空不提示）；false 不提示
 * @param {Function} [options.reload] (结果, ...args) => 成功后执行，一般为刷新列表
 * @param {import('vue').Ref<boolean>} [options.loading] 调用期间置为 true 的状态
 */
export function useAction(api, options = {}) {
  const { confirm, successKey = 'common.msg.operationSuccess', reload, loading } = options

  async function execute(args) {
    if (loading) loading.value = true
    try {
      const res = await api(...args)
      if (successKey) {
        const text = typeof successKey === 'function' ? successKey(res, ...args) : t(successKey)
        if (text) message.success(text)
      }
      await reload?.(res, ...args)
      return res
    } finally {
      if (loading) loading.value = false
    }
  }

  return function run(...args) {
    const c = call(confirm, args)
    if (!c) return execute(args)
    return new Promise((resolve) => {
      Modal.confirm({
        ...(typeof c === 'string' ? { title: c } : c),
        onOk: () => execute(args).then(resolve),
        onCancel: () => resolve(undefined)
      })
    })
  }
}
