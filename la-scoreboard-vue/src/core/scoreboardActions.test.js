import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from './matchModel.js'
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

test('未知或校验失败的动作不会部分修改原状态', () => {
  const match = liveMatch()
  const before = structuredClone(match)

  assert.throws(() => act(match, 'unknown-action'), /未知操作/)
  assert.deepEqual(match, before)
  assert.throws(() => act(match, 'goal', { team: 'home', player: '' }), /请填写进球球员/)
  assert.deepEqual(match, before)
})

test('待确认进球期间只允许确认或取消该进球', () => {
  const match = liveMatch()
  act(match, 'begin-goal', { team: 'home' })
  const before = structuredClone(match)

  assert.throws(() => act(match, 'possession', { team: 'away' }), /请先确认或取消/)
  assert.deepEqual(match, before)
})

test('撤销恢复上一步状态并保留已作废记录和撤销审计', () => {
  const match = liveMatch()
  act(match, 'goal', { team: 'home', player: '7', assist: '8' })
  const goalId = match.events.at(-1).id

  act(match, 'undo')

  assert.deepEqual(match.scores, { home: 0, away: 0 })
  assert.equal(match.possession, 'away')
  assert.equal(match.running, false)
  assert.equal(match.undo, null)
  assert.equal(match.events.find(event => event.id === goalId).voided, true)
  assert.match(match.events.at(-1).text, /已撤销上一步/)
  assert.equal(match.events.at(-1).id, goalId + 1)
})
