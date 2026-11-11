/** Mobile documents (trade.xxx): quick receipt (direct purchase receipt), save modes (draft / save and post), drafts, print / share PDF */
export default {
  entry: {
    purchase: 'Quick Receipt',
    draft: 'Drafts'
  },
  kind: {
    salesOut: 'Sales Delivery',
    purchaseIn: 'Purchase Receipt'
  },
  partner: {
    salesOut: 'Customer',
    purchaseIn: 'Supplier'
  },
  save: {
    save: 'Save',
    draft: 'Save Draft',
    post: 'Save & Post',
    draftSaved: 'Draft saved, pending review',
    posted: 'Posted'
  },
  purchase: {
    disabled: 'Direct documents are disabled (business rule "Allow direct documents"). Receive against a purchase order on the desktop or use Receive.',
    supplier: 'Supplier',
    bizDate: 'Receipt Date',
    scanTip: 'Scan a product or location barcode, or enter a code or name',
    emptyRows: 'Scan or search products to start',
    batchRequired: 'Required',
    expiry: 'Expiry',
    payable: 'Payable',
    submit: 'Confirm Receipt',
    selectSupplier: 'Please select a supplier',
    sheet: {
      supplier: 'Select Supplier',
      material: 'Select Product'
    }
  },
  draft: {
    title: 'Draft',
    searchPh: 'Search document no.',
    empty: 'No drafts pending review',
    status: 'Draft',
    bizDate: 'Business Date',
    orderNo: 'Order No.',
    totalAmount: 'Total (estimated)',
    createBy: 'Prepared By',
    items: 'Lines ({n})',
    qtyPrice: '{qty} {unit} × {price}',
    post: 'Post',
    postConfirm: 'Post draft {no}? Stock will move and receivables / payables will be created.',
    deleteConfirm: 'Delete draft {no}?'
  },
  print: {
    ask: 'Print or share this document (PDF)?',
    print: 'Print / Share',
    later: 'Not now',
    generating: 'Generating PDF',
    failed: 'Failed to generate PDF',
    direct: 'Direct'
  }
}
