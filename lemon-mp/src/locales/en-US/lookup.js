/** Quick lookup (lookup.xxx): search, price setting, images, stock by location, selling / purchase price history, trades, price reference */
export default {
  entry: 'Quick Lookup',
  searchPh: 'Name / spec / initials, keyboard voice input works',
  voice: 'Voice',
  voiceHold: 'Hold to talk',
  voiceRelease: 'Release to finish',
  voiceFail: 'Nothing recognized',
  voiceDenied: 'Please allow microphone access',
  colIndex: '#',
  colName: 'Product',
  colQty: 'Stock',
  colPrice: 'Price',
  noPrice: 'No price',
  priceDenied: 'No permission for the selected price, showing quantities only',
  tapTip: 'Tap product for images, stock for locations, price for history; long press for more',
  actions: {
    image: 'Product Images',
    stock: 'Stock by Location',
    sales: 'Selling Price History',
    purchase: 'Purchase Price History',
    history: 'Movement History'
  },
  setting: {
    title: 'Displayed Price',
    tip: 'Choose one price to show in the results, or none to show quantities only. Visible prices depend on your role; grey ones are not permitted',
    purchase: 'Purchase',
    cost: 'Cost',
    sales: 'Sales',
    none: 'Other',
    noPermission: 'No permission',
    saved: 'Saved'
  },
  image: {
    title: 'Images of {name}',
    empty: 'No images (upload product images in Materials on the web)'
  },
  stock: {
    title: 'Stock of {name}',
    batch: 'Batch {batch}',
    expiry: 'Expires {date}',
    frozen: 'Frozen {n}',
    total: 'Total {n}',
    history: 'Movement History'
  },
  price: {
    title: '{name}',
    sales: 'Selling',
    purchase: 'Purchase',
    recent: 'Last #{n}',
    latest: 'Latest',
    max: 'Highest',
    min: 'Lowest',
    none: 'No records',
    noPermission: 'No permission to view this price',
    moreTrades: 'More Trades',
    moreContent: 'More',
    scope: 'Based on documents you can see; prices are tax-inclusive before discounts'
  },
  trades: {
    salesTitle: 'Sales Trades',
    purchaseTitle: 'Purchase Trades',
    start: 'Start date',
    end: 'End date',
    keywordPh: 'Partner code, name or phone',
    qty: 'Qty {n}',
    prepared: 'Prepared {name}',
    reviewed: 'Reviewed {name}'
  },
  ref: {
    title: 'Price Reference',
    keywordPh: 'Partner code or name',
    quotations: 'Quotes',
    sales: 'Sales',
    purchases: 'Purchases',
    customerCompare: 'Customers',
    supplierCompare: 'Suppliers',
    noPermission: 'No permission to view price reference',
    last: 'Latest {price} ({date})',
    min: 'Lowest {price} ({date})',
    max: 'Highest {price} ({date})'
  }
}
