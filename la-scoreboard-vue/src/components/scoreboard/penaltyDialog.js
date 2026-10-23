export function createPenaltyDraft(defaults = {}) {
  return {
    team: defaults.team || 'home',
    player: defaults.player || '',
    subject: defaults.subject || 'player',
    kind: defaults.kind || 'major',
    reason: defaults.reason || '',
    server: defaults.server || '',
    awarded: defaults.awarded || 'opponent',
  }
}

export function requiresPenaltyServer(match, draft) {
  return draft.kind !== 'minor-turnover'
    && (draft.kind === 'expulsion' || draft.subject === 'official'
      || (draft.subject === 'goalie' && match.rules.goalieProxy))
}

export function penaltyKindOptions(match) {
  return [
    { value: 'minor-turnover', label: '轻微犯规 · 仅换球权，无罚时' },
    { value: 'minor', label: `绿牌 · ${match.rules.minor / 1000} 秒` },
    { value: 'major', label: `黄牌 · ${match.rules.major / 1000} 秒` },
    { value: 'expulsion', label: `红牌 · 驱逐 + 代罚 ${match.rules.expulsion / 1000} 秒` },
  ]
}
