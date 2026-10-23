export function pendingPenaltyTasks(match) {
  return match.penalties.filter(penalty => !penalty.released
    && (!penalty.seated || penalty.remaining === 0))
}

export function canResumeClock(match) {
  return ['ready', 'period'].includes(match.phase)
    && !(match.running && match.ball === 'live')
}

export function canPauseClock(match) {
  return match.phase === 'period'
    && match.periodStarted === true
    && match.running === true
}
