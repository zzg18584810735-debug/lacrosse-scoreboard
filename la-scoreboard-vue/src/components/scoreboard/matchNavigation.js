import { groupLabel, isCustom } from '../../core/rules.js'

export const SCOREBOARD_VIEWS = [
  ['desk', '计时台'],
  ['rules', '本场规则'],
  ['log', '比赛记录'],
]

export function shortMatchDate(date) {
  return date.slice(5).replace('-', '/')
}

export function navigationSummary(match) {
  const custom = isCustom(match) ? '自定义 · ' : ''
  const stage = match.config.stage === 'knockout' ? '淘汰／名次赛' : '常规赛'
  const shot = match.rules.shot ? `${match.rules.shot / 1000} 秒进攻钟` : '无进攻钟'
  return `SLL · ${groupLabel(match.config, match.rules)} · ${stage} · ${custom}${match.rules.periods} × ${match.rules.period / 60000} 分钟 · ${shot}`
}

export function compactSyncSummary(syncStatus, remoteMatchId) {
  if (remoteMatchId === null
    || remoteMatchId === undefined
    || String(remoteMatchId).trim() === '') return ''
  const label = {
    loading: '读取中',
    syncing: '同步中',
    synced: '已同步',
    offline: '网络离线',
    conflict: '同步冲突',
    error: '同步失败',
  }[syncStatus]
  return label ? `${label} #${remoteMatchId}` : ''
}
