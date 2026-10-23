import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from './matchModel.js'
import { matchRecords } from './matchRecords.js'
import { act } from './scoreboardActions.js'

function createSixes() {
  return createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
}

function liveMatch() {
  const match = createSixes()
  match.phase = 'period'
  match.periodStarted = true
  match.running = true
  match.ball = 'live'
  match.possession = 'away'
  return match
}

test('前场控球暂停原子更新球权、额度和暂停上下文', () => {
  const match = liveMatch()
  const before = match.timeouts.home

  act(match, 'confirm-timeout', { team: 'home', condition: 'frontcourt' })

  const event = matchRecords(match, 'timeout')[0]
  assert.equal(match.phase, 'timeout')
  assert.equal(match.possession, 'home')
  assert.equal(match.frontcourt, true)
  assert.equal(match.running, false)
  assert.equal(match.timeouts.home, before - 1)
  assert.equal(match.timeoutContext.phase, 'period')
  assert.equal(match.timeoutContext.timeoutId, event.id)
  assert.equal(event.condition, 'frontcourt')
  assert.equal(event.durationSeconds, 60)
  assert.equal(event.title, '红队暂停（60秒）')
  assert.match(event.text, /60 秒球队暂停/)
  assert.match(event.text, /本队在进攻区控球/)
  assert.equal(match.events.length, 1)
  assert.deepEqual(match.events.map(item => item.eventType), ['TEAM_TIMEOUT'])
})

test('无效暂停条件不会污染比赛状态', () => {
  const match = liveMatch()
  const before = structuredClone(match)

  assert.throws(
    () => act(match, 'confirm-timeout', { team: 'home', condition: 'halftime' }),
    /选择暂停条件/,
  )
  assert.deepEqual(match, before)
})

test('比赛开始前不能申请球队暂停', () => {
  const match = createSixes()
  const before = structuredClone(match)

  assert.throws(
    () => act(match, 'confirm-timeout', { team: 'home', condition: 'before-start' }),
    /比赛尚未开始/,
  )
  assert.deepEqual(match, before)
})

test('中场暂停结束后恢复原休息时间并保持休息钟暂停', () => {
  const match = createSixes()
  match.phase = 'break'
  match.period = match.rules.halftimeAfter
  match.auxiliary = 90000
  match.auxiliaryRunning = true

  act(match, 'confirm-timeout', { team: 'away', condition: 'halftime' })
  act(match, 'end-timeout')

  assert.equal(match.phase, 'break')
  assert.equal(match.auxiliary, 90000)
  assert.equal(match.auxiliaryRunning, false)
  assert.match(match.notice, /回到中场休息/)
  assert.equal(match.events.at(-1).title, '蓝队球队暂停结束')
  assert.equal(match.events.at(-1).team, 'away')
})

test('暂停更正为另一队时同步恢复和扣减当前额度', () => {
  const match = liveMatch()
  const initial = { ...match.timeouts }
  act(match, 'confirm-timeout', { team: 'home', condition: 'frontcourt' })
  const timeoutId = matchRecords(match, 'timeout')[0].id

  act(match, 'correct-timeout', {
    id: timeoutId,
    mode: 'edit',
    team: 'away',
    correctionReason: '裁判确认申请方记录错误',
  })

  assert.equal(match.timeouts.home, initial.home)
  assert.equal(match.timeouts.away, initial.away - 1)
  assert.equal(matchRecords(match, 'timeout')[0].team, 'away')
  assert.equal(match.events.at(-1).category, 'timeout-correction')
})

test('作废当前暂停时恢复原阶段和暂停额度', () => {
  const match = liveMatch()
  const initial = match.timeouts.home
  act(match, 'confirm-timeout', { team: 'home', condition: 'frontcourt' })
  const timeoutId = matchRecords(match, 'timeout')[0].id

  act(match, 'correct-timeout', {
    id: timeoutId,
    mode: 'cancel',
    correctionReason: '裁判确认未准予暂停',
  })

  assert.equal(match.phase, 'period')
  assert.equal(match.timeouts.home, initial)
  assert.equal(match.timeoutContext, null)
  assert.equal(matchRecords(match, 'timeout')[0].voided, true)
  assert.equal(match.events.at(-1).category, 'timeout-correction')
})

test('已经处于球队暂停时不能再次申请暂停', () => {
  const match = liveMatch()
  act(match, 'confirm-timeout', { team: 'home', condition: 'frontcourt' })
  const before = structuredClone(match)

  assert.throws(
    () => act(match, 'confirm-timeout', { team: 'away', condition: 'dead-ball' }),
    /当前正在暂停/,
  )
  assert.deepEqual(match, before)
})
