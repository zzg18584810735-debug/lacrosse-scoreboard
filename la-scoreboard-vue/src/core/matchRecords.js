import { groupLabel } from './rules.js'

const TEAM_RELATED_CATEGORIES = new Set([
  'goal',
  'goal-detail',
  'penalty',
  'penalty-correction',
  'timeout',
  'timeout-correction',
])

/**
 * 返回比赛记录在“相关队伍／球员”列中的结构化关联对象。
 * 裁判判定和比赛节点即使携带球权球队，也统一属于全场。
 */
export function recordRelation(match, record) {
  const target = record.targetId == null
    ? null
    : match.events.find(event => event.id === Number(record.targetId))
  const changes = record.changes || {}
  const team = record.team || changes.team || target?.team || null
  const player = record.player || changes.player || target?.player || ''
  const assist = record.assist || changes.assist || target?.assist || ''
  const subject = record.subject || changes.subject || target?.subject || ''
  const teamRelated = TEAM_RELATED_CATEGORIES.has(record.category)
    || (record.eventType === 'SYSTEM' && Boolean(team))

  if (!teamRelated || !['home', 'away'].includes(team)) {
    return { scope: 'match', team: null, player: '', assist: '' }
  }
  return {
    scope: player ? 'person' : 'team',
    team,
    player: subject === 'official' && player ? `球队官员 ${player}` : player,
    assist,
  }
}

export function matchRecords(match, category = 'all') {
  const details = new Map()
  const corrections = new Map()
  const originals = new Map()
  const invalidGoals = new Map()
  for (const event of match.events) {
    if (event.category === 'goal-ruling' && !event.voided) {
      for (const id of event.invalidGoalIds) invalidGoals.set(id, event)
    }
    if (event.category === 'goal-detail' && !event.voided) details.set(event.targetId, event)
    if (['penalty-correction', 'timeout-correction'].includes(event.category) && !event.voided) {
      corrections.set(event.targetId, event)
      if (!originals.has(event.targetId)) originals.set(event.targetId, event.before)
    }
  }
  return match.events
    .filter(event => category === 'all' || event.category === category)
    .map(event => effectiveRecord(match, event, { details, corrections, originals, invalidGoals }))
}

export function foulCounts(match, team) {
  return Object.fromEntries(['minor-turnover', 'minor', 'major', 'expulsion'].map(kind => [
    kind,
    kind === 'minor-turnover'
      ? (match.events.some(event => !event.category)
          ? null
          : matchRecords(match, 'penalty').filter(event => event.team === team && event.kind === kind && !event.voided).length)
      : match.penalties.filter(penalty => penalty.team === team && penalty.kind === kind).length,
  ]))
}

export const penaltyLabel = kind => ({
  'minor-turnover': '无牌 · 仅换球权',
  minor: '绿牌',
  major: '黄牌',
  expulsion: '红牌',
}[kind])

export function exportFilename(match, extension) {
  const part = value => [...String(value)
    .replace(/[\\/:*?"<>|\u0000-\u001f\u007f]/g, '_')
    .trim()]
    .slice(0, 16)
    .join('')
    .replace(/[. ]+$/, '') || '未命名'
  return `${match.config.date}_${part(match.config.title)}_${part(match.config.home)}vs${part(match.config.away)}_比赛记录.${extension}`
}

export function exportCsv(match) {
  const categories = {
    'goal-ruling': '金球核对',
    goal: '进球',
    penalty: '犯规',
    timeout: '球队暂停',
    'goal-detail': '进球信息更正',
    'penalty-correction': '判罚更正',
    'timeout-correction': '暂停更正',
    'score-correction': '比分更正',
    'match-info': '比赛信息修改',
    system: '场上操作',
  }
  const status = match.goldenConflict
    ? '金球冲突待核对'
    : {
        ready: '待开赛',
        period: '比赛中',
        timeout: '球队暂停',
        break: '节间休息',
        'period-end': '待核对',
        finished: '已结束',
      }[match.phase]
  const header = ['比赛名称','比赛日期','组别','主队','客队','导出时主队总分','导出时客队总分','比赛状态','记录编号','类型','节次','比赛剩余时间','主客身份','球队','球员','助攻','牌型','犯规备注','记录状态','标题','详细信息','更正／补录依据','关联记录编号','原始记录','规则依据']
  const events = matchRecords(match)
  const rows = (events.length ? events : [{ text: '暂无比赛事件', category: 'summary' }]).map(event => {
    const relation = recordRelation(match, event)
    return [
    match.config.title,
    match.config.date,
    groupLabel(match.config, match.rules),
    match.config.home,
    match.config.away,
    match.scores.home,
    match.scores.away,
    status,
    event.id,
    categories[event.category] || (event.category === 'summary' ? '比赛信息' : '其他记录'),
    event.period,
    event.time,
    relation.team === 'home' ? '主队' : relation.team === 'away' ? '客队' : '',
    relation.team ? match.config[relation.team] : '',
    relation.player,
    relation.assist,
    event.kind ? penaltyLabel(event.kind) : '',
    event.category === 'penalty' ? event.reason : '',
    event.voided ? (event.cancelled ? '已作废' : '已撤销') : event.originalText ? '已更正' : '有效',
    event.title,
    event.text,
    event.correctionReason || (event.category !== 'penalty' ? event.reason : ''),
    event.targetId,
    event.originalText,
    event.rule,
    ]
  })
  return `\uFEFF${[header, ...rows].map(row => row.map(csvCell).join(',')).join('\r\n')}\r\n`
}

function effectiveRecord(match, event, indexes) {
  if (event.category === 'goal') {
    const invalid = indexes.invalidGoals.get(event.id)
    const detail = indexes.details.get(event.id)
    return {
      ...event,
      ...(invalid && !event.voided
        ? { voided: true, cancelled: true, correctionReason: invalid.reason, originalText: event.text }
        : {}),
      player: detail ? detail.player : (event.player || ''),
      assist: detail ? detail.assist : (event.assist || ''),
    }
  }
  const correction = indexes.corrections.get(event.id)
  if (correction && !event.voided) {
    const original = indexes.originals.get(event.id)
    return {
      ...event,
      ...correction.changes,
      voided: Boolean(correction.changes.cancelled),
      correctionReason: correction.correctionReason,
      originalText: event.text + (original?.reason ? ` 犯规备注：${original.reason}` : ''),
    }
  }
  // 旧版 v4 定时罚时只在罚时对象中保存犯规备注。
  if (event.category === 'penalty') {
    return {
      ...event,
      reason: event.reason ?? match.penalties.find(penalty => penalty.id === event.id)?.reason ?? '',
    }
  }
  return event
}

function csvCell(value) {
  let text = String(value ?? '')
  // 即使被引号包裹，电子表格仍可能执行公式，因此需要添加文本前缀。
  if (/^[\s\uFEFF]*[=+\-@]/.test(text) || /^[\t\r\n]/.test(text)) text = `'${text}`
  return `"${text.replace(/"/g, '""')}"`
}
