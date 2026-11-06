import request from '@/utils/request'

/** 识别可能较慢（识图模型、校验不通过重试一次） */
const TIMEOUT = 300000

/** 当前用户可用的录入目标：[{ key, label, route, form }] */
export const listExtractTargets = () => request.get('/ai/extract/targets')

const formData = ({ target, text, files }) => {
  const fd = new FormData()
  if (target) fd.append('target', target)
  if (text) fd.append('text', text)
  ;(files || []).forEach((f) => fd.append('files', f))
  return fd
}

/** 判断内容最可能是哪种单据：[{ key, label, confidence, reason }]，按可能性从高到低 */
export const detectExtractTarget = (input) => request.post('/ai/extract/detect', formData(input), { timeout: TIMEOUT })

/**
 * 识别并生成预填数据（不保存单据）：
 * { target, label, route, form, data, warnings: [{ field, value, message }], extracted, source, truncated, promptTokens, completionTokens }
 */
export const runExtract = (input) => request.post('/ai/extract', formData(input), { timeout: TIMEOUT })
