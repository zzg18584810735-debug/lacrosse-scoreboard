import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from './matchModel.js'
import { matchRecords } from './matchRecords.js'
import { act } from './scoreboardActions.js'

function liveSixes() {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  match.phase = 'period'
  match.periodStarted = true
  match.running = true
  match.ball = 'live'
  return match
}

test('比赛钟更正为零后进入本节结束核对', () => {
  const match = liveSixes()
  match.main = 1000

  act(match, 'correct', {
    scope: 'main',
    main: 0,
    reason: '裁判确认本节时间已到',
  })

  assert.equal(match.main, 0)
  assert.equal(match.phase, 'period-end')
  assert.equal(match.running, false)
  assert.match(match.alert, /时间到/)
})

test('更正进攻钟时清除待复赛重置标志并保持停表', () => {
  const match = liveSixes()
  match.resetOnResume = true
  match.shot = 5000

  act(match, 'correct', {
    scope: 'shot',
    shot: 15000,
    reason: '裁判确认复赛时间',
  })

  assert.equal(match.shot, 15000)
  assert.equal(match.resetOnResume, false)
  assert.equal(match.running, false)
  assert.equal(match.ball, 'dead')
  assert.match(match.notice, /复赛进攻时间 30 → 15 秒/)
})

test('比分更正保留原进球记录并生成审计说明', () => {
  const match = liveSixes()
  match.scores = { home: 1, away: 1 }
  match.events.push({ id: 1, category: 'goal', team: 'home', points: 1, periodNumber: 1, text: '主队进球' })
  match.sequence = 1

  act(match, 'correct', {
    scope: 'score',
    home: 2,
    away: 1,
    reason: '裁判确认漏记一分',
  })

  assert.deepEqual(match.scores, { home: 2, away: 1 })
  assert.equal(matchRecords(match, 'goal').length, 1)
  assert.match(match.notice, /原进球明细保留/)
})

test('单条罚时更正使用秒作为表单单位并写回毫秒', () => {
  const match = liveSixes()
  match.penalties.push({
    id: 7,
    team: 'away',
    server: '9',
    remaining: 30000,
    released: false,
  })

  act(match, 'correct', {
    scope: 'penalty',
    penaltyId: 7,
    penaltySeconds: 12.5,
    reason: '裁判确认剩余罚时',
  })

  assert.equal(match.penalties[0].remaining, 12500)
  assert.match(match.notice, /30 → 12.5 秒/)
})

test('本节结束后将比赛钟更正为正数会恢复比赛节并停表', () => {
  const match = liveSixes()
  match.phase = 'period-end'
  match.main = 0
  match.running = false
  match.ball = 'dead'

  act(match, 'correct', {
    scope: 'main',
    main: 10000,
    reason: '裁判确认仍剩十秒',
  })

  assert.equal(match.phase, 'period')
  assert.equal(match.main, 10000)
  assert.equal(match.running, false)
})

test('赛后比分更正重新进入结果核对且不重写进球明细', () => {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  match.phase = 'finished'
  match.period = match.rules.periods
  match.periodStarted = true
  match.main = 0
  match.scores = { home: 2, away: 1 }
  match.events.push({ id: 1, category: 'goal', team: 'home', points: 1, periodNumber: 4, text: '原进球' })
  match.sequence = 1

  act(match, 'final-score', {
    home: 3,
    away: 1,
    reason: '裁判复核纸质记录',
  })

  assert.equal(match.phase, 'period-end')
  assert.deepEqual(match.scores, { home: 3, away: 1 })
  assert.equal(matchRecords(match, 'goal').length, 1)
  assert.equal(match.events.at(-1).category, 'score-correction')
})

test('金球冲突核对保留首个有效进球并作废其他加时进球', () => {
  const match = createMatch({ preset: 'sll-u8', stage: 'knockout', date: '2026-10-21' })
  Object.assign(match, {
    period: 3,
    periodStarted: true,
    phase: 'period-end',
    main: 180000,
    goldenConflict: true,
    scores: { home: 2, away: 1 },
    sequence: 2,
  })
  match.events.push(
    { id: 1, category: 'goal', team: 'home', player: '7', points: 1, periodNumber: 3, period: '加时 1', remaining: 200000, text: '主队进球' },
    { id: 2, category: 'goal', team: 'away', player: '9', points: 1, periodNumber: 3, period: '加时 1', remaining: 180000, text: '客队进球' },
  )

  act(match, 'resolve-golden', {
    goalId: 1,
    home: 2,
    away: 1,
    reason: '裁判确认第一球有效',
  })

  const goals = matchRecords(match, 'goal')
  assert.equal(match.goldenConflict, false)
  assert.equal(match.goldenGoal, true)
  assert.equal(match.phase, 'period-end')
  assert.equal(goals.find(goal => goal.id === 1).voided, undefined)
  assert.equal(goals.find(goal => goal.id === 2).voided, true)
  assert.equal(match.events.at(-1).category, 'goal-ruling')
})

test('裁判确认全部加时进球无效时恢复平局并等待复赛', () => {
  const match = createMatch({ preset: 'sll-u8', stage: 'knockout', date: '2026-10-21' })
  Object.assign(match, {
    period: 3,
    periodStarted: true,
    phase: 'period-end',
    main: 180000,
    goldenConflict: true,
    scores: { home: 2, away: 1 },
    sequence: 1,
  })
  match.events.push({
    id: 1,
    category: 'goal',
    team: 'home',
    player: '7',
    points: 1,
    periodNumber: 3,
    period: '加时 1',
    remaining: 200000,
    text: '主队进球',
  })

  act(match, 'resolve-golden', {
    goalId: 'none',
    home: 1,
    away: 1,
    reason: '裁判确认进球无效',
  })

  assert.equal(match.goldenConflict, false)
  assert.equal(match.goldenGoal, false)
  assert.equal(match.phase, 'period')
  assert.deepEqual(match.scores, { home: 1, away: 1 })
  assert.equal(match.running, false)
})
