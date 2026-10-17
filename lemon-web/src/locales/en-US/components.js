/** 公共组件、组合式函数和工具 */
export default {
  im: {
    dingtalk: 'DingTalk',
    dingtalkShort: 'D',
    wecom: 'WeCom',
    wecomShort: 'W',
    feishu: 'Feishu',
    feishuShort: 'F'
  },
  greeting: {
    lateNight: 'Good night',
    earlyMorning: 'Good morning',
    morning: 'Good morning',
    noon: 'Good afternoon',
    afternoon: 'Good afternoon',
    evening: 'Good evening'
  },
  duration: {
    dhm: '{d}d {h}h {m}m',
    dh: '{d}d {h}h',
    hm: '{h}h {m}m',
    ms: '{m}m {s}s',
    s: '{s}s'
  },
  toolbar: {
    export: 'Export to Excel (current filters)',
    large: 'Comfortable',
    middle: 'Default',
    small: 'Compact',
    columns: 'Show columns'
  },
  status: {
    confirmEnable: 'Confirm Enable',
    confirmDisable: 'Confirm Disable',
    enableContent: 'Enable "{name}"?',
    disableContent: 'Disable "{name}"?',
    enabled: 'Enabled',
    disabled: 'Disabled'
  },
  formDrawer: {
    shortcut: '{key} + Enter to save',
    discardContent: 'What you have entered will be lost after closing.'
  },
  record: {
    attachments: 'Attachments',
    history: 'Change History',
    noHistory: 'No change history',
    empty: '(empty)',
    time: 'Time',
    changedBy: 'Changed By',
    field: 'Field',
    change: 'Change'
  },
  theme: {
    light: 'Light',
    dark: 'Dark'
  },
  attachment: {
    upload: 'Upload Attachment',
    maxSize: 'Max {n} MB per file',
    empty: 'No attachments',
    confirmDelete: 'Delete this attachment?',
    tooLarge: '{name} exceeds {n} MB'
  },
  icon: {
    search: 'Search icons, e.g. user',
    noMatch: 'No matching icons',
    select: 'Click to choose an icon'
  },
  import: {
    title: 'Import {name}',
    tipBefore: 'First',
    downloadTemplate: 'download the import template',
    tipAfter: ', fill it in and upload it. Columns marked * are required. If any row has errors, nothing is imported.',
    dropHint: 'Click or drag an .xlsx file here',
    rows: '{n} rows',
    rowsWithErrors: '{n} rows, {e} with errors',
    success: '{n} rows imported',
    rowNum: 'Row',
    error: 'Error',
    required: '{name} is required',
    notFound: '{name} "{value}" does not exist',
    invalidNumber: '{name} "{value}" is not a valid number',
    invalidDate: '{name} "{value}" is not a valid date',
    invalidBool: '{name} must be Yes or No',
    sep: '; ',
    noData: 'The file contains no data',
    parseFailed: 'Failed to parse the file. Please use the import template'
  },
  excel: {
    noSheet: 'The file has no worksheet',
    missingColumns: 'Missing columns: {columns}. Please use the import template',
    listSep: ', ',
    templateName: '{name} Import Template'
  },
  chart: {
    yi: '{n} 100M',
    wan: '{n} 10K',
    month: 'M{n}'
  },
  user: {
    searchPlaceholder: 'Search username or nickname',
    moveTitle: 'Change Department ({n} users)',
    targetDept: 'Target Department',
    moveTip: 'After the change, "own department" data permissions take effect immediately based on the new department.',
    moved: 'Department changed',
    confirmN: 'OK ({n})',
    selectedN: '{n} selected',
    selectedOne: 'Selected: {name}',
    pickTitle: 'Select Users',
    added: 'Added',
    noMatch: 'No matching users',
    editUser: 'Edit User',
    usernamePlaceholder: 'Login username',
    dept: 'Department',
    selectDept: 'Select the department'
  },
  customFields: {
    title: 'Additional Info',
    required: 'Please fill in {names}'
  },
  tree: {
    selectAll: 'Select all',
    linked: 'Cascade',
    noMatch: 'No matching data',
    selectedOf: '{n} / {total} selected',
    linkAncestors: 'Checking also checks children and parents; unchecking also unchecks children',
    linkDescendants: 'Checking or unchecking also applies to all children',
    searchDept: 'Search departments',
    allDepts: 'All Departments',
    noMatchDept: 'No matching departments',
    noDept: 'No departments'
  },
  feature: {
    industry: 'Industry Templates',
    industryTip: 'Selecting one checks the feature modules commonly used in that industry; you can adjust them afterwards',
    moduleCount: '{n} modules',
    modules: 'Feature Modules',
    modulesTip: '{n} / {total} selected. Checking adds required modules automatically; unchecking also removes modules that depend on it',
    required: 'Required',
    requires: 'Requires: {names}',
    addedDeps: 'Also checked required modules: {names}',
    removedDeps: 'Also unchecked dependent modules: {names}'
  },
  layout: {
    searchMenuShortcut: 'Search menus ({key} K)',
    searchMenu: 'Search menus',
    searchPlaceholder: 'Type a page or module name, e.g. Inbound, Voucher, Finance',
    resultsN: '{n} results',
    recent: 'Recent',
    noMatch: 'No matching pages',
    noRecent: 'No recent pages yet. Type a keyword to search',
    open: 'Open',
    unreadMessages: 'Unread Messages',
    noUnread: 'No unread messages',
    moreUnread: '{n} more unread. View all',
    viewAll: 'View all messages',
    reloadPage: 'Reload page',
    closeOthers: 'Close Others',
    closeLeft: 'Close Left',
    closeRight: 'Close Right',
    closeAll: 'Close All'
  },
  password: {
    letterDigit: 'containing letters and digits',
    mixedCaseDigit: 'containing uppercase letters, lowercase letters and digits',
    letterDigitSymbol: 'containing letters, digits and special characters',
    hint: '{min}-32 characters, {rule}',
    copied: 'A random password was generated and copied to the clipboard',
    copyManually: 'A random password was generated. Please copy it manually'
  },
  table: {
    exportLimited: 'Too much data: only the first {n} rows were exported. Please narrow the filters'
  }
}
