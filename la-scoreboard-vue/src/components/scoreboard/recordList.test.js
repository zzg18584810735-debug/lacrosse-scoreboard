import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import {
  correctionAction,
  emptyRecordText,
  recordPersonLabel,
  recordTeamLabel,
  recordTeamSide,
  visibleRecords,
} from './recordList.js'

test('计时台只展示最近三十条记录，完整页面不截断', () => {
  const records = Array.from({ length: 35 }, (_, index) => ({ id: index + 1 }))
  assert.equal(visibleRecords(records, true).length, 30)
  assert.equal(visibleRecords(records, false).length, 35)
})

test('空记录提示区分全部和分类筛选', () => {
  assert.deepEqual(emptyRecordText('all'), ['暂无比赛记录', '鸣哨开赛后，操作记录会显示在这里'])
  assert.deepEqual(emptyRecordText('goal'), ['暂无此类记录', '旧版未分类记录可在“全部”中查看'])
})

test('记录按业务主体区分全场、球队和人员', () => {
  const match = { config: { home: '熊猫', away: '袋鼠' }, events: [] }
  assert.equal(recordTeamLabel({ eventType: 'MATCH_START', team: 'home', category: 'system' }, match), '全场')
  assert.equal(recordTeamLabel({ eventType: 'POSSESSION', team: 'away', category: 'system' }, match), '全场')
  assert.equal(recordTeamLabel({ eventType: 'TEAM_TIMEOUT', team: 'home', category: 'timeout' }, match), '熊猫')
  assert.equal(recordTeamLabel({ eventType: 'SYSTEM', team: 'away', category: 'system' }, match), '袋鼠')
  assert.equal(recordTeamSide({ eventType: 'SYSTEM', team: 'away', category: 'system' }, match), 'away')
  assert.equal(recordPersonLabel({ eventType: 'PENALTY', team: 'home', category: 'penalty', player: '7' }, match), '7')
  assert.equal(recordPersonLabel({ eventType: 'PENALTY', team: 'away', category: 'penalty', player: '教练', subject: 'official' }, match), '球队官员 教练')
  assert.equal(recordTeamLabel({}, match), '全场')
})

test('更正记录继承修改后的球队和人员', () => {
  const match = {
    config: { home: '熊猫', away: '袋鼠' },
    events: [{ id: 3, category: 'penalty', team: 'home', player: '7' }],
  }
  const correction = {
    category: 'penalty-correction',
    targetId: 3,
    changes: { team: 'away', player: '9' },
  }
  assert.equal(recordTeamLabel(correction, match), '袋鼠')
  assert.equal(recordPersonLabel(correction, match), '9')
})

test('有效事件提供对应修改入口，作废事件不再修改', () => {
  assert.deepEqual(correctionAction({ category: 'penalty' }), {
    action: 'correct-penalty-form',
    label: '更正／作废判罚',
  })
  assert.deepEqual(correctionAction({ category: 'goal', player: '' }), {
    action: 'goal-details',
    label: '补充球员',
  })
  assert.equal(correctionAction({ category: 'timeout', voided: true }), null)
})

test('记录操作按钮紧跟标题并位于详情内容之前', () => {
  const source = readFileSync(new URL('./MatchRecords.vue', import.meta.url), 'utf8')
  const compactSummaryStart = source.indexOf('<summary>')
  const compactSummaryEnd = source.indexOf('</summary>', compactSummaryStart)
  const fullTitleStart = source.indexOf('<div class="record-title-line">')
  const fullTextStart = source.indexOf('<span class="record-text">', fullTitleStart)
  const compactAction = source.indexOf('class="record-edit"', compactSummaryStart)
  const fullAction = source.indexOf('class="record-edit"', fullTitleStart)

  assert.ok(compactAction > compactSummaryStart && compactAction < compactSummaryEnd)
  assert.ok(fullAction > fullTitleStart && fullAction < fullTextStart)
})
