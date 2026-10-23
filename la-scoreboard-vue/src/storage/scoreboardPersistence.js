import {
  loadStoredMatch,
  prepareMatchStorage,
  saveStoredMatch,
} from './matchStorage.js'

const READ_ERROR = '本场缓存无法读取或保存。仍可在页面测试；导出后可新建比赛重新保存。'
const WRITE_ERROR = '浏览器无法保存记录。请在关闭页面前导出比赛 JSON。'
const EXTERNAL_CHANGE = '另一个页面已修改本地比赛，当前页面停止保存并停表保护。请刷新以读取最新记录。'

export function createScoreboardPersistence(storageApi, hooks = {}) {
  let storage = storageApi
  let blocked = false

  function report(message) {
    hooks.onMessage?.(message)
  }

  function resolveStorage() {
    storage ||= localStorage
    return storage
  }

  function load(restore) {
    try {
      const target = resolveStorage()
      prepareMatchStorage(target)
      const cached = loadStoredMatch(target)
      return cached && restore ? restore(cached) : cached
    } catch {
      blocked = true
      report(READ_ERROR)
      return null
    }
  }

  function save(match) {
    if (blocked) return false
    try {
      saveStoredMatch(resolveStorage(), match)
      return true
    } catch {
      report(WRITE_ERROR)
      return false
    }
  }

  function blockExternal() {
    blocked = true
    report(EXTERNAL_CHANGE)
  }

  function reset() {
    blocked = false
    report('')
  }

  return {
    blockExternal,
    get blocked() { return blocked },
    load,
    reset,
    save,
  }
}
