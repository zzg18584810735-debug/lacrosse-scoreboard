import assert from 'node:assert/strict'
import test from 'node:test'
import {
  activeCorrectionPenalties,
  correctionPenaltySeconds,
  correctionScopes,
  createMatchCorrectionDraft,
} from './matchCorrectionDialog.js'

const createMatch = overrides => ({
  phase: 'period', main: 125999, shot: 14201, resetOnResume: false, period: 1,
  scores: { home: 2, away: 3 }, penalties: [],
  rules: { periods: 4, period: 480000, overtime: 180000, shot: 30000 },
  ...overrides,
})

test('比赛阶段提供时间、进攻时间、比分和罚时更正', () => {
  assert.deepEqual(correctionScopes(createMatch()).map(scope => scope.value), ['main', 'shot', 'score', 'penalty'])
})

test('节间只允许更正比分或罚时', () => {
  assert.deepEqual(correctionScopes(createMatch({ phase: 'break' })).map(scope => scope.value), ['score', 'penalty'])
})

test('比赛更正草稿按页面单位转换当前时间', () => {
  assert.deepEqual(createMatchCorrectionDraft(createMatch()), {
    scope: 'main', minutes: 2, seconds: 5.999, shot: 14.201, home: 2, away: 3,
    penaltyId: '', penaltySeconds: '', reason: '', maxMinutes: 8,
  })
})

test('罚时更正只列出未释放记录并带入向上取整的剩余秒数', () => {
  const match = createMatch({ penalties: [
    { id: 1, remaining: 12501, released: false },
    { id: 2, remaining: 1000, released: true },
  ] })
  assert.deepEqual(activeCorrectionPenalties(match).map(item => item.id), [1])
  assert.equal(correctionPenaltySeconds(match, 1), 12.501)
})
