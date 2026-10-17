/** 条码标签（components/label、utils/labels.js） */
export default {
  label: {
    material: {
      name: 'Material Label',
      fieldName: 'Material Name',
      fieldCode: 'Material Code',
      fieldSpec: 'Specification',
      fieldUnit: 'Unit',
      sampleName: 'Industrial PC',
      sampleUnit: 'pcs'
    },
    location: {
      name: 'Location Label',
      fieldCode: 'Location Code',
      fieldName: 'Location Name',
      fieldZone: 'Zone',
      fieldWarehouse: 'Warehouse',
      sampleName: 'Assembly Zone Row 1 Level 2',
      sampleZone: 'Zone A',
      sampleWarehouse: 'Shenzhen FG Warehouse'
    },
    box: {
      name: 'Box Label',
      fieldBoxNo: 'Box No.',
      fieldCustomer: 'Customer',
      fieldDeliveryNo: 'Delivery Notice',
      fieldSeq: 'Box Seq.',
      fieldContents: 'Contents',
      fieldWeight: 'Gross Weight',
      sampleCustomer: 'Shenzhen Smart Automation Co., Ltd.',
      sampleContents: 'IPC-3000 × 2, PKG-ESD × 4'
    },
    codeError: 'Cannot generate barcode: {value}',
    title: 'Labels',
    previewTitle: 'Label preview',
    printTitle: 'Print {name}',
    copiesEach: 'Copies each',
    sheets: 'sheets',
    summary: '{n} items, {total} labels. Label size, barcode type and fields are set in "System - Print Templates - Labels"',
    editor: {
      modified: 'Modified',
      confirmReset: 'Restore the default style?',
      reset: 'Restore Default',
      paper: 'Label Paper',
      width: 'Width',
      height: 'Height',
      fontSize: 'Font Size',
      code: 'Code',
      barcode: 'Barcode (Code 128)',
      qr: 'QR Code',
      showText: 'Show text under code',
      fields: 'Fields',
      fieldsHint: 'Check to show, add a prefix (e.g. "Code"), reorder; the first line is bold',
      prefix: 'Prefix',
      preview: 'Preview (sample data)',
      saved: 'Saved; new prints will use this style',
      resetDone: 'Default style restored'
    }
  }
}
