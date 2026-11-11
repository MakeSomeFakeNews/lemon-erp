/** 商品速查（lookup.xxx）：搜索、查询价格设置、图片、库存分布、售价 / 进价历史、往来明细、价格参考 */
export default {
  entry: '商品速查',
  searchPh: '名称 / 规格 / 简拼，可用键盘语音输入',
  voice: '语音',
  voiceHold: '按住说话',
  voiceRelease: '松开结束',
  voiceFail: '没有识别到内容',
  voiceDenied: '请允许使用麦克风',
  colIndex: '#',
  colName: '商品',
  colQty: '库存',
  colPrice: '价格',
  noPrice: '不显示价格',
  priceDenied: '所选价格没有查看权限，只显示数量',
  tapTip: '点击商品看图片、点击库存看分布、点击价格看历史；长按更多',
  actions: {
    image: '商品图片',
    stock: '库存分布',
    sales: '售价历史参考',
    purchase: '进价历史参考',
    history: '经营历史（进销存）'
  },
  setting: {
    title: '查询价格',
    tip: '从下面选择一种价格显示在速查结果中；不选价格则只显示数量。能看到哪些价格由职责（权限）决定，灰色的没有权限',
    purchase: '进货类',
    cost: '成本',
    sales: '销售类',
    none: '其他',
    noPermission: '无权限',
    saved: '已保存'
  },
  image: {
    title: '{name} 的图片',
    empty: '没有图片（在电脑端「物料管理」的商品图片中上传）'
  },
  stock: {
    title: '{name} 的库存',
    batch: '批次 {batch}',
    expiry: '有效期至 {date}',
    frozen: '冻结 {n}',
    total: '合计 {n}',
    history: '经营历史'
  },
  price: {
    title: '{name}',
    sales: '售价',
    purchase: '进价',
    recent: '最近第 {n} 次',
    latest: '系统最新价',
    max: '系统最高价',
    min: '系统最低价',
    none: '暂无记录',
    noPermission: '没有查看该价格的权限',
    moreTrades: '更多交易历史',
    moreContent: '更多内容',
    scope: '按看得到的单据统计，单价为优惠前含税单价'
  },
  trades: {
    salesTitle: '售价交易历史',
    purchaseTitle: '进价交易历史',
    start: '开始日期',
    end: '结束日期',
    keywordPh: '单位编号、名称或电话',
    qty: '数量 {n}',
    prepared: '制单 {name}',
    reviewed: '复核 {name}'
  },
  ref: {
    title: '价格参考',
    keywordPh: '往来单位编码或名称',
    quotations: '报价',
    sales: '销售',
    purchases: '进货',
    customerCompare: '客户',
    supplierCompare: '供货商',
    noPermission: '没有查看价格参考的权限',
    last: '最新 {price}（{date}）',
    min: '最低 {price}（{date}）',
    max: '最高 {price}（{date}）'
  }
}
