import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from './matchModel.js'
import {
  correctionRecord,
  penaltyData,
  rebuildDiscipline,
  recordGoal,
  recordMatchEvent,
  recordTimeoutBucket,
  timeoutBucket,
} from './matchEvents.js'

test('统一事件写入记录编号、节次、时间和规则依据', () => {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  match.main = 479001

  recordMatchEvent(match, '测试事件', '5.1', { category: 'test' })

  assert.equal(match.sequence, 1)
  assert.deepEqual(match.events[0], {
    id: 1,
    period: '第 1 节',
    periodNumber: 1,
    time: '08:00',
    remaining: 479001,
    text: '测试事件',
    rule: '5.1',
    category: 'test',
    title: '测试事件',
  })
  assert.equal(match.notice, '测试事件')
  assert.equal(match.rule, '5.1')
})

test('进球记录清理球员信息并保留实际发生时间', () => {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })

  recordGoal(match, { team: 'home', player: ' 7 ', assist: ' 8 ' }, { period: 2, main: 420000 })

  assert.equal(match.events[0].category, 'goal')
  assert.equal(match.events[0].player, '7')
  assert.equal(match.events[0].assist, '8')
  assert.equal(match.events[0].period, '第 2 节')
  assert.equal(match.events[0].time, '07:00')
  assert.throws(
    () => recordGoal(match, { team: 'home', player: '', assist: '' }, { period: 2, main: 420000 }),
    /请填写进球球员/,
  )
})

test('判罚数据校验代罚人员和重复罚时', () => {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })

  assert.throws(
    () => penaltyData(match, { team: 'home', player: '教练', subject: 'official', kind: 'major' }),
    /请填写另一名代罚球员/,
  )

  match.penalties.push({ team: 'home', server: '7', released: false })
  assert.throws(
    () => penaltyData(match, { team: 'home', player: '8', server: '7', subject: 'player', kind: 'minor' }),
    /仍有未释放的罚时/,
  )
})

test('纪律累计忽略官员判罚并按重大犯规上限罚退球员', () => {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  match.rules.majorLimit = 2
  match.penalties.push(
    { team: 'home', player: '7', subject: 'player', kind: 'major' },
    { team: 'home', player: '7', subject: 'player', kind: 'major' },
    { team: 'home', player: '教练', subject: 'official', kind: 'major' },
  )

  rebuildDiscipline(match)

  assert.deepEqual(match.majors, { 'home:7': 2 })
  assert.deepEqual(match.disqualified, ['home:7'])
})

test('暂停额度按上下半场及旧记录节次恢复', () => {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })

  assert.equal(timeoutBucket(match, 1), 'first')
  assert.equal(recordTimeoutBucket(match, { period: '第 3 节' }), 'second')
  assert.equal(recordTimeoutBucket(match, { period: '加时 1' }), 'overtime-5')
})

test('记录更正只允许查找有效记录并要求填写依据', () => {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  match.events.push({ id: 1, category: 'goal', text: '主队进球。', period: '第 1 节', time: '07:00' })

  const event = correctionRecord(match, {
    id: 1,
    mode: 'edit',
    correctionReason: '裁判确认球员号码有误',
  }, 'goal')

  assert.equal(event.id, 1)
  assert.throws(
    () => correctionRecord(match, { id: 1, mode: 'edit', correctionReason: '' }, 'goal'),
    /请填写 200 字以内的更正依据/,
  )
})
