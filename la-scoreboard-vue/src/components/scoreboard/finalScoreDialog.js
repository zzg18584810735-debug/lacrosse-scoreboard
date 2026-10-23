export function createFinalScoreDraft(match) {
  return {
    home: match.scores.home,
    away: match.scores.away,
    reason: '',
  }
}

export function finalScoreChanged(match, draft) {
  return Number(draft.home) !== match.scores.home || Number(draft.away) !== match.scores.away
}
