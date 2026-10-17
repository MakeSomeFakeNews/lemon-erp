/** 条码标签（components/label、utils/labels.js） */
export default {
  label: {
    material: {
      name: '物料标签',
      fieldName: '物料名称',
      fieldCode: '物料编码',
      fieldSpec: '规格型号',
      fieldUnit: '单位',
      sampleName: '工控主机',
      sampleUnit: '台'
    },
    location: {
      name: '库位标签',
      fieldCode: '库位编码',
      fieldName: '库位名称',
      fieldZone: '区域',
      fieldWarehouse: '仓库',
      sampleName: '整机区 1 排 2 层',
      sampleZone: 'A 区',
      sampleWarehouse: '深圳成品仓'
    },
    box: {
      name: '箱标签',
      fieldBoxNo: '箱号',
      fieldCustomer: '客户',
      fieldDeliveryNo: '发货通知',
      fieldSeq: '箱序',
      fieldContents: '箱内物料',
      fieldWeight: '毛重',
      sampleCustomer: '深圳智造自动化有限公司',
      sampleContents: 'IPC-3000 × 2、PKG-ESD × 4'
    },
    codeError: '无法生成条码：{value}',
    title: '标签',
    previewTitle: '标签预览',
    printTitle: '打印{name}',
    copiesEach: '每个打印',
    sheets: '张',
    summary: '共 {n} 个，打印 {total} 张。标签纸尺寸、条码类型、显示字段在「系统管理 - 打印模板 - 标签」中设置',
    editor: {
      modified: '已修改',
      confirmReset: '确定恢复默认样式？',
      reset: '恢复默认',
      paper: '标签纸',
      width: '宽',
      height: '高',
      fontSize: '字号',
      code: '条码',
      barcode: '条形码（Code 128）',
      qr: '二维码',
      showText: '条码下显示内容',
      fields: '显示字段',
      fieldsHint: '勾选显示，可加前缀（如「编码」），调整顺序；第一行加粗',
      prefix: '前缀',
      preview: '预览（示例数据）',
      saved: '已保存，之后打印即按新样式',
      resetDone: '已恢复默认样式'
    }
  }
}
