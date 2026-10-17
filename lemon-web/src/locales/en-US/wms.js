/** 仓库管理（wms.xxx）；按分组拆分在 _wms/ 下（顶层键不能重复） */
import components from './_wms/components.js'
import views from './_wms/views.js'
import label from './_wms/label.js'

export default { ...components, ...views, ...label }
