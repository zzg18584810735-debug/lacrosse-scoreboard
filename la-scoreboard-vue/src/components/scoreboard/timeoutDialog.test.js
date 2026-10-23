import assert from 'node:assert/strict'
import test from 'node:test'
import { timeoutDialogContext } from './timeoutDialog.js'

const createMatch = overrides => ({
  phase: 'period',
  period: 1,
  config: { home: '甲队', away: '乙队' },
  rules: { halftimeAfter: 2 },
  ...overrides,
})

test('比赛开始前不提供球队暂停条件', () => {
  const context = timeoutDialogContext(createMatch({ phase: 'ready' }), 'home')
  assert.deepEqual(context.conditions, [])
})

test('中场暂停计入上半场', () => {
  const context = timeoutDialogContext(createMatch({ phase: 'break', period: 2 }), 'away')
  assert.equal(context.halftime, true)
  assert.deepEqual(context.conditions, [{ value: 'halftime', label: '确认中场暂停 · 计入上半场' }])
})

test('比赛节暂停提供前场控球和死球两种裁判条件', () => {
  const context = timeoutDialogContext(createMatch(), 'away')
  assert.deepEqual(context.conditions, [
    { value: 'frontcourt', label: '乙队在进攻区控球，确认暂停' },
    { value: 'dead-ball', label: '死球后球权判给乙队，确认暂停' },
  ])
})
