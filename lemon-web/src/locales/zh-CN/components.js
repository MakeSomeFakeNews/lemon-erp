/** 公共组件、组合式函数和工具 */
export default {
  im: {
    dingtalk: '钉钉',
    dingtalkShort: '钉',
    wecom: '企业微信',
    wecomShort: '企',
    feishu: '飞书',
    feishuShort: '飞'
  },
  greeting: {
    lateNight: '夜深了',
    earlyMorning: '早上好',
    morning: '上午好',
    noon: '中午好',
    afternoon: '下午好',
    evening: '晚上好'
  },
  duration: {
    dhm: '{d} 天 {h} 小时 {m} 分',
    dh: '{d} 天 {h} 小时',
    hm: '{h} 小时 {m} 分',
    ms: '{m} 分 {s} 秒',
    s: '{s} 秒'
  },
  toolbar: {
    export: '导出 Excel（当前查询条件）',
    large: '宽松',
    middle: '默认',
    small: '紧凑',
    columns: '列展示'
  },
  status: {
    confirmEnable: '确认启用',
    confirmDisable: '确认停用',
    enableContent: '确定要启用「{name}」吗？',
    disableContent: '确定要停用「{name}」吗？',
    enabled: '已启用',
    disabled: '已停用'
  },
  formDrawer: {
    shortcut: '{key} + Enter 保存',
    discardContent: '关闭后，本次填写的内容将会丢失。'
  },
  record: {
    attachments: '附件',
    history: '变更记录',
    noHistory: '暂无变更记录',
    empty: '（空）',
    time: '时间',
    changedBy: '修改人',
    field: '字段',
    change: '变更'
  },
  theme: {
    light: '亮色',
    dark: '暗色'
  },
  attachment: {
    upload: '上传附件',
    maxSize: '单个文件不超过 {n}MB',
    empty: '暂无附件',
    confirmDelete: '确定删除该附件？',
    tooLarge: '{name} 超过 {n}MB'
  },
  icon: {
    search: '搜索图标，如 user',
    noMatch: '没有匹配的图标',
    select: '点击选择图标'
  },
  import: {
    title: '导入{name}',
    tipBefore: '先',
    downloadTemplate: '下载导入模板',
    tipAfter: '，按模板填写后上传；带 * 的列必填，任何一行有错误时整批不导入。',
    dropHint: '点击或拖拽 .xlsx 文件到这里',
    rows: '共 {n} 行',
    rowsWithErrors: '共 {n} 行，{e} 行有错误',
    success: '成功导入 {n} 行',
    rowNum: '行号',
    error: '错误',
    required: '{name}不能为空',
    notFound: '{name}「{value}」不存在',
    invalidNumber: '{name}「{value}」不是有效的数字',
    invalidDate: '{name}「{value}」不是有效的日期',
    invalidBool: '{name}请填写 是 或 否',
    sep: '；',
    noData: '文件中没有数据',
    parseFailed: '文件解析失败，请使用导入模板'
  },
  excel: {
    noSheet: '文件中没有工作表',
    missingColumns: '缺少列：{columns}，请使用导入模板',
    listSep: '、',
    templateName: '{name}导入模板'
  },
  chart: {
    yi: '{n} 亿',
    wan: '{n} 万',
    month: '{n}月'
  },
  user: {
    searchPlaceholder: '搜索用户名或昵称',
    moveTitle: '调整部门（{n} 人）',
    targetDept: '目标部门',
    moveTip: '调整后，用户的「本部门」类数据权限会按新部门立即生效。',
    moved: '部门已调整',
    confirmN: '确定（{n}）',
    selectedN: '已选 {n} 人',
    selectedOne: '已选 {name}',
    pickTitle: '选择用户',
    added: '已添加',
    noMatch: '没有匹配的用户',
    editUser: '编辑用户',
    usernamePlaceholder: '登录用户名',
    dept: '所属部门',
    selectDept: '请选择所属部门'
  },
  customFields: {
    title: '扩展信息',
    required: '请填写{names}'
  },
  tree: {
    selectAll: '全选',
    linked: '父子联动',
    noMatch: '没有匹配的数据',
    selectedOf: '已选择 {n} / {total} 项',
    linkAncestors: '勾选时同时勾选下级和上级，取消时同时取消下级',
    linkDescendants: '勾选或取消时同时处理全部下级',
    searchDept: '搜索部门',
    allDepts: '全部部门',
    noMatchDept: '没有匹配的部门',
    noDept: '暂无部门'
  },
  feature: {
    industry: '行业模板',
    industryTip: '选择后自动勾选该行业常用的功能模块，可再调整',
    moduleCount: '{n} 个模块',
    modules: '功能模块',
    modulesTip: '已选 {n} / {total}；勾选时自动加上依赖的模块，取消时一并取消依赖它的模块',
    required: '必选',
    requires: '依赖：{names}',
    addedDeps: '同时勾选了依赖的模块：{names}',
    removedDeps: '同时取消了依赖它的模块：{names}'
  },
  layout: {
    searchMenuShortcut: '搜索菜单（{key} K）',
    searchMenu: '搜索菜单',
    searchPlaceholder: '输入页面名称或所属模块，如：入库、凭证、财务',
    resultsN: '{n} 个结果',
    recent: '最近访问',
    noMatch: '没有匹配的页面',
    noRecent: '还没有访问记录，输入关键字搜索',
    open: '打开',
    unreadMessages: '未读消息',
    noUnread: '没有未读消息',
    moreUnread: '还有 {n} 条未读，查看全部',
    viewAll: '查看全部消息',
    reloadPage: '刷新当前页',
    closeOthers: '关闭其他',
    closeLeft: '关闭左侧',
    closeRight: '关闭右侧',
    closeAll: '关闭全部'
  },
  password: {
    letterDigit: '包含字母和数字',
    mixedCaseDigit: '包含大写字母、小写字母和数字',
    letterDigitSymbol: '包含字母、数字和特殊字符',
    hint: '{min}-32 位，{rule}',
    copied: '已生成随机密码并复制到剪贴板',
    copyManually: '已生成随机密码，请手动复制'
  },
  table: {
    exportLimited: '数据较多，只导出了前 {n} 条，请缩小查询范围'
  }
}
