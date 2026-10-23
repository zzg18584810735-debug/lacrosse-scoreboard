import { defineStore } from 'pinia'
import { computed, ref, shallowRef } from 'vue'
import { scoreboardApi } from '../api/scoreboard.js'
import {
  applyMatchStateSaveResult,
  toScoreboardMatch,
} from '../adapters/matchDetailAdapter.js'

export const SYNC_STATUS = Object.freeze({
  LOCAL: 'local',
  LOADING: 'loading',
  SYNCING: 'syncing',
  SYNCED: 'synced',
  OFFLINE: 'offline',
  CONFLICT: 'conflict',
  ERROR: 'error',
})

/**
 * Pinia 比赛状态仓库。
 * 计分引擎会原地修改比赛对象，因此使用 shallowRef，并在一次业务
 * 操作完成后显式调用 notify() 替换根对象引用。根引用变化后，接收
 * match 属性的时钟等子组件才能被 Vue 正确刷新。
 */
export const useScoreboardStore = defineStore('scoreboard', () => {
  const match = shallowRef(null)
  const ageGroups = ref([])
  const groupRules = ref([])
  const revision = ref(0)
  const remoteDetail = shallowRef(null)
  const remoteMatchId = ref(null)
  const stateVersion = ref(null)
  const syncStatus = ref(SYNC_STATUS.LOCAL)
  const syncError = ref('')
  const lastSyncedAt = ref(null)
  const syncMessage = computed(() => ({
    [SYNC_STATUS.LOCAL]: '仅保存于本机',
    [SYNC_STATUS.LOADING]: '正在读取服务器比赛',
    [SYNC_STATUS.SYNCING]: '正在同步',
    [SYNC_STATUS.SYNCED]: '已与服务器同步',
    [SYNC_STATUS.OFFLINE]: '网络不可用，暂存于本机',
    [SYNC_STATUS.CONFLICT]: '服务器比赛已变化，请刷新核对',
    [SYNC_STATUS.ERROR]: '同步失败',
  })[syncStatus.value])

  function replace(nextState) {
    match.value = nextState
    revision.value += 1
  }

  function notify(nextState = match.value) {
    match.value = { ...nextState }
    revision.value += 1
  }

  async function loadMatchDetail(matchId, api = scoreboardApi, adapt = toScoreboardMatch) {
    const normalizedId = normalizeMatchId(matchId)
    remoteMatchId.value = normalizedId
    remoteDetail.value = null
    stateVersion.value = null
    syncStatus.value = SYNC_STATUS.LOADING
    syncError.value = ''
    try {
      const detail = await api.getMatchDetail(normalizedId)
      requireDetail(detail)
      const adaptedMatch = adapt(detail)
      remoteDetail.value = detail
      stateVersion.value = detail.state.stateVersion
      replace(adaptedMatch)
      markSynced(stateVersion.value)
      return detail
    } catch (error) {
      markSyncFailed(error)
      throw error
    }
  }

  async function loadGroupRules(api = scoreboardApi) {
    const rules = await api.listGroupRules()
    if (!Array.isArray(rules)) throw new Error('服务器返回的组别规则不完整。')
    groupRules.value = rules
    return rules
  }

  async function loadAgeGroups(api = scoreboardApi) {
    const items = await api.listAgeGroups()
    if (!Array.isArray(items)) throw new Error('服务器返回的比赛组别字典不完整。')
    ageGroups.value = items
    return items
  }

  async function loadLatestMatch(api = scoreboardApi, adapt = toScoreboardMatch) {
    const matches = await api.listMatches()
    if (!Array.isArray(matches)) throw new Error('服务器返回的比赛列表不完整。')
    if (!matches.length) return null
    const latest = matches[0]
    if (latest?.id === null || latest?.id === undefined) {
      throw new Error('最近比赛缺少比赛 ID。')
    }
    await loadMatchDetail(latest.id, api, adapt)
    return latest
  }

  async function createRemoteMatch(payload, api = scoreboardApi, adapt = toScoreboardMatch) {
    const matchId = await api.createMatch(payload)
    return loadMatchDetail(matchId, api, adapt)
  }

  async function syncMatchInfo(payload, api = scoreboardApi, adapt = toScoreboardMatch) {
    if (remoteMatchId.value === null) throw new Error('当前不是服务器比赛。')
    await api.updateMatchInfo(remoteMatchId.value, payload)
    return loadMatchDetail(remoteMatchId.value, api, adapt)
  }

  async function syncMatchRule(payload, api = scoreboardApi, adapt = toScoreboardMatch) {
    if (remoteMatchId.value === null) throw new Error('当前不是服务器比赛。')
    await api.updateMatchRule(remoteMatchId.value, payload)
    return loadMatchDetail(remoteMatchId.value, api, adapt)
  }

  async function clearRemoteMatchRecords(api = scoreboardApi, adapt = toScoreboardMatch) {
    if (remoteMatchId.value === null) throw new Error('当前不是服务器比赛。')
    try {
      beginSync()
      requireVersion(stateVersion.value)
      const matchId = remoteMatchId.value
      await api.clearMatchRecords(matchId, { expectedVersion: stateVersion.value })
      return loadMatchDetail(matchId, api, adapt)
    } catch (error) {
      markSyncFailed(error)
      throw error
    }
  }

  async function syncMatchState(payload, api = scoreboardApi) {
    if (remoteMatchId.value === null) return null
    try {
      beginSync()
      requireVersion(stateVersion.value)
      const result = await api.saveMatchState(remoteMatchId.value, {
        ...payload,
        expectedVersion: stateVersion.value,
      })
      requireStateSaveResult(result)
      applyMatchStateSaveResult(match.value, result, remoteDetail.value)
      mergeRemoteResult(result)
      notify()
      markSynced(result.state.stateVersion)
      return result
    } catch (error) {
      markSyncFailed(error)
      throw error
    }
  }

  function mergeRemoteResult(result) {
    if (!remoteDetail.value) return
    remoteDetail.value = {
      ...remoteDetail.value,
      state: result.state,
      events: mergeById(remoteDetail.value.events, result.events),
      penalties: mergeById(remoteDetail.value.penalties, result.penalties),
    }
  }

  function beginSync() {
    if (syncStatus.value === SYNC_STATUS.CONFLICT) {
      throw new Error('服务器比赛已变化，请先刷新比赛详情。')
    }
    syncStatus.value = SYNC_STATUS.SYNCING
    syncError.value = ''
  }

  function markSynced(nextVersion = stateVersion.value, syncedAt = Date.now()) {
    if (nextVersion !== null && nextVersion !== undefined) {
      requireVersion(nextVersion)
      stateVersion.value = nextVersion
    }
    syncStatus.value = SYNC_STATUS.SYNCED
    syncError.value = ''
    lastSyncedAt.value = syncedAt
  }

  function markSyncFailed(error) {
    syncError.value = error?.message || '同步失败，请稍后重试。'
    syncStatus.value = isVersionConflict(error)
      ? SYNC_STATUS.CONFLICT
      : isOfflineError(error)
        ? SYNC_STATUS.OFFLINE
        : SYNC_STATUS.ERROR
  }

  function useLocalMode() {
    remoteDetail.value = null
    remoteMatchId.value = null
    stateVersion.value = null
    syncStatus.value = SYNC_STATUS.LOCAL
    syncError.value = ''
    lastSyncedAt.value = null
  }

  return {
    beginSync,
    createRemoteMatch,
    ageGroups,
    groupRules,
    lastSyncedAt,
    loadMatchDetail,
    loadAgeGroups,
    loadGroupRules,
    loadLatestMatch,
    match,
    markSynced,
    markSyncFailed,
    remoteDetail,
    remoteMatchId,
    clearRemoteMatchRecords,
    revision,
    stateVersion,
    syncError,
    syncMessage,
    syncStatus,
    syncMatchState,
    syncMatchInfo,
    syncMatchRule,
    get: () => match.value,
    replace,
    notify,
    useLocalMode,
  }
})

export function isVersionConflict(error) {
  return error?.status === 409
    || error?.code === 409
    || /比赛状态已经变化|状态版本已经变化|状态版本.*变化/.test(error?.message || '')
}

function isOfflineError(error) {
  return error?.status === 0 || /网络连接失败|请求超时/.test(error?.message || '')
}

function normalizeMatchId(matchId) {
  const value = String(matchId ?? '').trim()
  if (!/^\d+$/.test(value) || value === '0') throw new Error('比赛 ID 不正确。')
  return value
}

function requireDetail(detail) {
  if (!detail?.match || !detail?.rule || !detail?.state) {
    throw new Error('服务器返回的比赛详情不完整。')
  }
  requireVersion(detail.state.stateVersion)
}

function requireVersion(version) {
  if (!Number.isInteger(version) || version < 0) {
    throw new Error('服务器返回的状态版本号不正确。')
  }
}

function requireStateSaveResult(result) {
  if (!result?.state || !Array.isArray(result.events) || !Array.isArray(result.penalties)) {
    throw new Error('服务器返回的状态保存结果不完整。')
  }
  requireVersion(result.state.stateVersion)
}

function mergeById(current = [], changed = []) {
  const values = new Map(current.map(item => [String(item.id), item]))
  for (const item of changed) values.set(String(item.id), item)
  return [...values.values()]
}
