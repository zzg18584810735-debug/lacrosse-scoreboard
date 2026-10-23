import {
  correctionRecord,
  recordMatchEvent,
  recordTimeoutBucket,
  timeoutBucket,
} from './matchEvents.js'
import { MATCH_EVENT_TYPE } from './eventTypes.js'
import { matchRecords } from './matchRecords.js'
import { isYouth } from './rules.js'
import { freeze, isOvertime, isPeriodActive, stopTime } from './timing.js'

const TIMEOUT_ACTIONS = new Set([
  'confirm-timeout',
  'timeout',
  'end-timeout',
  'correct-timeout',
])

export function applyTimeoutAction(match, type, payload = {}, dispatch = null) {
  if (!TIMEOUT_ACTIONS.has(type)) return false

  switch (type) {
    case 'confirm-timeout':
      confirmTimeout(match, payload, dispatch)
      break
    case 'timeout':
      startTimeout(match, payload)
      break
    case 'end-timeout':
      endTimeout(match)
      break
    case 'correct-timeout':
      correctTimeout(match, payload)
      break
  }
  return true
}

function confirmTimeout(match, payload, dispatch) {
  requireTeam(payload.team)
  const halfBreak = match.phase === 'break' && match.period === match.rules.halftimeAfter

  if (halfBreak) {
    requireTimeout(payload.condition === 'halftime', '比赛阶段已变化，请关闭后重新确认暂停条件。')
  } else {
    requireTimeout(isPeriodActive(match) && match.periodStarted, '比赛尚未开始或当前正在暂停，不能申请球队暂停。')
    requireTimeout(['frontcourt', 'dead-ball'].includes(payload.condition), '请按裁判判定选择暂停条件。')
    requireTimeout(typeof dispatch === 'function', '球队暂停动作缺少比赛状态调度器。')
    // 暂停资格确认是一个原子操作，失败时不得留下球权或计时变化。
    if (payload.condition === 'dead-ball') deadBall(match)
    // 暂停条件是本次暂停事件的一部分。这里仅同步球权和前场状态，
    // 不把弹窗内的条件确认拆成额外的球权、进入前场比赛记录。
    dispatch('possession', { team: payload.team, recordEvent: false })
    if (payload.condition === 'frontcourt') dispatch('frontcourt', { recordEvent: false })
  }

  if (typeof dispatch === 'function') dispatch('timeout', { team: payload.team })
  else startTimeout(match, { team: payload.team })
  const event = match.events.at(-1)
  event.condition = payload.condition
  event.text += ` 裁判确认条件：${halfBreak ? '中场暂停' : payload.condition === 'frontcourt' ? '本队在进攻区控球' : '死球后球权判给本队'}。`
  match.notice = event.text
}

function startTimeout(match, payload) {
  requireTeam(payload.team)
  const halfBreak = match.phase === 'break' && match.period === match.rules.halftimeAfter
  requireTimeout((isPeriodActive(match) && match.periodStarted) || halfBreak, '比赛尚未开始或当前正在暂停，不能申请球队暂停。')
  requireTimeout(!isOvertime(match) || match.rules.overtimeTimeouts > 0, '本场规则不发放加时球队暂停。')
  requireTimeout(match.timeouts[payload.team] > 0, '本队本半场暂停已用完。')
  requireTimeout(halfBreak
    || (match.possession === payload.team && (match.ball === 'dead' || match.frontcourt)), '球队暂停须在前场控球，或死球后被授予球权。请先记录符合条件的球权／前场状态。')

  match.timeoutContext = {
    phase: match.phase,
    auxiliary: match.auxiliary,
    auxiliaryRunning: match.auxiliaryRunning,
    timeoutId: match.sequence + 1,
  }
  match.timeouts[payload.team] -= 1
  deadBall(match, true)
  match.phase = 'timeout'
  match.auxiliary = match.rules.timeout
  match.auxiliaryRunning = true
  recordMatchEvent(
    match,
    `${match.config[payload.team]}使用 ${match.rules.timeout / 1000} 秒球队暂停，剩余 ${match.timeouts[payload.team]} 次。${isYouth(match) ? '比赛钟与罚时暂停。' : '三种比赛时钟暂停，进攻钟不重置。'}`,
    '6.10.4–5',
    {
      category: 'timeout',
      eventType: MATCH_EVENT_TYPE.TEAM_TIMEOUT,
      team: payload.team,
      durationSeconds: match.rules.timeout / 1000,
      periodNumber: match.period,
      timeoutBucket: timeoutBucket(match),
    },
  )
}

function endTimeout(match) {
  requireTimeout(match.phase === 'timeout', '当前不是球队暂停。')
  const timeoutEvent = match.events.find(event => event.id === match.timeoutContext?.timeoutId)
  const team = timeoutEvent?.team
  const teamName = team ? match.config[team] : ''
  match.phase = match.timeoutContext?.phase || 'period'
  match.auxiliary = match.timeoutContext?.auxiliary || 0
  match.auxiliaryRunning = false
  match.timeoutContext = null
  match.alert = ''
  recordMatchEvent(
    match,
    match.phase === 'break'
      ? `${teamName ? `${teamName}球队` : '球队'}暂停结束，回到中场休息；休息钟保持暂停，可手动继续。`
      : `${teamName ? `${teamName}球队` : '球队'}暂停结束，等待裁判鸣哨复赛。`,
    '6.10',
    { eventType: MATCH_EVENT_TYPE.SYSTEM, team },
  )
}

function correctTimeout(match, payload) {
  const event = correctionRecord(match, payload, 'timeout')
  const bucket = recordTimeoutBucket(match, event)
  const currentBucket = timeoutBucket(match)
  const cancelled = payload.mode === 'cancel'
  const team = cancelled ? event.team : payload.team
  requireTeam(team)

  const limit = bucket.startsWith('overtime-')
    ? match.rules.overtimeTimeouts
    : match.rules.timeoutsPerHalf
  const others = matchRecords(match, 'timeout')
    .filter(item => !item.voided
      && item.id !== event.id
      && item.team === team
      && recordTimeoutBucket(match, item) === bucket)
    .length
  requireTimeout(cancelled || others < limit, '该队在这次暂停所属半场／加时的次数已用完，不能转记。')

  if (bucket === currentBucket && (cancelled || team !== event.team)) {
    requireTimeout(cancelled || match.timeouts[team] > 0, '该队本半场／加时暂停次数已用完。')
    match.timeouts[event.team] = Math.min(limit, match.timeouts[event.team] + 1)
    if (!cancelled) match.timeouts[team] -= 1
  }

  const currentTimeoutId = match.timeoutContext?.timeoutId
    || matchRecords(match, 'timeout').filter(item => !item.voided).at(-1)?.id
  if (cancelled && match.phase === 'timeout' && currentTimeoutId === event.id) {
    match.phase = match.timeoutContext?.phase || 'period'
    match.auxiliary = match.timeoutContext?.auxiliary || 0
    match.timeoutContext = null
    match.alert = ''
  }

  freeze(match)
  recordMatchEvent(
    match,
    `${cancelled ? '作废' : '更正'}暂停 #${event.id}：${payload.correctionReason}。${cancelled ? '取消' : '改记为'}${match.config[team]}暂停。${bucket === currentBucket ? '已同步本半场／加时剩余次数。' : '仅修正该历史半场／加时记录，不改变当前剩余次数。'}比赛时钟数值与球权不回溯，保持停表。`,
    '裁判更正',
    {
      category: 'timeout-correction',
      targetId: event.id,
      before: { ...event },
      changes: {
        team,
        timeoutBucket: bucket,
        cancelled,
        text: `${match.config[team]}球队暂停${cancelled ? '（已作废）' : '（已更正）'}。`,
      },
      correctionReason: String(payload.correctionReason).trim(),
    },
  )
}

function deadBall(match, force = false) {
  match.ball = 'dead'
  match.clear = null
  if (force || stopTime(match)) match.running = false
}

function requireTeam(team) {
  requireTimeout(['home', 'away'].includes(team), '请选择申请暂停的球队。')
}

function requireTimeout(ok, message) {
  if (!ok) throw new Error(message)
}
