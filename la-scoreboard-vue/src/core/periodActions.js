import { recordMatchEvent } from './matchEvents.js'
import { MATCH_EVENT_TYPE } from './eventTypes.js'
import { isYouth } from './rules.js'
import { canFinish, isOvertime } from './timing.js'

const PERIOD_ACTIONS = new Set(['next', 'prepare'])

export function applyPeriodAction(match, type, payload = {}) {
  if (!PERIOD_ACTIONS.has(type)) return false

  if (type === 'next') finishPeriodReview(match)
  else prepareNextPeriod(match, payload)
  return true
}

function finishPeriodReview(match) {
  requirePeriod(match.phase === 'period-end', '必须等到本节结束再换节。')
  if (canFinish(match)) {
    match.phase = 'finished'
    match.alert = ''
    recordMatchEvent(
      match,
      match.scores.home === match.scores.away
        ? '常规赛平局，比分已核对，比赛结束。'
        : '比分已核对，比赛结束。',
      isYouth(match) ? '5.1.2 / 6.6.3' : '5.1.5',
      { eventType: MATCH_EVENT_TYPE.MATCH_END },
    )
    return
  }

  match.phase = 'break'
  match.auxiliary = match.period >= match.rules.periods
    ? match.rules.overtimeBreak
    : match.period === match.rules.halftimeAfter
      ? match.rules.halftimeMs
      : match.rules.breakMs
  match.auxiliaryRunning = true
  match.alert = ''
  recordMatchEvent(
    match,
    `进入 ${match.auxiliary / 60000} 分钟休息。${match.period >= match.rules.periods ? `下一节为 ${match.rules.overtime / 60000} 分钟${match.rules.goldenGoal ? '金球' : '完整'}加时。` : '下一节交换攻守方向。'}未完成罚时保留。`,
    '5.1 / 9.4',
  )
}

function prepareNextPeriod(match, payload) {
  requirePeriod(match.phase === 'break', '当前不在节间休息。')
  requirePeriod(payload.carry !== true || match.possession, '没有可延续的球权。请选择重新争球。')

  match.period += 1
  match.main = isOvertime(match) ? match.rules.overtime : match.rules.period
  match.phase = 'ready'
  match.periodStarted = false
  match.auxiliary = 0
  match.auxiliaryRunning = false
  match.running = false
  match.ball = 'dead'
  match.frontcourt = false
  match.clear = null
  match.alert = ''
  if (!payload.carry) {
    match.possession = null
    resetShot(match)
    match.resetOnResume = false
  }
  match.goldenGoal = false

  if (match.rules.halftimeAfter > 0 && match.period === match.rules.halftimeAfter + 1) {
    match.timeouts = {
      home: match.rules.timeoutsPerHalf,
      away: match.rules.timeoutsPerHalf,
    }
    for (const penalty of match.penalties) {
      if (!penalty.released && penalty.remaining > 0) {
        penalty.seated = false
        penalty.started = false
      }
    }
  }
  if (isOvertime(match)) {
    match.timeouts = {
      home: match.rules.overtimeTimeouts,
      away: match.rules.overtimeTimeouts,
    }
    if (match.period === match.rules.periods + 1 && match.rules.alternateReset) {
      match.alternate = match.rules.alternateFirst === 'manual'
        ? null
        : match.rules.alternateFirst
    }
  }

  recordMatchEvent(
    match,
    payload.carry
      ? '按裁判确认延续球权及进攻剩余时间，等待开赛。中场返回的受罚球员需重新确认就座。'
      : '下一节准备就绪，中心开球。中场返回的受罚球员需重新确认就座，等待开赛。',
    '5.3.1 / 6.1 / 6.10.5',
  )
}

function resetShot(match) {
  match.shot = match.rules.shot
  match.alert = ''
}

function requirePeriod(ok, message) {
  if (!ok) throw new Error(message)
}
