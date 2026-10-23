export function canRecordGoal(match) {
  return match.phase === 'period' && match.running && match.ball === 'live'
}

export function canRecordPenalty(match) {
  return match.phase === 'period' && match.periodStarted === true
}

export function canRequestTeamTimeout(match, team) {
  const allowedPhase = (match.phase === 'period' && match.periodStarted === true)
    || (match.phase === 'break' && match.period === match.rules.halftimeAfter)
  return allowedPhase && match.timeouts[team] > 0
}

export function expelledNumbers(match, team) {
  const prefix = `${team}:`
  return match.disqualified
    .filter(value => value.startsWith(prefix))
    .map(value => value.slice(prefix.length))
}
