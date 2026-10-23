import assert from 'node:assert/strict'
import test from 'node:test'
import { ScoreboardEngine } from '../engine/scoreboardEngine.js'
import { createScoreboardPersistence } from '../storage/scoreboardPersistence.js'
import { createScoreboardController } from './useScoreboardController.js'

function createStore() {
  let state
  return {
    get: () => state,
    replace: value => { state = value },
    notify() {},
  }
}

function createController() {
  const storage = { getItem: () => null, setItem() {}, removeItem() {} }
  const persistence = createScoreboardPersistence(storage)
  const engine = new ScoreboardEngine(createStore(), persistence)
  engine.settle = () => {}
  engine.render = () => {}
  return { controller: createScoreboardController(engine, persistence), engine }
}

test('Vue控制器用结构化参数派发界面动作', () => {
  const { controller, engine } = createController()

  const effect = controller.performAction('record-filter', { filter: 'goal' })

  assert.deepEqual(effect, { recordFilter: 'goal' })
  assert.equal('recordFilter' in engine, false)
})

test('Vue控制器返回导出意图而不操作DOM', () => {
  const { controller } = createController()

  const effect = controller.performAction('export-json')

  assert.equal(effect.download.type, 'application/json')
  assert.match(effect.download.filename, /\.json$/)
  assert.match(effect.download.content, /"config"/)
})

test('打开判罚弹窗只停表不生成裁判暂停记录', () => {
  const { controller, engine } = createController()
  Object.assign(engine.state, {
    phase: 'period',
    periodStarted: true,
    running: true,
    ball: 'live',
  })
  engine.state.rules.foulStop = true

  const effect = controller.performAction('penalty-form', { team: 'home', kind: 'major' })

  assert.equal(engine.state.running, false)
  assert.equal(engine.state.events.length, 0)
  assert.equal(effect.dialog, 'penalty')
})

test('Vue控制器用业务类型和结构化草稿提交表单', () => {
  const { controller, engine } = createController()

  controller.submitForm('match-info', {
    title: '测试比赛',
    date: '2026-10-21',
    home: '主队甲',
    away: '客队乙',
  }, { dialog: 'match-info' })

  assert.equal(engine.state.config.title, '测试比赛')
  assert.equal(engine.state.config.home, '主队甲')
  assert.equal('dialog' in engine, false)
})

test('确认进球等待状态和进球事件同步完成后再结束提交', async () => {
  const { controller, engine } = createController()
  Object.assign(engine.state, {
    phase: 'period',
    periodStarted: true,
    running: true,
    ball: 'live',
  })
  controller.performAction('goal', { team: 'home' })
  const goalId = engine.state.pendingGoal.id
  let finishSync
  engine.commit = () => new Promise(resolve => { finishSync = resolve })

  let submitted = false
  const submission = controller.submitForm('goal-entry', {
    goalId,
    team: 'home',
    player: '7',
    assist: '',
  }, { dialog: 'goal-entry' }).then(() => { submitted = true })

  await Promise.resolve()
  assert.equal(submitted, false)
  assert.equal(engine.state.events.at(-1).category, 'goal')

  finishSync()
  await submission
  assert.equal(submitted, true)
})

test('比赛时间更正为零时进入本节结束核对', () => {
  const { controller, engine } = createController()
  Object.assign(engine.state, {
    phase: 'period',
    periodStarted: true,
    main: 1000,
  })

  controller.submitForm('correct', {
    scope: 'main',
    minutes: 0,
    seconds: 0,
    reason: '裁判确认时间到',
  }, { dialog: 'correct' })

  assert.equal(engine.state.phase, 'period-end')
  assert.equal(engine.state.main, 0)
  assert.match(engine.state.alert, /时间到/)
})

test('清空比赛记录入口只返回确认弹窗意图', () => {
  const { controller } = createController()

  assert.deepEqual(controller.performAction('clear-match-records-form'), {
    dialog: 'clear-match-records',
    editingRules: false,
    penaltyDefaults: undefined,
  })
})

test('确认清空记录时等待既有同步并调用服务器处理', async () => {
  const { controller, engine } = createController()
  const calls = []
  engine.stateStore.remoteMatchId = '18'
  engine.stateStore.clearRemoteMatchRecords = async () => { calls.push('clear') }
  engine.waitForSync = async () => { calls.push('wait') }

  await controller.submitForm('clear-match-records', { confirm: true }, { dialog: 'clear-match-records' })

  assert.deepEqual(calls, ['wait', 'clear'])
})
