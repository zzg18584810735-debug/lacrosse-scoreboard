export function timeoutDialogContext(match, team) {
  const halftime = match.phase === 'break' && match.period === match.rules.halftimeAfter
  const teamName = match.config[team]
  const conditions = match.phase === 'ready'
    ? []
    : halftime
      ? [{ value: 'halftime', label: '确认中场暂停 · 计入上半场' }]
      : [
          { value: 'frontcourt', label: `${teamName}在进攻区控球，确认暂停` },
          { value: 'dead-ball', label: `死球后球权判给${teamName}，确认暂停` },
        ]

  return {
    halftime,
    teamName,
    conditions,
    conditionPrompt: halftime ? '确认本次暂停' : '裁判确认的暂停条件（点选即生效）',
  }
}
