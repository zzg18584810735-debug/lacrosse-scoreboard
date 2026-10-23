import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from './matchModel.js'
import { overtimeGoals, reconcileMatch } from './matchOutcome.js'

test('多条加时金球记录进入冲突核对状态', () => {
  const match = createMatch({ preset: 'sll-u8', stage: 'knockout', date: '2026-10-21' })
  Object.assign(match, {
    period: 3,
    periodStarted: true,
    phase: 'period-end',
    goldenGoal: true,
    scores: { home: 2, away: 0 },
  })
  match.events.push(
    { id: 1, category: 'goal', team: 'home', points: 1, periodNumber: 3, period: '加时 1', time: '03:00', text: '第一球' },
    { id: 2, category: 'goal', team: 'home', points: 1, periodNumber: 3, period: '加时 1', time: '02:00', text: '第二球' },
  )

  reconcileMatch(match)

  assert.equal(overtimeGoals(match).length, 2)
  assert.equal(match.goldenConflict, true)
  assert.equal(match.goldenGoal, false)
  assert.match(match.alert, /金球进球记录有冲突/)
})
