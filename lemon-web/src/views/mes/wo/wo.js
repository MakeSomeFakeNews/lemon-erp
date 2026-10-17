import { Modal } from 'ant-design-vue'
import { t } from '@/locales'


/** 完成百分比（0～100） */
export const pct = (a, b) => (Number(b) ? Math.min(Math.round((Number(a) / Number(b)) * 100), 100) : 0)

/** 新工单生成后询问是否打开 */
export function offerOpen(title, content, onOk) {
  Modal.confirm({ title, content, okText: t('mes.wo.openNew'), cancelText: t('mes.wo.stay'), onOk })
}
