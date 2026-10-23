export function activePenalties(match) {
  return match.penalties.filter(penalty => !penalty.released)
}

export function penaltyTypeLabel(match, penalty) {
  return {
    minor: `绿牌 · ${match.rules.minor / 1000} 秒`,
    major: `黄牌 · ${match.rules.major / 1000} 秒`,
    expulsion: '红牌 · 驱逐',
  }[penalty.kind]
}

export function penaltyStatus(match, penalty) {
  if (penalty.remaining === 0) return '罚时到，等待释放'
  if (!penalty.seated) return '等待就座'
  if (!penalty.started) return '等待复赛哨'
  return match.running ? '与比赛钟同步' : '暂停'
}

export function penaltyDetails(match, penalty) {
  const details = [penaltyTypeLabel(match, penalty)]
  if (penalty.reason) details.push(penalty.reason)
  if (penalty.server !== penalty.player) details.push(`${penalty.server} 号代罚`)
  if (match.disqualified.includes(`${penalty.team}:${penalty.player}`)) details.push('犯规者本场禁入')
  return details.join(' / ')
}
