import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from './matchModel.js'
import { act } from './scoreboardActions.js'

test('修改比赛基础信息会去除空白并保留规则比分和计时状态', () => {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  match.scores.home = 2
  match.main = 123000
  const rules = structuredClone(match.rules)

  act(match, 'match-info', {
    title: '  秋季决赛  ',
    date: '2026-10-22',
    home: '  红队  ',
    away: '  蓝队  ',
  })

  assert.equal(match.config.title, '秋季决赛')
  assert.equal(match.config.date, '2026-10-22')
  assert.equal(match.config.home, '红队')
  assert.equal(match.config.away, '蓝队')
  assert.deepEqual(match.scores, { home: 2, away: 0 })
  assert.equal(match.main, 123000)
  assert.deepEqual(match.rules, rules)
  assert.equal(match.events.at(-1).category, 'match-info')
})

test('比赛基础信息没有变化时不修改原状态', () => {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  const before = structuredClone(match)

  assert.throws(() => act(match, 'match-info', match.config), /比赛信息没有变化/)
  assert.deepEqual(match, before)
})

test('U12比赛不能通过信息修改跨越2027年分组边界', () => {
  const match = createMatch({ preset: 'sll-u12', date: '2026-12-31' })

  assert.throws(() => act(match, 'match-info', {
    ...match.config,
    date: '2027-01-01',
  }), /男女分组变化/)
})
