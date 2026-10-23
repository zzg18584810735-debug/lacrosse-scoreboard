export const RECORD_FILTERS = [
  ['all', '全部'],
  ['goal', '进球'],
  ['penalty', '犯规'],
  ['timeout', '暂停'],
]

export function visibleRecords(records, compact) {
  return compact ? records.slice(0, 30) : records
}

export function recordTeamLabel(record, match) {
  const team = recordRelation(match, record).team
  return team ? (match.config[team] || '未命名球队') : '全场'
}

export function recordTeamSide(record, match) {
  return recordRelation(match, record).team
}

export function recordPersonLabel(record, match) {
  return recordRelation(match, record).player
}

export function recordAssistLabel(record, match) {
  return recordRelation(match, record).assist
}

export function emptyRecordText(filter) {
  return filter === 'all'
    ? ['暂无比赛记录', '鸣哨开赛后，操作记录会显示在这里']
    : ['暂无此类记录', '旧版未分类记录可在“全部”中查看']
}

export function correctionAction(record) {
  if (record.voided) return null
  if (record.category === 'penalty') {
    return { action: 'correct-penalty-form', label: '更正／作废判罚' }
  }
  if (record.category === 'timeout') {
    return { action: 'correct-timeout-form', label: '更正／作废暂停' }
  }
  if (record.category === 'goal') {
    return { action: 'goal-details', label: record.player || record.assist ? '修改球员' : '补充球员' }
  }
  return null
}
import { recordRelation } from '../../core/matchRecords.js'
