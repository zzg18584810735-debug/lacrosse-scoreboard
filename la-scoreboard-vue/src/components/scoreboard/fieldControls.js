export function isFieldActive(match) {
  return match.phase === 'period'
}

export function canSetPossession(match) {
  return match.main > 0 && ['ready', 'period'].includes(match.phase)
}

export function otherTeam(team) {
  return team === 'home' ? 'away' : 'home'
}

export function canSetAlternate(match) {
  return ['ready', 'period', 'break'].includes(match.phase)
}

export function clearProgressLabel(match) {
  if (match.frontcourt) return '已在前场'
  if (match.clear !== null) return '推进中'
  return `${match.rules.clearMs / 1000} 秒辅助计时`
}
