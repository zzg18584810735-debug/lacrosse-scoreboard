import { isOvertime } from '../../core/timing.js'

export const correctionLabels = {
  main: '比赛时间',
  shot: '进攻时间',
  score: '总比分',
  penalty: '剩余罚时',
}

export const correctionReasonPlaceholders = {
  main: '例如：裁判确认比赛应剩 2 分 10 秒',
  shot: '例如：裁判确认复赛进攻时间为 15 秒',
  score: '例如：裁判确认主队被多记一分',
  penalty: '例如：裁判确认该球员还需罚时 20 秒',
}

export function correctionScopes(match) {
  const scopes = []
  if (match.phase !== 'break') scopes.push({ value: 'main', label: correctionLabels.main })
  if (match.rules.shot && match.phase !== 'break') scopes.push({ value: 'shot', label: '复赛进攻时间' })
  scopes.push({ value: 'score', label: correctionLabels.score })
  scopes.push({ value: 'penalty', label: correctionLabels.penalty })
  return scopes
}

export function createMatchCorrectionDraft(match) {
  const scopes = correctionScopes(match)
  return {
    scope: scopes[0].value,
    minutes: Math.floor(Math.round(match.main) / 60000),
    seconds: (Math.round(match.main) % 60000) / 1000,
    shot: Math.ceil(match.resetOnResume ? match.rules.shot : match.shot) / 1000,
    home: match.scores.home,
    away: match.scores.away,
    penaltyId: '',
    penaltySeconds: '',
    reason: '',
    maxMinutes: Math.floor((isOvertime(match) ? match.rules.overtime : match.rules.period) / 60000),
  }
}

export function activeCorrectionPenalties(match) {
  return match.penalties.filter(penalty => !penalty.released)
}

export function correctionPenaltySeconds(match, penaltyId) {
  const penalty = activeCorrectionPenalties(match).find(item => item.id === Number(penaltyId))
  return penalty ? Math.ceil(penalty.remaining) / 1000 : ''
}

export function correctionPenaltyLabel(match, penalty) {
  const kind = { minor: '绿牌', major: '黄牌', expulsion: '红牌' }[penalty.kind] || '判罚'
  const seconds = Math.ceil(Math.max(0, penalty.remaining) / 1000)
  const time = `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`
  return `${match.config[penalty.team]} · ${penalty.server} 号 · ${kind} · 剩 ${time}（记录 #${penalty.id}）`
}
