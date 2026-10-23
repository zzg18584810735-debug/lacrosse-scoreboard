import { overtimeGoals } from '../../core/matchOutcome.js'

export function goldenGoalCandidates(match) {
  return [...overtimeGoals(match)].sort((left, right) =>
    (left.periodNumber - right.periodNumber) || (right.remaining - left.remaining))
}

export function createGoldenGoalReviewDraft(match) {
  return {
    goalId: '',
    home: match.scores.home,
    away: match.scores.away,
    reason: '',
  }
}

export function goldenGoalCandidateLabel(match, goal) {
  return `#${goal.id} · ${goal.period} ${goal.time} · ${match.config[goal.team]} · ${goal.player}`
}
