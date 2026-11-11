import { t } from '@/locales'
import { runAction } from '@/composables/useAction'
import { bizRule } from '@/store/bizRules'
import { hasPerm } from '@/store/user'
import { addTradeDoc, addTradeDraft } from '@/api/trade'
import { askPrint } from './print'

/**
 * 手机开单的保存方式（与 Web 端出入库单表单一致，#75 草稿与审核）：
 * - 业务参数「进销存单据先存草稿」（scm.tradeDoc.draft）关闭：只有「保存」，保存即审核（出入库、生成应收应付）
 * - 开启且有审核权限（scm:{kind}:post）：「存草稿」+「保存并审核」
 * - 开启但没有审核权限：只有「存草稿」，由有审核权限的人在「单据草稿」或 Web 端审核
 * 调用前页面须已 loadBizRules(['scm.tradeDoc.draft'])
 *
 * @returns [{ mode: 'draft' | 'post', label, ghost }]，最后一个为主按钮
 */
export function saveModes(kind) {
  if (bizRule('scm.tradeDoc.draft', false) !== true) return [{ mode: 'post', label: t('trade.save.save') }]
  const modes = [{ mode: 'draft', label: t('trade.save.draft'), ghost: true }]
  if (hasPerm(`scm:${kind}:post`)) modes.push({ mode: 'post', label: t('trade.save.post') })
  else modes[0].ghost = false
  return modes
}

/**
 * 保存出入库单或草稿；审核成功后询问是否打印 / 分享 PDF
 *
 * @param build 返回请求体的函数（在确认后、调用接口前执行，可以是异步的，如先推荐库位）
 * @returns 是否成功
 */
export async function saveTrade(kind, mode, build, { then } = {}) {
  let id = null
  const ok = await runAction(
    async () => {
      const body = await build()
      id = await (mode === 'draft' ? addTradeDraft : addTradeDoc)(kind, body)
    },
    { success: mode === 'draft' ? t('trade.save.draftSaved') : t('trade.save.posted'), then }
  )
  if (ok && mode === 'post') askPrint(kind, id)
  return ok
}
