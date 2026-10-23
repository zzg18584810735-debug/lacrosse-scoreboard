import assert from 'node:assert/strict'
import test from 'node:test'
import {
  canPauseClock,
  canResumeClock,
  pendingPenaltyTasks,
} from './matchClockControls.js'

test('罚时待办只包含未就座或已经到时且未释放的记录', () => {
  const waiting = { id: 1, seated: false, remaining: 30_000, released: false }
  const active = { id: 2, seated: true, remaining: 20_000, released: false }
  const expired = { id: 3, seated: true, remaining: 0, released: false }
  const released = { id: 4, seated: true, remaining: 0, released: true }

  assert.deepEqual(pendingPenaltyTasks({ penalties: [waiting, active, expired, released] }), [waiting, expired])
})

test('鸣哨和暂停按钮遵守当前比赛阶段与活球状态', () => {
  assert.equal(canResumeClock({ phase: 'ready', running: false, ball: 'dead' }), true)
  assert.equal(canResumeClock({ phase: 'period', running: true, ball: 'live' }), false)
  assert.equal(canPauseClock({ phase: 'ready', periodStarted: false, running: false }), false)
  assert.equal(canPauseClock({ phase: 'period', periodStarted: true, running: true }), true)
  assert.equal(canPauseClock({ phase: 'period', periodStarted: true, running: false }), false)
  assert.equal(canPauseClock({ phase: 'break', periodStarted: true, running: false }), false)
})
