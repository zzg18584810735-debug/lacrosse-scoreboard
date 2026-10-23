import assert from 'node:assert/strict'
import test from 'node:test'
import { createEmptyMatch, createMatch, validateMatchDate } from './matchModel.js'

test('比赛模型按新建比赛的本场规则配置开场和交替球权', () => {
  const match = createMatch({ preset: 'sll-u8', date: '2026-10-21' })

  assert.equal(match.main, 8 * 60 * 1000)
  assert.equal(match.shot, 0)
  assert.equal(match.timeouts.home, 1)
  assert.equal(match.possession, 'away')
  assert.equal(match.alternate, 'away')
})

test('新建比赛配置由裁判确认首次球权时不预设球队', () => {
  const match = createMatch({
    preset: 'sll-sixes',
    date: '2026-10-21',
    rules: { alternateFirst: 'manual' },
  })

  assert.equal(match.possession, null)
  assert.equal(match.alternate, null)
})

test('2027年起U12比赛创建时要求选择男女组', () => {
  assert.throws(() => createMatch({ preset: 'sll-u12', date: '2027-01-01' }), /请选择男子组或女子组/)
})

test('比赛模型统一校验比赛日期', () => {
  assert.doesNotThrow(() => validateMatchDate('2026-10-21'))
  assert.throws(() => validateMatchDate('2026-02-30'), /请选择有效比赛日期/)
})

test('没有比赛时创建全部停表且数值归零的页面占位状态', () => {
  const match = createEmptyMatch()

  assert.equal(match.config.title, '暂无比赛')
  assert.equal(match.main, 0)
  assert.equal(match.shot, 0)
  assert.equal(match.clear, 0)
  assert.equal(match.rules.shot, 0)
  assert.equal(match.rules.clearMs, 0)
  assert.deepEqual(match.scores, { home: 0, away: 0 })
  assert.deepEqual(match.timeouts, { home: 0, away: 0 })
  assert.equal(match.running, false)
  assert.equal(match.possession, null)
  assert.equal(match.auxiliaryRunning, false)
})
