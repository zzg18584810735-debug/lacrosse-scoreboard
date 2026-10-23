import assert from 'node:assert/strict'
import test from 'node:test'
import { createScoreboardPersistence } from '../storage/scoreboardPersistence.js'
import { ScoreboardEngine } from './scoreboardEngine.js'

function createStore() {
  let state
  let notifications = 0
  return {
    get: () => state,
    replace: value => { state = value },
    notify: () => { notifications += 1 },
    notificationCount: () => notifications,
  }
}

test('计分引擎刷新状态时只通知Vue状态仓库', () => {
  const storage = { getItem: () => null, setItem() {}, removeItem() {} }
  const store = createStore()
  const persistence = createScoreboardPersistence(storage)
  const engine = new ScoreboardEngine(store, persistence)

  engine.render()

  assert.equal(store.notificationCount(), 1)
})

test('鸣哨后每次计时推进都会通知Vue刷新倒计时', () => {
  const storage = { getItem: () => null, setItem() {}, removeItem() {} }
  const store = createStore()
  const persistence = createScoreboardPersistence(storage)
  const engine = new ScoreboardEngine(store, persistence)

  engine.dispatch('possession', { team: 'home' })
  engine.dispatch('resume')
  const before = engine.state.main
  const notifications = store.notificationCount()
  engine.last = performance.now() - 1000
  engine.settle()

  assert.ok(engine.state.main < before)
  assert.equal(store.notificationCount(), notifications + 1)
})

test('球队暂停倒计时归零后自动结束并记录对应球队', () => {
  const storage = { getItem: () => null, setItem() {}, removeItem() {} }
  const store = createStore()
  const persistence = createScoreboardPersistence(storage)
  const engine = new ScoreboardEngine(store, persistence)

  engine.dispatch('possession', { team: 'home' })
  engine.dispatch('resume')
  engine.dispatch('confirm-timeout', { team: 'home', condition: 'frontcourt' })
  engine.state.auxiliary = 1
  engine.last = performance.now() - 10

  engine.settle()

  assert.equal(engine.state.phase, 'period')
  assert.equal(engine.state.timeoutContext, null)
  assert.equal(engine.state.auxiliaryRunning, false)
  assert.equal(engine.state.events.at(-1).title, '红队球队暂停结束')
  assert.equal(engine.state.events.at(-1).team, 'home')
  assert.match(engine.state.events.at(-1).text, /等待裁判鸣哨复赛/)
})

test('损坏的比赛缓存不会中断计分页面启动', () => {
  const messages = []
  const storage = {
    getItem: () => JSON.stringify({ version: 4, rules: {} }),
    setItem() { throw new Error('损坏缓存被锁定后不应覆盖') },
    removeItem() {},
  }
  const store = createStore()
  const persistence = createScoreboardPersistence(storage, {
    onMessage: message => messages.push(message),
  })

  const engine = new ScoreboardEngine(store, persistence)

  assert.equal(engine.state.version, 4)
  assert.equal(engine.state.phase, 'ready')
  assert.equal(persistence.blocked, true)
  assert.match(messages[0], /缓存无法读取或保存/)
})

test('比赛操作完成后通过串行队列保存远程状态和新增事件', async () => {
  const storage = { getItem: () => null, setItem() {}, removeItem() {} }
  const store = createStore()
  const requests = []
  store.remoteMatchId = '10'
  store.remoteDetail = { roster: [] }
  store.syncMatchState = async payload => {
    requests.push(payload)
    return { state: { stateVersion: requests.length }, events: [], penalties: [] }
  }
  const engine = new ScoreboardEngine(store, createScoreboardPersistence(storage))

  engine.dispatch('possession', { team: 'home' })
  await engine.waitForSync()
  requests.length = 0
  engine.dispatch('resume')
  await engine.waitForSync()

  assert.equal(requests.length, 1)
  assert.equal(requests[0].phase, 'PERIOD')
  assert.equal(requests[0].events.length, 1)
  assert.equal(requests[0].events[0].eventType, 'MATCH_START')
})
