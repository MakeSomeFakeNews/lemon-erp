import request from '@/utils/request'

/** 参数设置：[{ key, group, groupLabel, label, type, value, defaultValue, modified, options, min, max, remark }] */
export const listConfig = () => request.get('/system/config')
/** 保存参数：{ key: value }，值为空时恢复默认 */
export const saveConfig = (data) => request.put('/system/config', data)
