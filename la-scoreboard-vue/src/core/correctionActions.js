import { recordMatchEvent } from './matchEvents.js'
import {
  confirmGoldenGoal,
  GOLDEN_CONFLICT_HINT,
  overtimeGoals,
  reconcileMatch,
} from './matchOutcome.js'
import { isYouth } from './rules.js'
import {
  canFinish,
  clock,
  endPeriod,
  freeze,
  isOvertime,
  periodHasStarted,
} from './timing.js'

const CORRECTION_ACTIONS = new Set(['final-score', 'resolve-golden', 'correct'])

export function applyCorrectionAction(match, type, payload = {}) {
  if (!CORRECTION_ACTIONS.has(type)) return false

  switch (type) {
    case 'final-score':
      correctFinalScore(match, payload)
      break
    case 'resolve-golden':
      resolveGoldenConflict(match, payload)
      break
    case 'correct':
      correctMatchState(match, payload)
      break
  }
  return true
}

function correctFinalScore(match, payload) {
  requireCorrection(match.phase === 'finished', '请在比赛确认结束后使用赛后比分更正。')
  const reason = correctionReason(payload.reason)
  requireScore(payload)
  requireCorrection(payload.home !== match.scores.home
    || payload.away !== match.scores.away, '比分没有变化。')

  const before = { ...match.scores }
  match.scores = { home: payload.home, away: payload.away }
  freeze(match)
  match.goldenGoal = Boolean(match.rules.goldenGoal
    && isOvertime(match)
    && periodHasStarted(match)
    && payload.home !== payload.away)
  match.phase = match.rules.goldenGoal
    && isOvertime(match)
    && payload.home === payload.away
    && match.main > 0
    ? 'period'
    : 'period-end'
  match.alert = match.phase === 'period'
    ? '比分恢复平局，金球结束已撤回；请按裁判指令复赛。'
    : canFinish(match)
      ? '赛后比分已更正，请重新核对并确认比赛结束。'
      : '比分恢复平局，请核对后按本场规则进入加时。'
  // 审计记录必须描述重算后的稳定结果，而不是中间状态。
  reconcileMatch(match)
  recordMatchEvent(
    match,
    `赛后比分更正：${before.home}:${before.away} → ${payload.home}:${payload.away}。依据：${reason}。原进球明细保留，未自动重写；${match.alert}`,
    '裁判更正',
    {
      category: 'score-correction',
      before,
      changes: { ...match.scores },
      reason,
    },
  )
}

function resolveGoldenConflict(match, payload) {
  requireCorrection(match.goldenConflict, '没有待核对的金球冲突。')
  const goals = overtimeGoals(match)
  const selected = goals.find(event => event.id === Number(payload.goalId))
  requireCorrection(payload.goalId === 'none' || selected, '请选择裁判确认的首个有效进球，或确认所有加时进球无效。')
  const reason = correctionReason(payload.reason, '请填写 200 字以内的裁判核对依据。')
  requireScore(payload)
  requireCorrection(
    selected
      ? payload[selected.team] - payload[otherTeam(selected.team)] === (selected.points ?? match.rules.points)
      : payload.home === payload.away,
    selected
      ? '金球应使该队从平局领先一个进球的分值，请核对总比分。'
      : '所有加时进球无效时应恢复平局，请核对总比分。',
  )

  const before = { ...match.scores }
  const invalidGoalIds = goals
    .filter(event => event.id !== selected?.id)
    .map(event => event.id)
  match.scores = { home: payload.home, away: payload.away }
  match.goldenConflict = false
  freeze(match)
  deadBall(match)
  if (selected) {
    confirmGoldenGoal(match)
  } else {
    match.goldenGoal = false
    match.phase = match.main > 0 ? 'period' : 'period-end'
    match.alert = '裁判确认加时进球均无效，已恢复平局并停表；请核对时间、球权和判罚后再复赛。'
  }
  recordMatchEvent(
    match,
    `金球冲突核对：${selected ? `首个有效进球为 #${selected.id} ${match.config[selected.team]} · ${selected.player}，其他加时进球作废` : '所有加时进球作废'}。比分 ${before.home}:${before.away} → ${payload.home}:${payload.away}。依据：${reason}。原记录保留；时钟未倒拨，请同时核对球权与判罚。`,
    isYouth(match) ? '5.1.2' : '本场金球规则',
    {
      category: 'goal-ruling',
      targetId: selected?.id ?? null,
      invalidGoalIds,
      before,
      changes: { ...match.scores },
      reason,
    },
  )
}

function correctMatchState(match, payload) {
  requireCorrection(match.phase !== 'finished', '比赛已结束，请使用“赛后更正比分”。')
  const reason = correctionReason(payload.reason)
  const scope = payload.scope
  let data = payload

  if (scope !== undefined) {
    requireCorrection(['main', 'shot', 'score', 'penalty'].includes(scope), '请选择要更正的项目。')
    requireCorrection(scope !== 'shot' || match.rules.shot > 0, '本场不使用进攻钟。')
    requireCorrection(!(['main', 'shot'].includes(scope) && match.phase === 'break'), '当前在节间休息；请先准备下一节，再核定比赛时间或进攻时间。')
    requireCorrection(!(scope === 'main' && match.phase === 'timeout' && payload.main === 0), '请先结束球队暂停，再将比赛时间更正为零并核对本节结束。')
    const fields = {
      main: ['main'],
      shot: ['shot'],
      score: ['home', 'away'],
      penalty: ['penaltyId', 'penaltySeconds'],
    }[scope]
    const supplied = Object.fromEntries(fields.map(key => [key, payload[key]]))
    data = {
      main: match.main,
      shot: match.shot,
      ...match.scores,
      reason,
      ...supplied,
    }
    requireChangedScope(match, scope, data)
  }

  const before = {
    main: match.main,
    shot: match.resetOnResume ? match.rules.shot : match.shot,
    ...match.scores,
  }
  const penaltyBefore = data.penaltyId
    ? structuredClone(match.penalties.find(item => item.id === Number(data.penaltyId)))
    : null
  requireCorrection(Number.isFinite(data.main)
    && data.main >= 0
    && data.main <= (isOvertime(match) ? match.rules.overtime : match.rules.period), '比赛时间超出本节范围。')
  requireCorrection(Number.isFinite(data.shot)
    && data.shot >= 0
    && data.shot <= match.rules.shot, match.rules.shot
    ? `进攻时间须为 0–${match.rules.shot / 1000} 秒。`
    : '本组不使用进攻钟，不能设置进攻时间。')
  requireScore(data)

  freeze(match)
  deadBall(match)
  match.main = data.main
  match.shot = data.shot
  if (scope === undefined || scope === 'shot') match.resetOnResume = false
  match.scores = { home: data.home, away: data.away }
  match.alert = ''
  if (match.phase === 'period-end' && match.main > 0 && !match.goldenGoal) match.phase = 'period'
  if (match.goldenGoal && match.scores.home === match.scores.away) {
    match.goldenGoal = false
    match.phase = match.main > 0 ? 'period' : 'period-end'
  }
  if (match.rules.goldenGoal
    && isOvertime(match)
    && periodHasStarted(match)
    && match.scores.home !== match.scores.away) {
    confirmGoldenGoal(match)
  }
  if (data.penaltyId) {
    const penalty = match.penalties.find(item => item.id === Number(data.penaltyId))
    requireCorrection(penalty
      && !penalty.released
      && Number.isFinite(data.penaltySeconds)
      && data.penaltySeconds >= 0
      && data.penaltySeconds <= 600, '罚时更正须为 0–600 秒且该条尚未释放。')
    penalty.remaining = data.penaltySeconds * 1000
  }

  const change = correctionDescription(match, scope, before, penaltyBefore, data)
  reconcileMatch(match)
  recordMatchEvent(
    match,
    `裁判更正：${reason}。${change}。${match.goldenConflict ? GOLDEN_CONFLICT_HINT : match.goldenGoal ? '金球立即停表，核对后结束比赛。' : '全部停表等待核对。'}`,
    match.goldenGoal && isYouth(match) ? '4.3 / 5.1.2' : '4.3 / 5.3.3',
  )
  if (match.main === 0 && ['ready', 'period'].includes(match.phase)) endPeriod(match)
}

function requireChangedScope(match, scope, data) {
  if (scope === 'penalty') {
    const target = match.penalties.find(item => item.id === Number(data.penaltyId) && !item.released)
    requireCorrection(target, '请先选择尚未释放的罚时；新增或作废判罚请到对应判罚记录操作。')
    requireCorrection(Math.abs(data.penaltySeconds * 1000 - target.remaining) >= 1, '剩余罚时没有变化，请核对后再保存。')
    return
  }
  const changed = scope === 'main'
    ? Math.abs(data.main - match.main) >= 1
    : scope === 'shot'
      ? Math.abs(data.shot - (match.resetOnResume ? match.rules.shot : match.shot)) >= 1
      : data.home !== match.scores.home || data.away !== match.scores.away
  requireCorrection(changed, '所选项目没有变化，请修改数值或取消。')
}

function correctionDescription(match, scope, before, penaltyBefore, data) {
  if (scope === 'main') return `比赛时间 ${clock(before.main)} → ${clock(match.main)}`
  if (scope === 'shot') return `复赛进攻时间 ${Math.round(before.shot) / 1000} → ${Math.round(match.shot) / 1000} 秒`
  if (scope === 'score') return `总比分 ${before.home}:${before.away} → ${data.home}:${data.away}（原进球明细保留）`
  if (scope === 'penalty') {
    return `${match.config[penaltyBefore.team]} ${penaltyBefore.server} 号剩余罚时 ${Math.round(penaltyBefore.remaining) / 1000} → ${data.penaltySeconds} 秒`
  }
  return `比分 ${data.home}:${data.away}，比赛钟 ${clock(match.main)}，进攻钟 ${clock(match.shot)}`
}

function correctionReason(value, message = '请填写 200 字以内的更正依据。') {
  const reason = String(value || '').trim()
  requireCorrection(reason && reason.length <= 200, message)
  return reason
}

function requireScore(payload) {
  requireCorrection(['home', 'away'].every(team => Number.isInteger(payload[team])
    && payload[team] >= 0
    && payload[team] <= 999), '比分须为 0–999 的整数。')
}

function deadBall(match) {
  match.ball = 'dead'
  match.clear = null
  match.running = false
}

function otherTeam(team) {
  return team === 'home' ? 'away' : 'home'
}

function requireCorrection(ok, message) {
  if (!ok) throw new Error(message)
}
