import { t } from '@/locales'
import { cancelConfirmIo, confirmIo } from '@/api/fin'
import { useAction } from '@/composables/useAction'
import { formatMoney } from '@/components/fin/fin'

/** 收支单的确认、取消确认（列表和详情共用） */
export function useIoActions(reload) {
  const confirm = useAction((r) => confirmIo(r.id), {
    confirm: (r) => ({ title: t('fin.io.confirmTitle', { no: r.docNo, amount: formatMoney(r.amount) }), content: t('fin.io.confirmTip') }),
    successKey: 'fin.io.confirmed',
    reload: () => reload()
  })
  const cancelConfirm = useAction((r) => cancelConfirmIo(r.id), {
    confirm: (r) => ({ title: t('fin.io.cancelTitle', { no: r.docNo }), content: t('fin.io.cancelTip') }),
    successKey: 'fin.io.cancelled',
    reload: () => reload()
  })
  return { confirm, cancelConfirm }
}
