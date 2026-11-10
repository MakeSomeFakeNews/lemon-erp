import {
  AuditOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  CopyOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  EyeOutlined,
  FileTextOutlined,
  HistoryOutlined,
  KeyOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  PrinterOutlined,
  RollbackOutlined,
  SendOutlined,
  SettingOutlined,
  StopOutlined,
  SwapOutlined,
  UndoOutlined,
  UnorderedListOutlined
} from '@ant-design/icons-vue'
import { t } from '@/locales'

/**
 * 操作列的常用操作：type → 图标、文字（消息键）、是否危险操作。
 * 页面写 { type: 'edit', onClick } 即可；文字、图标不同时用 label / icon 覆盖。
 */
export const ACTION_TYPES = {
  view: { icon: EyeOutlined, label: 'common.action.view' },
  detail: { icon: EyeOutlined, label: 'common.action.detail' },
  edit: { icon: EditOutlined, label: 'common.action.edit' },
  modify: { icon: EditOutlined, label: 'common.action.modify' },
  delete: { icon: DeleteOutlined, label: 'common.action.delete', danger: true },
  add: { icon: PlusOutlined, label: 'common.action.add' },
  copy: { icon: CopyOutlined, label: 'common.action.copy' },
  print: { icon: PrinterOutlined, label: 'common.action.print' },
  download: { icon: DownloadOutlined, label: 'common.action.download' },
  submit: { icon: SendOutlined, label: 'common.action.submit' },
  audit: { icon: AuditOutlined, label: 'common.action.audit' },
  unaudit: { icon: UndoOutlined, label: 'common.action.unaudit' },
  approve: { icon: CheckCircleOutlined, label: 'common.action.approve' },
  confirm: { icon: CheckCircleOutlined, label: 'common.action.confirm' },
  reject: { icon: CloseCircleOutlined, label: 'common.action.reject', danger: true },
  cancel: { icon: CloseCircleOutlined, label: 'common.action.cancel' },
  enable: { icon: PlayCircleOutlined, label: 'common.action.enable' },
  disable: { icon: StopOutlined, label: 'common.action.disable' },
  // 以下只给图标，文字由页面传 label
  reverse: { icon: RollbackOutlined, danger: true },
  run: { icon: PlayCircleOutlined },
  log: { icon: FileTextOutlined },
  history: { icon: HistoryOutlined },
  list: { icon: UnorderedListOutlined },
  move: { icon: SwapOutlined },
  password: { icon: KeyOutlined },
  setting: { icon: SettingOutlined }
}

/** 每行直接显示的图标最多 4 个（含「更多」）；超过时前 3 个直接显示，其余进「更多」 */
export const MAX_INLINE = 4

/** 图标按钮宽度、竖线分隔（1px + 左右间距 4）、单元格左右内边距（middle 尺寸 8 × 2）、余量 */
const ICON = 24
const DIVIDER = 9
const CELL = 16
const SPARE = 4

/** 与 Ant Design Vue 默认字体一致，用于测量文字宽度 */
const FONT = "14px -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif"
let measureCtx
export function textWidth(text) {
  measureCtx ||= document.createElement('canvas').getContext('2d')
  if (!measureCtx) return String(text).length * 8
  measureCtx.font = FONT
  return measureCtx.measureText(String(text)).width
}

/** 操作的显示文字：label 优先，否则按 type 取 */
export const actionLabel = (a) => a.label || (ACTION_TYPES[a.type]?.label ? t(ACTION_TYPES[a.type].label) : '')

/**
 * 按显示规则把可见的操作分为直接显示和「更多」：
 * - more: true 的操作进「更多」；「更多」中只有一个操作且放得下时直接显示（不为一个操作弹下拉）
 * - 直接显示的超过 MAX_INLINE（含「更多」图标）时，从后往前把操作移进「更多」（写在 slot 中的自定义操作始终直接显示）
 * - 一共只有一个操作时显示「图标 + 文字」（text 为 true）
 */
export function layoutActions(visible, max = MAX_INLINE) {
  let inline = visible.filter((a) => !a.more || a.slot)
  let more = visible.filter((a) => a.more && !a.slot)
  if (more.length === 1 && inline.length + 1 <= max) {
    inline = visible.filter((a) => !a.more || a.slot || a === more[0])
    more = []
  }
  if (inline.length + (more.length ? 1 : 0) > max) {
    const keep = []
    const moved = []
    let room = max - 1
    // 自定义操作必须直接显示，先占位
    room -= inline.filter((a) => a.slot).length
    inline.forEach((a) => {
      if (a.slot) keep.push(a)
      else if (room > 0) {
        keep.push(a)
        room--
      } else moved.push(a)
    })
    inline = keep
    more = [...moved, ...more]
  }
  return { inline, more, text: inline.length === 1 && !more.length }
}

/**
 * 操作列宽度：count 为一行中最多同时显示的图标数（含「更多」，不超过 MAX_INLINE），
 * labels 为可能单独出现的操作文字（单独出现时显示「图标 + 文字」，需要按文字计算），按当前语言测量。
 */
export function actionWidth(count, labels = []) {
  const n = Math.min(Math.max(count, 1), MAX_INLINE)
  const icons = n * ICON + (n - 1) * DIVIDER
  // 图标 + 文字的按钮：图标 14 + 间距 8 + 左右内边距 4 × 2
  const single = Math.max(0, ...labels.filter(Boolean).map((l) => Math.ceil(textWidth(l)) + 30))
  const title = Math.ceil(textWidth(t('common.action.operation'))) + 4
  return Math.max(icons, single, title) + CELL + SPARE
}

/**
 * 操作列定义：{ title: 操作, key: 'action', width, fixed: 'right', align: 'right' }，参数同 actionWidth；
 * extra 合并进列定义（如 { fixed: false }）
 */
export function actionColumn(count, labels = [], extra = {}) {
  return { title: t('common.action.operation'), key: 'action', width: actionWidth(count, labels), fixed: 'right', align: 'right', ...extra }
}
