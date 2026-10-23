import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from './matchModel.js'
import { act } from './scoreboardActions.js'

function createSixes() {
  return createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
}

test('普通节结束后进入节间休息', () => {
  const match = createSixes()
  match.phase = 'period-end'
  match.periodStarted = true
  match.main = 0

  act(match, 'next')

  assert.equal(match.phase, 'break')
  assert.equal(match.auxiliary, match.rules.breakMs)
  assert.equal(match.auxiliaryRunning, true)
  assert.match(match.notice, /下一节交换攻守方向/)
})

test('中场位置使用中场休息时长', () => {
  const match = createSixes()
  match.phase = 'period-end'
  match.periodStarted = true
  match.period = match.rules.halftimeAfter
  match.main = 0

  act(match, 'next')

  assert.equal(match.phase, 'break')
  assert.equal(match.auxiliary, match.rules.halftimeMs)
})

test('末节已有胜负时确认比赛结束', () => {
  const match = createSixes()
  match.phase = 'period-end'
  match.periodStarted = true
  match.period = match.rules.periods
  match.main = 0
  match.scores = { home: 3, away: 2 }

  act(match, 'next')

  assert.equal(match.phase, 'finished')
  assert.equal(match.alert, '')
  assert.match(match.notice, /比赛结束/)
  assert.equal(match.events.at(-1).eventType, 'MATCH_END')
})

test('淘汰赛平局在常规时间后进入加时休息', () => {
  const match = createMatch({ preset: 'sll-u8', stage: 'knockout', date: '2026-10-21' })
  match.phase = 'period-end'
  match.periodStarted = true
  match.period = match.rules.periods
  match.main = 0
  match.scores = { home: 1, away: 1 }

  act(match, 'next')

  assert.equal(match.phase, 'break')
  assert.equal(match.auxiliary, match.rules.overtimeBreak)
  assert.match(match.notice, /金球加时/)
})

test('准备普通下一节时重新争球并重置比赛状态', () => {
  const match = createSixes()
  match.phase = 'break'
  match.period = 1
  match.possession = 'home'
  match.shot = 9000
  match.frontcourt = true
  match.clear = 4000

  act(match, 'prepare', { carry: false })

  assert.equal(match.period, 2)
  assert.equal(match.main, match.rules.period)
  assert.equal(match.phase, 'ready')
  assert.equal(match.periodStarted, false)
  assert.equal(match.possession, null)
  assert.equal(match.shot, match.rules.shot)
  assert.equal(match.frontcourt, false)
  assert.equal(match.clear, null)
})

test('裁判确认延续球权时保留球权和进攻剩余时间', () => {
  const match = createSixes()
  match.phase = 'break'
  match.period = 1
  match.possession = 'away'
  match.shot = 11000

  act(match, 'prepare', { carry: true })

  assert.equal(match.possession, 'away')
  assert.equal(match.shot, 11000)
  assert.match(match.notice, /延续球权及进攻剩余时间/)
})

test('中场后重置暂停额度并要求未完成罚时重新就座', () => {
  const match = createSixes()
  match.phase = 'break'
  match.period = match.rules.halftimeAfter
  match.timeouts = { home: 0, away: 0 }
  match.penalties.push({ id: 1, remaining: 30000, seated: true, started: true, released: false })

  act(match, 'prepare', { carry: false })

  assert.equal(match.period, match.rules.halftimeAfter + 1)
  assert.deepEqual(match.timeouts, {
    home: match.rules.timeoutsPerHalf,
    away: match.rules.timeoutsPerHalf,
  })
  assert.equal(match.penalties[0].seated, false)
  assert.equal(match.penalties[0].started, false)
})

test('首次进入加时按规则重置时长、暂停和交替球权', () => {
  const match = createMatch({ preset: 'sll-u8', stage: 'knockout', date: '2026-10-21' })
  match.phase = 'break'
  match.period = match.rules.periods
  match.scores = { home: 1, away: 1 }
  match.alternate = 'home'

  act(match, 'prepare', { carry: false })

  assert.equal(match.period, match.rules.periods + 1)
  assert.equal(match.main, match.rules.overtime)
  assert.deepEqual(match.timeouts, {
    home: match.rules.overtimeTimeouts,
    away: match.rules.overtimeTimeouts,
  })
  assert.equal(match.alternate, match.rules.alternateFirst)
  assert.equal(match.phase, 'ready')
})

test('没有球权时不能选择延续球权', () => {
  const match = createSixes()
  match.phase = 'break'
  const before = structuredClone(match)

  assert.throws(() => act(match, 'prepare', { carry: true }), /没有可延续的球权/)
  assert.deepEqual(match, before)
})
