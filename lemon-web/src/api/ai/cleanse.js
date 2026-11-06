import request from '@/utils/request'

/** 数据整理：当前用户可整理的数据类型 [{ key, label, fields, canApply, supportsDisable }] */
export const listCleanseSources = () => request.get('/ai/cleanse/sources')
/** 任务列表（新的在前）：{ records, total } */
export const listCleanseTasks = (params) => request.get('/ai/cleanse/tasks', { params })
/** 发起整理（后台执行）：{ sourceKey, keyword } */
export const startCleanse = (data) => request.post('/ai/cleanse/tasks', data)
/** 任务详情：{ task: { status, phase, totalSteps, doneSteps, ... }, stats: [{ kind, status, cnt }] } */
export const getCleanseTask = (id) => request.get(`/ai/cleanse/tasks/${id}`)
export const cancelCleanse = (id) => request.post(`/ai/cleanse/tasks/${id}/cancel`)
/** 建议：params { current, size, kinds: 'NORMALIZE,MISSING', status, field, minConfidence, keyword } */
export const listCleanseSuggestions = (id, params) => request.get(`/ai/cleanse/tasks/${id}/suggestions`, { params })
/** 采纳：逐条返回 [{ id, success, message }] */
export const applyCleanseSuggestions = (ids) => request.post('/ai/cleanse/suggestions/apply', { ids })
/** 忽略：返回忽略的条数 */
export const ignoreCleanseSuggestions = (ids) => request.post('/ai/cleanse/suggestions/ignore', { ids })

/**
 * 导入字段对应：{ columns: [{ key, title, required }], headers: [...], samples: [[...]] }
 * → { mappings: [{ header, key, reason }], unmatched: [...], missing: [...] }
 */
export const mapImportColumns = (data) => request.post('/ai/import/mapping', data, { timeout: 150000 })
