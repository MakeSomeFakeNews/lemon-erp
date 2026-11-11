/** 手机开单（trade.xxx）：快速入库（采购入库直接开单）、保存方式（存草稿 / 保存并审核）、单据草稿、打印 / 分享 PDF */
export default {
  entry: {
    purchase: '快速入库',
    draft: '单据草稿'
  },
  kind: {
    salesOut: '销售出库',
    purchaseIn: '采购入库'
  },
  partner: {
    salesOut: '客户',
    purchaseIn: '供应商'
  },
  save: {
    save: '保存',
    draft: '存草稿',
    post: '保存并审核',
    draftSaved: '已存草稿，待审核',
    posted: '已审核记账'
  },
  purchase: {
    disabled: '直接开单已关闭（业务参数「允许直接开单」），请在电脑端按采购订单入库，或用「扫码收货」',
    supplier: '供应商',
    bizDate: '入库日期',
    scanTip: '扫描商品、库位条码，或输入编码、名称',
    emptyRows: '扫描或搜索商品开始入库',
    batchRequired: '必填',
    expiry: '有效期至',
    payable: '应付',
    submit: '确认入库',
    selectSupplier: '请选择供应商',
    sheet: {
      supplier: '选择供应商',
      material: '选择商品'
    }
  },
  draft: {
    title: '草稿详情',
    searchPh: '搜索单据号',
    empty: '没有待审核的草稿',
    status: '草稿',
    bizDate: '业务日期',
    orderNo: '订单号',
    totalAmount: '价税合计（估算）',
    createBy: '制单',
    items: '明细（{n} 行）',
    qtyPrice: '{qty} {unit} × {price}',
    post: '审核记账',
    postConfirm: '审核草稿 {no}？审核后出入库并生成应收应付',
    deleteConfirm: '删除草稿 {no}？'
  },
  print: {
    ask: '是否打印或分享这张单据（PDF）？',
    print: '打印 / 分享',
    later: '暂不',
    generating: '正在生成 PDF',
    failed: '生成 PDF 失败',
    direct: '直接开单',
    pickFormat: '选择打印格式',
    lastUsed: '上次使用',
    isDefault: '默认',
    more: '更多格式…',
    formatTagged: '{name}（{tags}）'
  }
}
