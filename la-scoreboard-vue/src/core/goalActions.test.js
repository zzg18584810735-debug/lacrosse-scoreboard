import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from './matchModel.js'
import { matchRecords } from './matchRecords.js'
import { act } from './scoreboardActions.js'

function liveMatch() {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  match.phase = 'period'
  match.periodStarted = true
  match.running = true
  match.ball = 'live'
  match.possession = 'away'
  return match
}

test('待确认进球在确认后计分并交还球权', () => {
  const match = liveMatch()

  act(match, 'begin-goal', { team: 'home' })

  assert.equal(match.scores.home, 0)
  assert.equal(match.events.length, 0)
  assert.equal(match.sequence, 0)
  assert.equal(match.pendingGoal.team, 'home')
  assert.equal(match.ball, 'dead')
  assert.equal(match.running, false)

  act(match, 'confirm-goal', {
    id: match.pendingGoal.id,
    team: 'home',
    player: '7',
    assist: '8',
  })

  assert.equal(match.scores.home, 1)
  assert.equal(match.possession, 'away')
  assert.equal(match.resetOnResume, true)
  assert.equal(match.pendingGoal, null)
  assert.equal(match.running, false)
  assert.equal(match.events.length, 1)
  assert.equal(match.events[0].category, 'goal')
  assert.equal(matchRecords(match, 'goal')[0].player, '7')
})

test('取消待确认进球不改变比分', () => {
  const match = liveMatch()
  match.clear = 8000
  match.frontcourt = false
  act(match, 'begin-goal', { team: 'away' })

  act(match, 'cancel-goal')

  assert.deepEqual(match.scores, { home: 0, away: 0 })
  assert.equal(match.pendingGoal, null)
  assert.equal(match.running, true)
  assert.equal(match.ball, 'live')
  assert.equal(match.clear, 8000)
  assert.equal(match.events.length, 0)
  assert.equal(match.sequence, 0)
  assert.match(match.notice, /已取消进球录入/)
})

test('刷新后取消服务器恢复的待确认进球仍可重新录入', () => {
  const match = liveMatch()
  match.running = false
  match.ball = 'dead'
  match.pendingGoal = {
    id: 3,
    team: 'home',
    period: 1,
    main: 420000,
    player: '',
    assist: '',
  }

  act(match, 'cancel-goal')

  assert.equal(match.pendingGoal, null)
  assert.equal(match.running, true)
  assert.equal(match.ball, 'live')
})

test('即时进球动作校验球员后更新比分和比赛状态', () => {
  const match = liveMatch()

  act(match, 'goal', { team: 'away', player: '9', assist: '' })

  assert.equal(match.scores.away, 1)
  assert.equal(match.running, false)
  assert.equal(match.ball, 'dead')
  assert.equal(match.possession, 'home')
  assert.equal(matchRecords(match, 'goal')[0].team, 'away')
})

test('无效即时进球不会修改原比赛状态', () => {
  const match = liveMatch()
  const before = structuredClone(match)

  assert.throws(() => act(match, 'goal', { team: 'home', player: '' }), /请填写进球球员/)
  assert.deepEqual(match, before)
})

test('补录已计入比分的进球只增加记录并保持停表', () => {
  const match = liveMatch()
  match.main = 400000
  match.scores.home = 1

  act(match, 'backfill-goal', {
    team: 'home',
    player: '7',
    assist: '',
    scoreMode: 'included',
    period: 1,
    main: 450000,
    reason: '裁判核对纸质记录',
  })

  const goal = matchRecords(match, 'goal')[0]
  assert.equal(match.scores.home, 1)
  assert.equal(match.running, false)
  assert.equal(goal.backfilled, true)
  assert.equal(goal.scoreAdjustment, 0)
  assert.equal(goal.time, '07:30')
})

test('补充进球球员信息保留原进球并新增审计记录', () => {
  const match = liveMatch()
  act(match, 'goal', { team: 'home', player: '待确认', assist: '' })
  const goalId = matchRecords(match, 'goal')[0].id

  act(match, 'goal-details', { id: goalId, player: '7', assist: '8' })

  const goal = matchRecords(match, 'goal')[0]
  assert.equal(goal.id, goalId)
  assert.equal(goal.player, '7')
  assert.equal(goal.assist, '8')
  assert.equal(match.events.at(-1).category, 'goal-detail')
  assert.equal(match.scores.home, 1)
})
