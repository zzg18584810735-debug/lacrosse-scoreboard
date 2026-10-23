import { matchRecords } from './matchRecords.js'
import { freeze, isOvertime, periodHasStarted } from './timing.js'

export const GOLDEN_CONFLICT_HINT = '加时金球进球记录有冲突，请先用“核对加时金球进球”确认有效进球和比分，或撤销误录。'

export const overtimeGoals = match => matchRecords(match, 'goal')
  .filter(event => !event.voided
    && (event.periodNumber > match.rules.periods || /^加时 \d+$/.test(event.period)))

export function confirmGoldenGoal(match) {
  freeze(match)
  match.ball = 'dead'
  match.clear = null
  match.phase = 'period-end'
  match.goldenGoal = true
  match.timeoutContext = null
  match.auxiliary = 0
  match.alert = '金球已记录，核对有效后结束比赛；误录可撤销或更正。'
}

export function holdGoldenConflict(match) {
  freeze(match)
  match.ball = 'dead'
  match.clear = null
  match.phase = 'period-end'
  match.goldenConflict = true
  match.goldenGoal = false
  match.timeoutContext = null
  match.auxiliary = 0
  match.alert = GOLDEN_CONFLICT_HINT
}

export function reconcileMatch(match) {
  if (match.rules.goldenGoal && isOvertime(match)) {
    const goals = overtimeGoals(match)
    const goal = goals[0]
    const opponent = goal?.team === 'home' ? 'away' : 'home'
    const inconsistent = goal
      && (match.scores[goal.team] - match.scores[opponent] !== (goal.points ?? match.rules.points)
        || !match.goldenGoal
        || !['period-end', 'finished'].includes(match.phase))
    if (goals.length > 1 || inconsistent) holdGoldenConflict(match)
  }

  // 核对上一节结果不能作为下一节加时已经开赛的依据。
  if (isOvertime(match) && !periodHasStarted(match) && !match.goldenGoal && !match.goldenConflict) {
    if (match.scores.home === match.scores.away) {
      if (match.phase !== 'ready') match.alert = '比分恢复平局，本节加时尚未开赛；请按裁判指令鸣哨开赛。'
      match.phase = 'ready'
    } else {
      if (match.phase !== 'finished') match.phase = 'period-end'
      match.alert = '上一节核对后已分胜负，本节加时尚未开赛；请核对比分并结束比赛。'
    }
    freeze(match)
    match.timeoutContext = null
    match.auxiliary = 0
  }
}
