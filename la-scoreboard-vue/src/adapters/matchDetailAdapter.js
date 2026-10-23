import { createMatch } from '../core/matchModel.js'
import { rebuildDiscipline } from '../core/matchEvents.js'
import { eventCategory, isMatchEventType, resolveEventTitle, startsPeriod } from '../core/eventTypes.js'
import { PRESETS } from '../core/rules.js'
import { clock } from '../core/timing.js'

const PHASES = {
  READY: 'ready',
  PERIOD: 'period',
  PERIOD_END: 'period-end',
  BREAK: 'break',
  TIMEOUT: 'timeout',
  FINISHED: 'finished',
}

/** 将后端比赛详情转换为前端计分核心使用的状态模型。 */
export function toScoreboardMatch(detail, options = {}) {
  requireDetail(detail)
  const sourceMatch = detail.match
  const sourceState = detail.state
  const rules = toFrontendRules(detail.rule)
  const presetCode = String(detail.preset || sourceMatch.preset || '').trim()
  const ageGroup = String(detail.ageGroup || sourceMatch.ageGroup || presetCode).trim()
  const preset = Object.hasOwn(PRESETS, presetCode) ? presetCode : 'custom'
  const match = createMatch({
    preset: 'custom',
    title: sourceMatch.title,
    date: sourceMatch.matchDate,
    home: sourceMatch.homeTeamName,
    away: sourceMatch.awayTeamName,
    stage: sourceMatch.stage || 'league',
    rules,
  })

  match.config = {
    ...match.config,
    preset,
    ageGroup,
    matchId: sourceMatch.id,
    matchNo: sourceMatch.matchNo,
    matchTime: sourceMatch.matchTime,
    venueName: sourceMatch.venueName,
    groupRuleId: sourceMatch.groupRuleId,
    homeTeamId: sourceMatch.homeTeamId,
    homeTeamColor: sourceMatch.homeTeamColor,
    awayTeamId: sourceMatch.awayTeamId,
    awayTeamColor: sourceMatch.awayTeamColor,
  }
  if (preset !== 'custom') {
    match.rules.id = PRESETS[preset].id
    match.rules.name = PRESETS[preset].name
    match.rules.family = PRESETS[preset].family
  } else if (ageGroup) {
    match.rules.name = ageGroup.toUpperCase()
  }

  match.phase = toPhase(sourceState.phase)
  match.period = integer(sourceState.currentPeriod, '当前节次', 1)
  match.main = secondsToMs(sourceState.mainRemainingSeconds, '比赛剩余时间')
  match.shot = secondsToMs(sourceState.shotRemainingSeconds ?? 0, '进攻剩余时间')
  match.clear = sourceState.clearRemainingSeconds === null || sourceState.clearRemainingSeconds === undefined
    ? null
    : secondsToMs(sourceState.clearRemainingSeconds, '推进剩余时间')
  match.auxiliary = secondsToMs(sourceState.auxiliaryRemainingSeconds ?? 0, '辅助剩余时间')
  match.auxiliaryType = sourceState.auxiliaryType || null
  match.running = flag(sourceState.mainRunning)
  match.auxiliaryRunning = flag(sourceState.auxiliaryRunning)
  match.ball = sideCode(sourceState.ballState, ['dead', 'live'], '死活球状态', 'dead')
  match.possession = sideCode(sourceState.possessionSide, ['home', 'away'], '当前球权', null)
  match.frontcourt = flag(sourceState.frontcourt)
  match.alternate = sideCode(sourceState.alternateSide, ['home', 'away'], '交替球权', null)
  match.resetOnResume = flag(sourceState.resetShotOnResume)
  match.goldenGoal = flag(sourceState.goldenGoal)
  match.goldenConflict = flag(sourceState.goldenConflict)
  match.scores = {
    home: integer(sourceMatch.homeScore ?? 0, '主队比分', 0),
    away: integer(sourceMatch.awayScore ?? 0, '客队比分', 0),
  }
  match.timeouts = {
    home: integer(sourceState.homeTimeoutsRemaining ?? 0, '主队暂停次数', 0),
    away: integer(sourceState.awayTimeoutsRemaining ?? 0, '客队暂停次数', 0),
  }

  const roster = new Map((detail.roster || []).map(item => [String(item.id), item]))
  const sourceEvents = Array.isArray(detail.events) ? detail.events : []
  const sequenceByDatabaseId = new Map(sourceEvents.map(event => [String(event.id), event.sequenceNo]))
  match.events = sourceEvents.map(event => toFrontendEvent(event, match, roster, sequenceByDatabaseId))
  match.sequence = match.events.reduce((maximum, event) => Math.max(maximum, event.id), 0)
  match.penalties = toFrontendPenalties(detail.penalties || [], sourceEvents, roster)
  rebuildDiscipline(match)
  match.periodStarted = periodStarted(sourceState, sourceEvents, match.period)
  match.timeoutContext = toTimeoutContext(sourceState, sequenceByDatabaseId)
  match.pendingGoal = toPendingGoal(sourceState, roster)
  match.undo = null
  match.notice = match.events.at(-1)?.text || '已从服务器载入本场比赛。'
  match.rule = match.events.at(-1)?.rule || '本场比赛规则'
  match.alert = match.goldenConflict ? '加时进球与比分存在冲突，请裁判核对。' : ''
  match.remoteTimerStartedAt = sourceState.timerStartedAt || null
  applyTimerAnchor(match, sourceState.timerStartedAt, options.now ?? Date.now())
  return match
}

export function toFrontendRules(rule) {
  requireObject(rule, '本场规则')
  return {
    players: integer(rule.playersPerTeam, '每队人数', 1),
    periods: integer(rule.regularPeriods, '常规节数', 1),
    period: minutesToMs(rule.periodMinutes, '每节时长'),
    halftimeAfter: integer(rule.halftimeAfterPeriod, '中场位置', 0),
    shot: secondsToMs(rule.shotClockSeconds, '进攻钟'),
    clearMs: secondsToMs(rule.clearSeconds, '推进时间'),
    points: integer(rule.pointsPerGoal, '进球分值', 1),
    breakMs: minutesToMs(rule.periodBreakMinutes, '节间休息'),
    halftimeMs: minutesToMs(rule.halftimeMinutes, '中场休息'),
    timeout: secondsToMs(rule.timeoutSeconds, '暂停时长'),
    timeoutsPerHalf: integer(rule.timeoutsPerHalf, '半场暂停次数', 0),
    overtimeTimeouts: integer(rule.overtimeTimeouts, '加时暂停次数', 0),
    stopWindow: secondsToMs(rule.stopWindowSeconds, '停表窗口'),
    allDeadStop: flag(rule.allDeadStop),
    foulStop: flag(rule.foulStop),
    overtimeStopAll: flag(rule.overtimeStopAll),
    shotLastOff: flag(rule.shotLastOff),
    overtimePolicy: code(rule.overtimePolicy, ['always', 'knockout', 'none'], '加时策略'),
    overtime: minutesToMs(rule.overtimeMinutes, '加时时长'),
    goldenGoal: flag(rule.goldenGoal),
    overtimeBreak: minutesToMs(rule.overtimeBreakMinutes, '加时休息'),
    minor: secondsToMs(rule.minorPenaltySeconds, '绿牌罚时'),
    major: secondsToMs(rule.majorPenaltySeconds, '黄牌罚时'),
    expulsion: secondsToMs(rule.expulsionPenaltySeconds, '红牌代罚'),
    majorLimit: integer(rule.majorLimit, '重大犯规上限', 1),
    goalieProxy: flag(rule.goalieProxy),
    passRequired: flag(rule.passRequired),
    alternateFirst: code(rule.alternateFirst, ['manual', 'home', 'away'], '首次交替球权'),
    alternateReset: flag(rule.alternateReset),
  }
}

/** 将前端规则（毫秒）转换为后端本场规则（分钟或秒）。 */
export function toScoreboardRuleRequest(rules) {
  requireObject(rules, '本场规则')
  return {
    playersPerTeam: rules.players,
    regularPeriods: rules.periods,
    periodMinutes: millisecondsToMinutes(rules.period, '每节时长'),
    halftimeAfterPeriod: rules.halftimeAfter,
    shotClockSeconds: millisecondsToSeconds(rules.shot),
    clearSeconds: millisecondsToSeconds(rules.clearMs),
    pointsPerGoal: rules.points,
    periodBreakMinutes: millisecondsToMinutes(rules.breakMs, '节间休息'),
    halftimeMinutes: millisecondsToMinutes(rules.halftimeMs, '中场休息'),
    timeoutSeconds: millisecondsToSeconds(rules.timeout),
    timeoutsPerHalf: rules.timeoutsPerHalf,
    overtimeTimeouts: rules.overtimeTimeouts,
    stopWindowSeconds: millisecondsToSeconds(rules.stopWindow),
    allDeadStop: flagNumber(rules.allDeadStop),
    foulStop: flagNumber(rules.foulStop),
    overtimeStopAll: flagNumber(rules.overtimeStopAll),
    shotLastOff: flagNumber(rules.shotLastOff),
    overtimePolicy: rules.overtimePolicy,
    overtimeMinutes: millisecondsToMinutes(rules.overtime, '加时时长'),
    goldenGoal: flagNumber(rules.goldenGoal),
    overtimeBreakMinutes: millisecondsToMinutes(rules.overtimeBreak, '加时休息'),
    minorPenaltySeconds: millisecondsToSeconds(rules.minor),
    majorPenaltySeconds: millisecondsToSeconds(rules.major),
    expulsionPenaltySeconds: millisecondsToSeconds(rules.expulsion),
    majorLimit: rules.majorLimit,
    goalieProxy: flagNumber(rules.goalieProxy),
    passRequired: flagNumber(rules.passRequired),
    alternateFirst: rules.alternateFirst,
    alternateReset: flagNumber(rules.alternateReset),
  }
}

export function toMatchInfoUpdateRequest(match, values) {
  return {
    title: String(values.title).trim(),
    matchNo: match.config.matchNo || null,
    matchDate: values.date,
    matchTime: match.config.matchTime || null,
    venueName: match.config.venueName || null,
    homeTeamName: String(values.home).trim(),
    homeTeamColor: match.config.homeTeamColor || null,
    awayTeamName: String(values.away).trim(),
    awayTeamColor: match.config.awayTeamColor || null,
  }
}

export function toMatchCreateRequest(values) {
  return {
    title: String(values.title || 'SLL 棍网球超级联赛').trim(),
    matchDate: values.date,
    ageGroup: String(values.ageGroup || '').trim(),
    groupRuleId: values.groupRuleId ? Number(values.groupRuleId) : null,
    saveAsTemplate: Boolean(values.saveAsTemplate),
    templateName: values.saveAsTemplate ? String(values.templateName || '').trim() : null,
    stage: values.stage || 'league',
    homeTeamName: String(values.home).trim(),
    awayTeamName: String(values.away).trim(),
    rules: toScoreboardRuleRequest(values.rules),
    roster: [],
    coaches: [],
    officials: [],
  }
}

/** 将前端即时比赛状态转换为后端状态保存请求。 */
export function toMatchStateSaveRequest(match, detail = {}) {
  requireObject(match, '当前比赛')
  const roster = Array.isArray(detail.roster) ? detail.roster : []
  const timeoutContext = match.timeoutContext
  return {
    homeScore: match.scores.home,
    awayScore: match.scores.away,
    phase: backendPhase(match.phase),
    currentPeriod: match.period,
    mainRemainingSeconds: millisecondsToSeconds(match.main),
    shotRemainingSeconds: millisecondsToSeconds(match.shot),
    clearRemainingSeconds: match.clear === null ? null : millisecondsToSeconds(match.clear),
    auxiliaryType: auxiliaryType(match),
    auxiliaryRemainingSeconds: millisecondsToSeconds(match.auxiliary),
    timeoutEventId: timeoutContext ? eventDatabaseId(match, timeoutContext.timeoutId) : null,
    timeoutReturnPhase: timeoutContext ? backendPhase(timeoutContext.phase) : null,
    timeoutReturnAuxiliaryType: timeoutContext?.auxiliaryType || null,
    timeoutReturnAuxiliarySeconds: timeoutContext ? millisecondsToSeconds(timeoutContext.auxiliary) : 0,
    timeoutReturnAuxiliaryRunning: flagNumber(timeoutContext?.auxiliaryRunning),
    mainRunning: flagNumber(match.running),
    auxiliaryRunning: flagNumber(match.auxiliaryRunning),
    ballState: match.ball,
    possessionSide: match.possession,
    frontcourt: flagNumber(match.frontcourt),
    alternateSide: match.alternate,
    resetShotOnResume: flagNumber(match.resetOnResume),
    goldenGoal: flagNumber(match.goldenGoal),
    goldenConflict: flagNumber(match.goldenConflict),
    pendingGoalSequenceNo: match.pendingGoal?.id ?? null,
    pendingGoalSide: match.pendingGoal?.team ?? null,
    pendingGoalPeriod: match.pendingGoal?.period ?? null,
    pendingGoalRemainingSeconds: match.pendingGoal ? millisecondsToSeconds(match.pendingGoal.main) : null,
    pendingGoalRosterId: match.pendingGoal
      ? rosterId(roster, match.pendingGoal.team, match.pendingGoal.player)
      : null,
    pendingGoalAssistRosterId: match.pendingGoal
      ? rosterId(roster, match.pendingGoal.team, match.pendingGoal.assist)
      : null,
    homeTimeoutsRemaining: match.timeouts.home,
    awayTimeoutsRemaining: match.timeouts.away,
    events: match.events
      .filter(event => !event.databaseId && !event.voided)
      .map(event => toBackendEvent(event, match, roster)),
    eventStatuses: eventStatusUpdates(match),
    penalties: match.penalties
      .filter(penalty => penalty.databaseId)
      .map(penalty => ({
        id: penalty.databaseId,
        remainingSeconds: millisecondsToSeconds(penalty.remaining),
        penaltyState: backendPenaltyState(penalty),
      })),
  }
}

/** 将状态保存响应中的数据库 ID 回填到当前前端比赛对象。 */
export function applyMatchStateSaveResult(match, result, detail = {}) {
  requireObject(result?.state, '状态保存结果')
  const roster = new Map((detail.roster || []).map(item => [String(item.id), item]))
  const sourceEvents = mergeSourceEvents(detail.events, result.events)
  const sequenceByDatabaseId = new Map(sourceEvents.map(event => [String(event.id), event.sequenceNo]))
  const penaltyByEventId = new Map()
  for (const sourceEvent of result.events || []) {
    const event = match.events.find(item => item.id === sourceEvent.sequenceNo)
    if (event) {
      const penalty = match.penalties.find(item => item.id === (event.category === 'penalty-correction'
        ? event.targetId
        : event.id))
      if (penalty) penaltyByEventId.set(String(sourceEvent.id), penalty)
      Object.assign(event, toFrontendEvent(sourceEvent, match, roster, sequenceByDatabaseId))
    }
  }
  for (const sourcePenalty of result.penalties || []) {
    let penalty = match.penalties.find(item => item.databaseId === sourcePenalty.id)
    if (!penalty) {
      const sourceEvent = (result.events || [])
        .find(event => event.id === sourcePenalty.penaltyEventId)
      if (sourceEvent) {
        const localEvent = match.events.find(item => item.id === sourceEvent.sequenceNo)
        penalty = penaltyByEventId.get(String(sourceEvent.id))
          || match.penalties.find(item => item.id === sourceEvent.sequenceNo)
      }
    }
    if (penalty) {
      const sourceEvent = (result.events || []).find(event => event.id === sourcePenalty.penaltyEventId)
      penalty.databaseId = sourcePenalty.id
      if (sourceEvent) penalty.id = sourceEvent.sequenceNo
    }
  }
  if (match.timeoutContext && result.state.timeoutEventId) {
    const timeoutEvent = match.events.find(event => event.databaseId === result.state.timeoutEventId)
    if (timeoutEvent) match.timeoutContext.timeoutId = timeoutEvent.id
  }
}

function mergeSourceEvents(current = [], changed = []) {
  const values = new Map(current.map(event => [String(event.id), event]))
  for (const event of changed || []) values.set(String(event.id), event)
  return [...values.values()]
}

function toFrontendEvent(event, match, roster, sequenceByDatabaseId) {
  const id = integer(event.sequenceNo, '事件顺序号', 1)
  const periodNumber = integer(event.periodNo ?? 1, '事件节次', 1)
  const correction = event.eventType === 'CORRECTION' ? correctionValues(event) : null
  const category = correction?.category || eventCategory(event.eventType)
  const status = event.eventStatus || 'VALID'
  const mapped = {
    id,
    databaseId: event.id,
    period: periodLabel(match.rules, periodNumber),
    periodNumber,
    time: clock(secondsToMs(event.remainingSeconds ?? 0, '事件剩余时间')),
    remaining: secondsToMs(event.remainingSeconds ?? 0, '事件剩余时间'),
    durationSeconds: event.durationSeconds ?? undefined,
    text: String(event.eventText || ''),
    rule: String(event.ruleReference || ''),
    category,
    team: event.teamSide || undefined,
    player: String(event.subjectName || '').trim() || undefined,
    voided: status !== 'VALID',
    cancelled: status === 'CANCELLED',
    eventType: event.eventType,
    eventStatus: status,
    occurredAt: event.occurredAt,
  }
  mapped.title = resolveEventTitle({ ...mapped, title: event.eventTitle }, match)
  if (event.relatedEventId !== null && event.relatedEventId !== undefined) {
    mapped.targetId = sequenceByDatabaseId.get(String(event.relatedEventId))
  }
  if (category === 'goal') {
    mapped.player = rosterLabel(roster, event.rosterId) || String(event.subjectName || '').trim()
    mapped.assist = rosterLabel(roster, event.assistRosterId) || String(event.assistName || '').trim()
    mapped.points = event.points ?? match.rules.points
  }
  if (category === 'timeout') {
    mapped.condition = timeoutCondition(event.timeoutCondition)
    mapped.timeoutBucket = timeoutBucket(event.timeoutBucket, periodNumber)
  }
  if (category === 'penalty') {
    mapped.player = event.penaltySubject === 'official'
      ? String(event.subjectName || '球队官员')
      : rosterLabel(roster, event.rosterId) || String(event.subjectName || '').trim()
    mapped.server = rosterLabel(roster, event.servedByRosterId)
      || String(event.servedByName || '').trim()
    mapped.subject = event.penaltySubject || 'player'
    mapped.kind = event.penaltyType === 'turnover' ? 'minor-turnover' : event.penaltyType
    mapped.reason = String(event.foulItem || '')
  }
  if (event.correctionReason) mapped.correctionReason = event.correctionReason
  if (correction) {
    mapped.before = correction.before
    mapped.changes = correction.changes
  }
  return mapped
}

function toBackendEvent(event, match, roster) {
  const eventType = backendEventType(event, match)
  const data = event.category === 'penalty-correction' && !event.changes?.cancelled
    ? event.changes
    : event
  const payload = {
    eventType,
    eventTitle: trimText(resolveEventTitle(event, match), 100),
    ruleReference: trimText(event.rule, 100) || undefined,
    periodNo: event.periodNumber ?? match.period,
    remainingSeconds: millisecondsToSeconds(event.remaining ?? match.main),
    occurredAt: event.occurredAt || undefined,
    teamSide: data.team || undefined,
  }
  if (eventType === 'GOAL') {
    payload.rosterId = rosterId(roster, event.team, event.player)
    payload.assistRosterId = rosterId(roster, event.team, event.assist)
    payload.subjectName = payload.rosterId ? undefined : trimText(event.player, 100)
    payload.assistName = payload.assistRosterId ? undefined : trimText(event.assist, 100)
    requireAdapter(payload.rosterId || payload.subjectName, '请填写进球球员号码或姓名。')
    payload.points = event.points ?? match.rules.points
  } else if (eventType === 'TEAM_TIMEOUT') {
    payload.durationSeconds = event.durationSeconds || millisecondsToSeconds(match.rules.timeout)
    payload.timeoutBucket = backendTimeoutBucket(event.timeoutBucket)
    payload.timeoutCondition = backendTimeoutCondition(event.condition)
  } else if (eventType === 'PENALTY') {
    payload.penaltySubject = data.subject || 'player'
    payload.penaltyType = data.kind === 'minor-turnover' ? 'turnover' : data.kind
    payload.rosterId = payload.penaltySubject === 'official'
      ? null
      : rosterId(roster, data.team, data.player)
    payload.subjectName = payload.penaltySubject === 'official' || !payload.rosterId
      ? trimText(data.player, 100)
      : undefined
    payload.servedByRosterId = payload.penaltyType === 'turnover'
      ? null
      : rosterId(roster, data.team, data.server)
    payload.servedByName = payload.penaltyType === 'turnover' || payload.servedByRosterId
      ? undefined
      : trimText(data.server, 100)
    requireAdapter(payload.rosterId || payload.subjectName,
      '请填写被判罚球员号码或姓名。')
    requireAdapter(payload.penaltyType === 'turnover'
      || payload.servedByRosterId || payload.servedByName,
      '请填写代罚球员号码或姓名。')
    payload.penaltySeconds = payload.penaltyType === 'turnover'
      ? 0
      : millisecondsToSeconds(match.rules[data.kind])
    payload.description = event.clockStopped || payload.penaltyType !== 'turnover'
      ? '计时停止'
      : undefined
    payload.penaltyRemainingSeconds = event.category === 'penalty-correction'
      ? millisecondsToSeconds(match.penalties.find(item => item.id === event.targetId)?.remaining)
      : payload.penaltySeconds
    const replacementPenalty = match.penalties.find(item => item.id === event.targetId)
    payload.penaltyState = replacementPenalty ? backendPenaltyState(replacementPenalty) : 'WAITING'
    payload.foulItem = data.reason || undefined
    if (event.category === 'penalty-correction') {
      payload.relatedEventId = eventDatabaseId(match, event.targetId)
      payload.correctionReason = trimText(event.correctionReason, 200)
    }
  } else if (eventType === 'CORRECTION') {
    payload.relatedEventId = eventDatabaseId(match, event.targetId)
    payload.correctionReason = trimText(event.correctionReason || event.reason || '裁判更正', 200)
    payload.beforeValue = compactJson(correctionSnapshot(event.before), 255)
    payload.afterValue = compactJson({
      _category: event.category,
      ...correctionSnapshot(event.changes),
    }, 255)
  } else if (eventType === 'SYSTEM') {
    payload.description = trimText(event.text || '比赛状态更新', 480)
    payload.subjectName = trimText(event.player, 100) || undefined
  }
  return payload
}

function backendEventType(event, match) {
  if (event.category === 'goal') return 'GOAL'
  if (event.category === 'timeout') return 'TEAM_TIMEOUT'
  if (event.category === 'penalty') return 'PENALTY'
  if (event.category === 'penalty-correction' && eventDatabaseId(match, event.targetId)
    && !event.changes?.cancelled) return 'PENALTY'
  if (['goal-detail', 'penalty-correction', 'timeout-correction', 'score-correction', 'goal-ruling'].includes(event.category)
    && eventDatabaseId(match, event.targetId)) return 'CORRECTION'
  if (isMatchEventType(event.eventType)) {
    return event.eventType
  }
  return 'SYSTEM'
}

function periodStarted(state, events, currentPeriod) {
  const hasStartEvent = events.some(event => startsPeriod(event.eventType)
    && event.periodNo === currentPeriod
    && event.eventStatus !== 'CANCELLED')
  if (hasStartEvent) return true
  if (state.phase === 'PERIOD') return true
  return state.phase === 'TIMEOUT' && state.timeoutReturnPhase === 'PERIOD'
}

function eventStatusUpdates(match) {
  const desired = new Map()
  for (const event of match.events) {
    if (event.databaseId && event.voided) desired.set(event.databaseId, 'CANCELLED')
    if (event.voided) continue
    if (['penalty-correction', 'timeout-correction'].includes(event.category)) {
      const targetId = eventDatabaseId(match, event.targetId)
      if (targetId) desired.set(targetId, event.changes?.cancelled ? 'CANCELLED' : 'REPLACED')
    }
    if (event.category === 'goal-ruling') {
      for (const sequenceNo of event.invalidGoalIds || []) {
        const targetId = eventDatabaseId(match, sequenceNo)
        if (targetId) desired.set(targetId, 'CANCELLED')
      }
    }
  }
  return [...desired].flatMap(([id, eventStatus]) => {
    const source = match.events.find(event => event.databaseId === id)
    return source?.eventStatus === eventStatus ? [] : [{ id, eventStatus }]
  })
}

function correctionValues(event) {
  const before = parseJson(event.beforeValue)
  const after = parseJson(event.afterValue)
  const category = after?._category
  if (!['goal-detail', 'penalty-correction', 'timeout-correction', 'score-correction', 'goal-ruling'].includes(category)) {
    return null
  }
  delete after._category
  return { category, before: before || {}, changes: after }
}

function parseJson(value) {
  if (!value) return {}
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

function compactJson(value, maximum) {
  const text = JSON.stringify(value)
  requireAdapter(text.length <= maximum, '更正内容过长，请精简犯规备注后重试。')
  return text
}

function correctionSnapshot(value = {}) {
  const keys = [
    'team', 'player', 'assist', 'kind', 'subject', 'server', 'timeoutBucket',
    'cancelled', 'home', 'away', 'points',
  ]
  return Object.fromEntries(keys
    .filter(key => value[key] !== undefined)
    .map(key => [key, value[key]]))
}

function rosterId(roster, teamSide, value) {
  const target = String(value || '').trim()
  if (!target) return null
  const matches = roster.filter(item => item.teamSide === teamSide
    && [item.jerseyNo, item.playerName, `${item.jerseyNo}号${item.playerName}`]
      .some(candidate => String(candidate ?? '').trim() === target))
  return matches.length === 1 ? matches[0].id : null
}

function eventDatabaseId(match, sequenceNo) {
  if (sequenceNo === null || sequenceNo === undefined) return null
  return match.events.find(event => event.id === Number(sequenceNo))?.databaseId ?? null
}

function backendPhase(phase) {
  const value = Object.entries(PHASES).find(([, frontend]) => frontend === phase)?.[0]
  requireAdapter(value, '比赛阶段不正确。')
  return value
}

function auxiliaryType(match) {
  if (match.phase === 'timeout') return 'TIMEOUT'
  if (match.phase !== 'break') return null
  if (match.period >= match.rules.periods) return 'OVERTIME_BREAK'
  return match.period === match.rules.halftimeAfter ? 'HALFTIME' : 'BREAK'
}

function backendPenaltyState(penalty) {
  if (penalty.released) return 'RELEASED'
  if (penalty.remaining === 0) return 'EXPIRED'
  if (penalty.started) return 'ACTIVE'
  if (penalty.seated) return 'SEATED'
  return 'WAITING'
}

function backendTimeoutBucket(value) {
  if (value === 'first') return 'FIRST_HALF'
  if (value === 'second') return 'SECOND_HALF'
  return value?.startsWith('overtime-') ? 'OVERTIME' : undefined
}

function backendTimeoutCondition(value) {
  return ({
    'before-start': 'BEFORE_START',
    halftime: 'HALFTIME',
    frontcourt: 'FRONTCOURT',
    'dead-ball': 'DEAD_BALL',
  })[value]
}

function millisecondsToSeconds(value) {
  return Math.ceil(Math.max(0, Number(value) || 0) / 1000)
}

function millisecondsToMinutes(value, label) {
  const minutes = Math.max(0, Number(value) || 0) / 60000
  requireAdapter(Number.isInteger(minutes), `${label}必须是整数分钟。`)
  return minutes
}

function flagNumber(value) {
  return value ? 1 : 0
}

function trimText(value, maximum) {
  return String(value || '').slice(0, maximum)
}

function toFrontendPenalties(penalties, sourceEvents, roster) {
  const eventByDatabaseId = new Map(sourceEvents.map(event => [String(event.id), event]))
  return penalties.map(penalty => {
    const event = eventByDatabaseId.get(String(penalty.penaltyEventId))
    requireAdapter(event?.eventType === 'PENALTY', '罚时缺少对应的判罚事件。')
    const state = code(penalty.penaltyState, ['WAITING', 'SEATED', 'ACTIVE', 'EXPIRED', 'RELEASED'], '罚时状态')
    return {
      id: integer(event.sequenceNo, '罚时事件顺序号', 1),
      databaseId: penalty.id,
      team: code(event.teamSide, ['home', 'away'], '犯规球队'),
      player: event.penaltySubject === 'official'
        ? String(event.subjectName || '球队官员')
        : rosterLabel(roster, event.rosterId) || String(event.subjectName || '').trim(),
      server: rosterLabel(roster, event.servedByRosterId)
        || String(event.servedByName || '').trim(),
      subject: event.penaltySubject || 'player',
      kind: code(event.penaltyType, ['minor', 'major', 'expulsion'], '判罚类型'),
      reason: String(event.foulItem || ''),
      remaining: secondsToMs(penalty.remainingSeconds, '罚时剩余时间'),
      seated: state !== 'WAITING',
      started: ['ACTIVE', 'EXPIRED', 'RELEASED'].includes(state),
      released: state === 'RELEASED',
    }
  })
}

function toTimeoutContext(state, sequenceByDatabaseId) {
  if (state.phase !== 'TIMEOUT') return null
  return {
    phase: toPhase(state.timeoutReturnPhase || 'PERIOD'),
    auxiliary: secondsToMs(state.timeoutReturnAuxiliarySeconds ?? 0, '暂停前辅助时间'),
    auxiliaryRunning: flag(state.timeoutReturnAuxiliaryRunning),
    auxiliaryType: state.timeoutReturnAuxiliaryType || null,
    timeoutId: state.timeoutEventId == null
      ? null
      : sequenceByDatabaseId.get(String(state.timeoutEventId)) ?? null,
  }
}

function toPendingGoal(state, roster) {
  if (state.pendingGoalSequenceNo === null || state.pendingGoalSequenceNo === undefined) return null
  return {
    id: integer(state.pendingGoalSequenceNo, '待确认进球顺序号', 1),
    team: code(state.pendingGoalSide, ['home', 'away'], '待确认进球队伍'),
    period: integer(state.pendingGoalPeriod, '待确认进球节次', 1),
    main: secondsToMs(state.pendingGoalRemainingSeconds, '待确认进球时间'),
    player: rosterLabel(roster, state.pendingGoalRosterId),
    assist: rosterLabel(roster, state.pendingGoalAssistRosterId),
  }
}

function applyTimerAnchor(match, timerStartedAt, now) {
  if (!timerStartedAt || (!match.running && !match.auxiliaryRunning)) return
  const startedAt = Date.parse(timerStartedAt)
  requireAdapter(Number.isFinite(startedAt), '计时启动时间不正确。')
  const elapsed = Math.max(0, now - startedAt)
  if (!elapsed) return
  if (match.auxiliaryRunning) {
    match.auxiliary = Math.max(0, match.auxiliary - elapsed)
    if (match.auxiliary === 0) {
      match.auxiliaryRunning = false
      match.alert = '暂停／休息时间到，等待裁判指令。'
    }
    return
  }
  if (!match.running || match.phase !== 'period') return
  const delta = Math.min(elapsed, match.main)
  match.main = Math.max(0, match.main - delta)
  const shotRunning = match.rules.shot > 0 && match.ball === 'live' && match.possession && match.shot > 0
  if (shotRunning) match.shot = Math.max(0, match.shot - delta)
  if (match.ball === 'live' && match.clear !== null) match.clear = Math.max(0, match.clear - delta)
  for (const penalty of match.penalties) {
    if (penalty.started && !penalty.released) penalty.remaining = Math.max(0, penalty.remaining - delta)
  }
  if (match.main === 0) {
    match.running = false
    match.ball = 'dead'
    match.clear = null
    match.phase = 'period-end'
    match.alert = `${periodLabel(match.rules, match.period)}时间到，请核对最后一次射门与比分。`
  }
}

function toPhase(value) {
  const phase = PHASES[value]
  requireAdapter(phase, '比赛阶段不正确。')
  return phase
}

function periodLabel(rules, period) {
  if (period > rules.periods) return `加时 ${period - rules.periods}`
  if (rules.periods === 2) return period === 1 ? '上半场' : '下半场'
  return `第 ${period} 节`
}

function rosterLabel(roster, id) {
  if (id === null || id === undefined) return ''
  const player = roster.get(String(id))
  requireAdapter(player, '比赛记录引用了不存在的本场球员。')
  return player.jerseyNo === null || player.jerseyNo === undefined
    ? String(player.playerName || '').trim()
    : String(player.jerseyNo)
}

function timeoutCondition(value) {
  return ({ BEFORE_START: 'before-start', HALFTIME: 'halftime', FRONTCOURT: 'frontcourt', DEAD_BALL: 'dead-ball' })[value] || ''
}

function timeoutBucket(value, period) {
  return ({ FIRST_HALF: 'first', SECOND_HALF: 'second' })[value]
    || (value === 'OVERTIME' ? `overtime-${period}` : '')
}

function minutesToMs(value, label) {
  return integer(value, label, 0) * 60000
}

function secondsToMs(value, label) {
  return integer(value, label, 0) * 1000
}

function integer(value, label, minimum) {
  requireAdapter(Number.isInteger(value) && value >= minimum, `${label}不正确。`)
  return value
}

function flag(value) {
  if (value === null || value === undefined) return false
  requireAdapter(value === 0 || value === 1 || typeof value === 'boolean', '布尔状态不正确。')
  return value === 1 || value === true
}

function code(value, values, label) {
  requireAdapter(values.includes(value), `${label}不正确。`)
  return value
}

function sideCode(value, values, label, fallback) {
  if (value === null || value === undefined || value === '') return fallback
  return code(String(value).toLowerCase(), values, label)
}

function requireDetail(detail) {
  requireObject(detail, '比赛详情')
  requireObject(detail.match, '比赛信息')
  requireObject(detail.rule, '本场规则')
  requireObject(detail.state, '比赛状态')
  requireAdapter(Array.isArray(detail.roster) && Array.isArray(detail.events)
    && Array.isArray(detail.penalties), '比赛详情列表不完整。')
}

function requireObject(value, label) {
  requireAdapter(value && typeof value === 'object' && !Array.isArray(value), `${label}不完整。`)
}

function requireAdapter(ok, message) {
  if (!ok) throw new Error(message)
}
