/** 审批（approval.xxx） */
export default {
  entry: '审批',
  title: '审批',
  detailTitle: '审批详情',
  tab: {
    todo: '待办',
    done: '已办',
    mine: '我发起的',
    copy: '抄送我的'
  },
  searchPh: '搜索流程标题',
  starter: '发起人',
  startTime: '发起时间',
  arriveTime: '到达时间',
  handleTime: '处理时间',
  endTime: '结束时间',
  process: '流程',
  currentNode: '当前节点',
  result: '处理结果',
  business: '业务信息',
  form: '申请内容',
  noForm: '没有表单数据',
  records: '审批记录',
  record: {
    start: '发起',
    approve: '同意',
    reject: '拒绝',
    transfer: '转交',
    cancel: '撤回',
    terminate: '终止',
    cc: '抄送',
    pending: '待审批'
  },
  action: {
    approve: '同意',
    reject: '拒绝',
    transfer: '转交',
    cancel: '撤回',
    comment: '审批意见',
    commentPh: '选填',
    rejectReason: '拒绝原因',
    rejectReasonPh: '请填写拒绝原因',
    transferTo: '转交给',
    transferNote: '转交说明',
    selectTransferee: '请选择转交人',
    notSelected: '点击选择',
    cancelReason: '撤回原因',
    cancelPh: '选填，撤回后流程结束',
    approved: '已同意',
    rejected: '已拒绝',
    transferred: '已转交',
    cancelled: '已撤回'
  },
  copy: {
    node: '抄送节点',
    time: '抄送时间'
  },
  start: {
    entry: '发起申请',
    title: '发起申请',
    searchPh: '搜索流程',
    clickToStart: '点击发起申请',
    noMatch: '没有匹配的流程',
    empty: '暂无可发起的流程',
    other: '其他',
    formTitle: '发起：{name}',
    noForm: '该流程没有表单，直接提交即可',
    submitted: '已提交',
    confirm: '确认提交申请？',
    inputNumber: '请输入数字',
    selectDate: '请选择日期',
    mustNumber: '{name}必须是数字',
    outRange: '{name}超出范围',
    tooLong: '{name}不能超过 {n} 个字',
    clear: '清除'
  },
  user: {
    title: '选择转交人',
    searchPh: '搜索账号或姓名',
    self: '本人'
  }
}
