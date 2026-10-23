import { formatRuleReference } from './rules.js'
import { MATCH_EVENT_TYPE, resolveEventTitle } from './eventTypes.js'

export const isOvertime = match => match.period > match.rules.periods

export const needsWinner = match => match.rules.overtimePolicy === 'always'
  || (match.rules.overtimePolicy === 'knockout' && match.config.stage === 'knockout')

export const canFinish = match => !match.goldenConflict
  && match.period >= match.rules.periods
  && (match.scores.home !== match.scores.away || !needsWinner(match))

export function periodName(match) {
  if (isOvertime(match)) return `加时 ${match.period - match.rules.periods}`
  if (match.rules.periods === 2) return match.period === 1 ? '上半场' : '下半场'
  return `第 ${match.period} 节`
}

export function periodHasStarted(match) {
  return match.periodStarted ?? inferredPeriodStarted(match)
}

export const lastStartedPeriod = match => match.period - (periodHasStarted(match) ? 0 : 1)

export function clock(milliseconds) {
  const seconds = Math.ceil(Math.max(0, milliseconds) / 1000)
  return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`
}

export const isPeriodActive = match => match.phase === 'period'

export const stopTime = match => match.rules.allDeadStop
  || (isOvertime(match) && match.rules.overtimeStopAll)
  || (match.period >= match.rules.periods && match.main <= match.rules.stopWindow)

export const shotOff = match => !match.rules.shot
  || (match.rules.shotLastOff && match.main < match.rules.shot)

export function freeze(match) {
  match.running = false
  match.auxiliaryRunning = false
}

function countdown(remaining, elapsed) {
  const safeRemaining = Number.isFinite(remaining) ? Math.max(0, remaining) : 0
  const safeElapsed = Number.isFinite(elapsed) ? Math.max(0, elapsed) : 0
  return Math.max(0, safeRemaining - safeElapsed)
}

// 使用单调时钟传入的实际耗时推进，不能把一次回调当作一秒。
export function advance(match, elapsed) {
  if (!Number.isFinite(elapsed) || elapsed <= 0) return false
  let changed = false
  if (match.auxiliaryRunning) {
    const before = match.auxiliary
    match.auxiliary = countdown(before, elapsed)
    changed = match.auxiliary !== before
    if (before > 0 && match.auxiliary === 0) {
      match.auxiliaryRunning = false
      if (match.phase === 'timeout') {
        // 球队暂停由运行引擎立即派发统一的 end-timeout 动作，避免先写一条
        // “时间到”再写一条“暂停结束”的重复记录。
        match.alert = '球队暂停时间到，正在结束暂停。'
      } else {
        match.alert = '休息时间到，等待裁判指令。'
        recordTimingEvent(match, match.alert, '5.1')
      }
    }
    return changed
  }

  // 每次推进到最近的截止点，保证回调延迟时的事件顺序仍然准确。
  while (elapsed > 0 && match.running && isPeriodActive(match) && match.main > 0) {
    const shotRunning = match.rules.shot > 0
      && match.ball === 'live'
      && match.possession
      && (!match.rules.shotLastOff || match.main > match.rules.shot)
      && match.shot > 0
    const clearRunning = match.ball === 'live' && match.clear !== null && match.clear > 0
    const penalties = match.penalties.filter(penalty => penalty.started && penalty.remaining > 0)
    const delta = Math.min(
      elapsed,
      match.main,
      ...penalties.map(penalty => penalty.remaining),
      ...(shotRunning ? [match.shot, ...(match.rules.shotLastOff ? [match.main - match.rules.shot] : [])] : []),
      ...(clearRunning ? [match.clear] : []),
    )
    match.main = countdown(match.main, delta)
    changed = true
    elapsed -= delta

    if (shotRunning) {
      match.shot = countdown(match.shot, delta)
      if (match.shot === 0) {
        match.alert = '进攻钟到期：等待裁判确认射门或违例，不自动判换球权。'
        recordTimingEvent(match, match.alert, '4.2.4 / 5.3.1')
      }
    }
    if (clearRunning) {
      match.clear = countdown(match.clear, delta)
      if (match.clear === 0) {
        match.alert = `${match.rules.clearMs / 1000} 秒推进提示到期，请裁判确认是否违例。`
        recordTimingEvent(match, match.alert, '6.9')
      }
    }
    for (const penalty of penalties) {
      penalty.remaining = countdown(penalty.remaining, delta)
      if (penalty.remaining === 0) {
        penalty.released = true
        recordTimingEvent(
          match,
          `${match.config[penalty.team]} ${penalty.server} 号罚时结束，已自动释放。`,
          '9.4.1',
          { eventType: MATCH_EVENT_TYPE.SYSTEM, team: penalty.team, player: penalty.server },
        )
      }
    }
    if (shotOff(match)) match.shot = match.rules.shot
    if (match.main === 0) endPeriod(match)
  }
  return changed
}

function inferredPeriodStarted(match) {
  // 旧版 v4 缓存没有开赛标志，只能根据真实鸣哨或下一节准备记录推断。
  const events = match.events.filter(event => !event.voided && event.period === periodName(match))
  for (let index = events.length - 1; index >= 0; index -= 1) {
    if (events[index].text.startsWith('鸣哨开赛／复赛。')) return true
    if (/^(下一节准备就绪，|按裁判确认延续球权及进攻剩余时间，)/.test(events[index].text)) return false
  }
  return !(match.phase === 'ready' || (match.phase === 'timeout' && match.timeoutContext?.phase === 'ready'))
}

export function endPeriod(match) {
  freeze(match)
  match.ball = 'dead'
  match.clear = null
  match.phase = 'period-end'
  match.alert = `${periodName(match)}时间到，请核对最后一次射门与比分。`
  recordTimingEvent(match, match.alert, '5.1 / 5.2.2', { eventType: MATCH_EVENT_TYPE.PERIOD_END })
}

function recordTimingEvent(match, text, rule, details = {}) {
  const formattedRule = formatRuleReference(match, rule)
  match.notice = text
  match.rule = formattedRule
  const event = {
    id: ++match.sequence,
    period: periodName(match),
    periodNumber: match.period,
    time: clock(match.main),
    remaining: match.main,
    text,
    rule: formattedRule,
    category: 'system',
    ...details,
  }
  event.title = resolveEventTitle(event, match)
  match.events.push(event)
}
