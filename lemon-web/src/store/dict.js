import { defineStore } from 'pinia'
import { getDictDataByType } from '@/api/system/dict'

/**
 * 字典缓存：同一类型只请求一次，并发请求共享同一个 Promise
 */
export const useDictStore = defineStore('dict', {
  state: () => ({
    cache: {}
  }),
  actions: {
    load(type) {
      if (this.cache[type]) return Promise.resolve(this.cache[type])
      if (!pending.has(type)) {
        pending.set(
          type,
          getDictDataByType(type)
            .then((list) => {
              this.cache[type] = list || []
              return this.cache[type]
            })
            .finally(() => pending.delete(type))
        )
      }
      return pending.get(type)
    },
    /** 字典变更后清理缓存；不传 type 清空全部 */
    clear(type) {
      if (type) delete this.cache[type]
      else this.cache = {}
    }
  }
})

const pending = new Map()
