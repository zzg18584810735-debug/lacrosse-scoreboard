import assert from 'node:assert/strict'
import test from 'node:test'
import { actionAllowedWithoutMatch, actionUnavailable, editable, readCorrection } from './actionPolicy.js'
import { createMatch } from './matchModel.js'

test('动作可用性按比赛阶段和活球状态判断', () => {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })

  assert.equal(actionUnavailable(match, 'goal'), '请先鸣哨开赛。')
  assert.equal(actionUnavailable(match, 'pause'), '请先鸣哨开赛。')
  assert.equal(actionUnavailable(match, 'timeout', { team: 'home' }), '请先鸣哨开赛。')
  assert.equal(actionUnavailable(match, 'penalty-form'), '请先鸣哨开赛。')
  Object.assign(match, { phase: 'period', periodStarted: true, running: true, ball: 'live' })
  assert.equal(actionUnavailable(match, 'goal'), '')
  assert.equal(actionUnavailable(match, 'pause'), '')
  assert.equal(actionUnavailable(match, 'timeout', { team: 'home' }), '')
  assert.equal(actionUnavailable(match, 'penalty-form'), '')
  match.running = false
  assert.match(actionUnavailable(match, 'pause'), /已经暂停/)
  match.phase = 'timeout'
  assert.match(actionUnavailable(match, 'timeout', { team: 'home' }), /先结束球队暂停/)
})

test('没有比赛时只允许新建比赛和页面级操作', () => {
  assert.equal(actionAllowedWithoutMatch('setup'), true)
  assert.equal(actionAllowedWithoutMatch('fullscreen'), true)
  assert.equal(actionAllowedWithoutMatch('view'), true)
  assert.equal(actionAllowedWithoutMatch('resume'), false)
  assert.equal(actionAllowedWithoutMatch('export'), false)
})

test('金球冲突期间只开放核对及安全操作', () => {
  const match = createMatch({ preset: 'sll-u8', date: '2026-10-21' })
  match.goldenConflict = true

  assert.match(actionUnavailable(match, 'goal'), /金球进球记录有冲突/)
  assert.equal(actionUnavailable(match, 'resolve-golden-form'), '')
})

test('只有首节待开赛且没有比赛操作时可以修改规则', () => {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  assert.equal(editable(match), true)

  match.events.push({ category: 'match-info' })
  assert.equal(editable(match), true)

  match.events.push({ category: 'system' })
  assert.equal(editable(match), false)
})

test('比赛更正表单统一换算时间参数', () => {
  assert.deepEqual(readCorrection({
    scope: 'main',
    minutes: '1',
    seconds: '1.5',
    reason: '裁判确认',
  }), {
    scope: 'main',
    reason: '裁判确认',
    main: 61500,
  })
  assert.throws(() => readCorrection({ scope: 'main', minutes: '1.5', seconds: '0' }), /非负整数分钟/)
})
