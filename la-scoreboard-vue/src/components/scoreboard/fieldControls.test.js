import assert from 'node:assert/strict'
import test from 'node:test'
import {
  canSetPossession,
  canSetAlternate,
  clearProgressLabel,
  isFieldActive,
  otherTeam,
} from './fieldControls.js'

test('场上操作只在比赛节阶段启用', () => {
  assert.equal(isFieldActive({ phase: 'period' }), true)
  assert.equal(isFieldActive({ phase: 'ready' }), false)
})

test('准备阶段可确认开场球权，其他场上操作仍保持禁用', () => {
  assert.equal(canSetPossession({ phase: 'ready', main: 480000 }), true)
  assert.equal(canSetPossession({ phase: 'period', main: 470000 }), true)
  assert.equal(canSetPossession({ phase: 'break', main: 0 }), false)
  assert.equal(canSetPossession({ phase: 'ready', main: 0 }), false)
})

test('交替球权可在准备、比赛节和休息阶段更正', () => {
  assert.equal(canSetAlternate({ phase: 'ready' }), true)
  assert.equal(canSetAlternate({ phase: 'break' }), true)
  assert.equal(canSetAlternate({ phase: 'finished' }), false)
  assert.equal(otherTeam('home'), 'away')
  assert.equal(otherTeam('away'), 'home')
})

test('推进状态显示当前业务含义', () => {
  const match = { frontcourt: false, clear: null, rules: { clearMs: 10_000 } }
  assert.equal(clearProgressLabel(match), '10 秒辅助计时')
  match.clear = 5_000
  assert.equal(clearProgressLabel(match), '推进中')
  match.frontcourt = true
  assert.equal(clearProgressLabel(match), '已在前场')
})
