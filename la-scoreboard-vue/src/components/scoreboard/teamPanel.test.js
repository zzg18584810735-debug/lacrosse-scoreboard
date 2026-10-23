import assert from 'node:assert/strict'
import test from 'node:test'
import {
  canRecordGoal,
  canRecordPenalty,
  canRequestTeamTimeout,
  expelledNumbers,
} from './teamPanel.js'

test('进球只允许在比赛节的活球计时状态录入', () => {
  assert.equal(canRecordGoal({ phase: 'period', running: true, ball: 'live' }), true)
  assert.equal(canRecordGoal({ phase: 'period', running: false, ball: 'live' }), false)
  assert.equal(canRecordGoal({ phase: 'timeout', running: true, ball: 'live' }), false)
})

test('判罚只能在比赛已经开始后记录', () => {
  assert.equal(canRecordPenalty({ phase: 'ready', periodStarted: false }), false)
  assert.equal(canRecordPenalty({ phase: 'period', periodStarted: true }), true)
  assert.equal(canRecordPenalty({ phase: 'timeout', periodStarted: true }), false)
})

test('球队暂停校验阶段和剩余次数', () => {
  const match = {
    phase: 'ready',
    period: 1,
    rules: { halftimeAfter: 2 },
    timeouts: { home: 1, away: 0 },
  }

  assert.equal(canRequestTeamTimeout(match, 'home'), false)
  assert.equal(canRequestTeamTimeout(match, 'away'), false)

  match.phase = 'period'
  match.periodStarted = true
  assert.equal(canRequestTeamTimeout(match, 'home'), true)

  match.phase = 'timeout'
  assert.equal(canRequestTeamTimeout(match, 'home'), false)

  match.phase = 'break'
  match.period = 2
  assert.equal(canRequestTeamTimeout(match, 'home'), true)
})

test('罚退号码按主客队分别展示', () => {
  const match = { disqualified: ['home:12', 'away:8', 'home:21'] }
  assert.deepEqual(expelledNumbers(match, 'home'), ['12', '21'])
  assert.deepEqual(expelledNumbers(match, 'away'), ['8'])
})
