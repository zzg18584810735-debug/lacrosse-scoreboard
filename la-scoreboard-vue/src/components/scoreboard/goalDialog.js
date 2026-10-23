import { lastStartedPeriod } from '../../core/timing.js'
import { matchRecords } from '../../core/matchRecords.js'

export const otherGoalTeam = team => team === 'home' ? 'away' : 'home'

export function goalDialogDraftKey(match, mode) {
  return `${mode}:${match.pendingGoal?.id ?? ''}`
}

export function createGoalDialogDraft(match, mode, initialTeam = 'home') {
  const backfill = mode === 'backfill'
  const pending = match.pendingGoal || {}
  const latestPeriod = lastStartedPeriod(match)
  const main = backfill
    ? (latestPeriod === match.period ? Math.ceil(match.main / 1000) * 1000 : 0)
    : (pending.main || 0)

  return {
    team: backfill ? initialTeam : (pending.team || initialTeam),
    goalId: backfill ? '' : pending.id,
    player: backfill ? '' : (pending.player || ''),
    assist: backfill ? '' : (pending.assist || ''),
    period: Math.max(1, latestPeriod),
    minutes: Math.floor(main / 60000),
    seconds: Math.floor(main / 1000) % 60,
    scoreMode: 'add',
    reason: '',
    latestPeriod,
    main,
  }
}

export function findGoalRecord(match, goalRecordId) {
  return matchRecords(match, 'goal').find(event => event.id === Number(goalRecordId))
}
