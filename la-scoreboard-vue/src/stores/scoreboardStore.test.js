import assert from 'node:assert/strict'
import test from 'node:test'
import { createPinia } from 'pinia'
import { ApiError } from '../api/request.js'
import { createMatch } from '../core/matchModel.js'
import { isVersionConflict, SYNC_STATUS, useScoreboardStore } from './scoreboardStore.js'

test('Pinia store可以接管和替换当前比赛状态', () => {
  const first = createMatch({ home: '主队一', away: '客队一' })
  const store = useScoreboardStore(createPinia())
  store.replace(first)

  assert.equal(store.get(), first)
  assert.equal(store.match.config.home, '主队一')

  const second = createMatch({ home: '主队二', away: '客队二' })
  store.replace(second)

  assert.equal(store.get(), second)
  assert.equal(store.match.config.away, '客队二')
  assert.equal(store.revision, 2)
})

test('计分引擎原地修改状态后可以通知Vue刷新', () => {
  const match = createMatch()
  const store = useScoreboardStore(createPinia())
  store.replace(match)
  const beforeNotify = store.match

  match.scores.home = 1
  store.notify(match)

  assert.equal(store.match.scores.home, 1)
  assert.notEqual(store.match, beforeNotify)
  assert.equal(store.revision, 2)
})

test('Pinia store加载服务器比赛详情并记录状态版本', async () => {
  const store = useScoreboardStore(createPinia())
  const detail = {
    match: { id: 18, title: '测试比赛' },
    rule: { matchId: 18 },
    state: { matchId: 18, stateVersion: 4 },
    events: [],
  }
  const adaptedMatch = createMatch({ title: '服务器比赛' })

  const loaded = await store.loadMatchDetail('18', {
    getMatchDetail: async matchId => {
      assert.equal(matchId, '18')
      return detail
    },
  }, () => adaptedMatch)

  assert.equal(loaded, detail)
  assert.equal(store.remoteDetail, detail)
  assert.equal(store.remoteMatchId, '18')
  assert.equal(store.match, adaptedMatch)
  assert.equal(store.stateVersion, 4)
  assert.equal(store.syncStatus, SYNC_STATUS.SYNCED)
  assert.equal(store.syncMessage, '已与服务器同步')
  assert.equal(typeof store.lastSyncedAt, 'number')
})

test('未指定比赛时读取列表中的最近一场比赛', async () => {
  const store = useScoreboardStore(createPinia())
  const adaptedMatch = createMatch({ title: '最近比赛' })
  const api = {
    listMatches: async () => [{ id: 23 }, { id: 18 }],
    getMatchDetail: async matchId => ({
      match: { id: Number(matchId) },
      rule: { matchId: Number(matchId) },
      state: { matchId: Number(matchId), stateVersion: 2 },
      events: [],
    }),
  }

  const latest = await store.loadLatestMatch(api, () => adaptedMatch)

  assert.equal(latest.id, 23)
  assert.equal(store.remoteMatchId, '23')
  assert.equal(store.match, adaptedMatch)
})

test('服务器没有比赛时返回空结果且不创建虚假远程比赛', async () => {
  const store = useScoreboardStore(createPinia())

  const latest = await store.loadLatestMatch({ listMatches: async () => [] })

  assert.equal(latest, null)
  assert.equal(store.remoteMatchId, null)
})

test('同步成功后更新版本号和最后同步时间', () => {
  const store = useScoreboardStore(createPinia())

  store.beginSync()
  assert.equal(store.syncStatus, SYNC_STATUS.SYNCING)
  store.markSynced(7, 123456)

  assert.equal(store.stateVersion, 7)
  assert.equal(store.lastSyncedAt, 123456)
  assert.equal(store.syncError, '')
})

test('远程状态保存携带当前版本并合并服务器响应', async () => {
  const store = useScoreboardStore(createPinia())
  const match = createMatch()
  store.replace(match)
  store.remoteMatchId = '18'
  store.stateVersion = 4
  store.remoteDetail = { state: { stateVersion: 4 }, events: [], penalties: [] }
  let received

  const result = await store.syncMatchState({ phase: 'PERIOD', events: [], penalties: [] }, {
    saveMatchState: async (matchId, payload) => {
      assert.equal(matchId, '18')
      received = payload
      return {
        state: { stateVersion: 5 },
        events: [],
        penalties: [],
      }
    },
  })

  assert.equal(received.expectedVersion, 4)
  assert.equal(result.state.stateVersion, 5)
  assert.equal(store.stateVersion, 5)
  assert.equal(store.remoteDetail.state.stateVersion, 5)
  assert.equal(store.syncStatus, SYNC_STATUS.SYNCED)
})

test('清空服务器比赛记录携带当前版本并重新载入权威详情', async () => {
  const store = useScoreboardStore(createPinia())
  store.remoteMatchId = '18'
  store.stateVersion = 5
  const adaptedMatch = createMatch({ title: '已清空记录的比赛' })
  let resetPayload
  const api = {
    clearMatchRecords: async (matchId, payload) => {
      assert.equal(matchId, '18')
      resetPayload = payload
    },
    getMatchDetail: async matchId => ({
      match: { id: Number(matchId) },
      rule: { matchId: Number(matchId) },
      state: { matchId: Number(matchId), stateVersion: 6 },
      events: [],
      penalties: [],
    }),
  }

  await store.clearRemoteMatchRecords(api, () => adaptedMatch)

  assert.deepEqual(resetPayload, { expectedVersion: 5 })
  assert.equal(store.match, adaptedMatch)
  assert.equal(store.stateVersion, 6)
  assert.equal(store.syncStatus, SYNC_STATUS.SYNCED)
})

test('分别加载组别字典和规则模板并在创建后切换到服务器比赛', async () => {
  const store = useScoreboardStore(createPinia())
  const adaptedMatch = createMatch({ title: '新比赛' })
  const api = {
    listAgeGroups: async () => [{ itemValue: 'U12', itemLabel: 'U12组', sortNo: 1 }],
    listGroupRules: async () => [{ id: 6, ageGroup: 'U12' }],
    createMatch: async payload => {
      assert.equal(payload.groupRuleId, 6)
      return 19
    },
    getMatchDetail: async matchId => ({
      match: { id: Number(matchId) },
      rule: { matchId: Number(matchId) },
      state: { matchId: Number(matchId), stateVersion: 0 },
      events: [],
    }),
  }

  await store.loadAgeGroups(api)
  await store.loadGroupRules(api)
  await store.createRemoteMatch({ groupRuleId: 6 }, api, () => adaptedMatch)

  assert.equal(store.ageGroups[0].itemValue, 'U12')
  assert.equal(store.groupRules[0].ageGroup, 'U12')
  assert.equal(store.remoteMatchId, '19')
  assert.equal(store.match, adaptedMatch)
})

test('网络失败进入离线状态且不覆盖本地比赛', async () => {
  const store = useScoreboardStore(createPinia())
  const localMatch = createMatch()
  store.replace(localMatch)

  await assert.rejects(() => store.loadMatchDetail(9, {
    getMatchDetail: async () => { throw new ApiError('网络连接失败，请稍后重试') },
  }), /网络连接失败/)

  assert.equal(store.match, localMatch)
  assert.equal(store.syncStatus, SYNC_STATUS.OFFLINE)
  assert.match(store.syncError, /网络连接失败/)
})

test('状态版本冲突会锁定后续同步直到重新加载', () => {
  const store = useScoreboardStore(createPinia())
  const error = new ApiError('比赛状态已经变化，请刷新后重试', { status: 400, code: 1006 })

  store.markSyncFailed(error)

  assert.equal(isVersionConflict(error), true)
  assert.equal(store.syncStatus, SYNC_STATUS.CONFLICT)
  assert.throws(() => store.beginSync(), /请先刷新比赛详情/)
})

test('不完整的服务器详情进入失败状态', async () => {
  const store = useScoreboardStore(createPinia())

  await assert.rejects(() => store.loadMatchDetail(3, {
    getMatchDetail: async () => ({ match: { id: 3 } }),
  }), /比赛详情不完整/)

  assert.equal(store.syncStatus, SYNC_STATUS.ERROR)
})
