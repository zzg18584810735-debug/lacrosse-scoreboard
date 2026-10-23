import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from './matchModel.js'
import { exportCsv, exportFilename, foulCounts, matchRecords } from './matchRecords.js'

test('比赛记录查询合并进球明细和判罚更正', () => {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  match.events.push(
    { id: 1, category: 'goal', text: '主队进球。', player: '', assist: '', period: '第 1 节', time: '07:00' },
    { id: 2, category: 'goal-detail', targetId: 1, player: '7', assist: '8', text: '补充进球信息。', period: '第 1 节', time: '06:59' },
    { id: 3, category: 'penalty', team: 'away', kind: 'major', text: '客队黄牌。', period: '第 1 节', time: '06:00' },
    { id: 4, category: 'penalty-correction', targetId: 3, changes: { kind: 'minor', cancelled: false }, before: { reason: '原犯规' }, correctionReason: '裁判改判', text: '判罚更正。', period: '第 1 节', time: '05:59' },
  )

  const goal = matchRecords(match, 'goal')[0]
  const penalty = matchRecords(match, 'penalty')[0]

  assert.equal(goal.player, '7')
  assert.equal(goal.assist, '8')
  assert.equal(penalty.kind, 'minor')
  assert.equal(penalty.correctionReason, '裁判改判')
  assert.match(penalty.originalText, /原犯规/)
})

test('犯规统计区分仅换球权事件和实际罚时', () => {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  match.events.push({ id: 1, category: 'penalty', team: 'home', kind: 'minor-turnover' })
  match.penalties.push({ id: 2, team: 'home', kind: 'major' })

  assert.deepEqual(foulCounts(match, 'home'), {
    'minor-turnover': 1,
    minor: 0,
    major: 1,
    expulsion: 0,
  })
})

test('CSV导出阻止公式执行并清理文件名非法字符', () => {
  const match = createMatch({
    preset: 'sll-sixes',
    date: '2026-10-21',
    title: '=危险公式',
    home: '主/队',
    away: '客:队',
  })

  assert.match(exportCsv(match), /"'=危险公式"/)
  assert.equal(exportFilename(match, 'csv'), '2026-10-21_=危险公式_主_队vs客_队_比赛记录.csv')
})
