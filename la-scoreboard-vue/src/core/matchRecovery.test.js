import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from './matchModel.js'
import { restoreMatch } from './matchRecovery.js'

test('恢复比赛时复制缓存并停止所有运行时钟', () => {
  const cached = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  Object.assign(cached, {
    phase: 'period',
    periodStarted: true,
    running: true,
    auxiliaryRunning: true,
    undo: { sequence: 0 },
  })

  const restored = restoreMatch(cached)

  assert.notEqual(restored, cached)
  assert.equal(restored.running, false)
  assert.equal(restored.auxiliaryRunning, false)
  assert.equal(restored.undo, null)
  assert.match(restored.notice, /所有时钟暂停/)
  assert.equal(cached.running, true)
})

test('恢复比赛规则不受对象字段顺序影响', () => {
  const cached = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  cached.rules = Object.fromEntries(Object.entries(cached.rules).reverse())

  const restored = restoreMatch(cached)

  assert.equal(restored.rules.id, 'sixes-cn-2026-v1')
  assert.equal(restored.rules.period, 8 * 60 * 1000)
})

test('恢复比赛拒绝字段缺失的规则快照', () => {
  const cached = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  delete cached.rules.points

  assert.throws(() => restoreMatch(cached), /规则字段损坏/)
})

test('恢复比赛拒绝损坏的事件记录', () => {
  const cached = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  cached.events.push({ id: 1, text: '缺少时间和节次' })
  cached.sequence = 1

  assert.throws(() => restoreMatch(cached), /比赛记录损坏/)
})
