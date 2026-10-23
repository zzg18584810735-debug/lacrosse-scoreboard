import { createMatch } from './matchModel.js'
import { holdGoldenConflict, reconcileMatch } from './matchOutcome.js'
import { freeze, isOvertime, periodHasStarted } from './timing.js'

export function restoreMatch(value) {
  requireRecovery(value?.version === 4, '当前记录格式不兼容。')
  const expected = createMatch({ ...value.config, rules: value.rules })
  requireRecovery(sameRuleFields(expected.rules, value.rules), '规则字段损坏。')
  const finite = item => typeof item === 'number' && Number.isFinite(item) && item >= 0

  requireRecovery(value.config && ['title', 'home', 'away', 'date'].every(key => typeof value.config[key] === 'string'), '比赛信息损坏。')
  requireRecovery(['ready', 'period', 'timeout', 'break', 'period-end', 'finished'].includes(value.phase)
    && Number.isInteger(value.period) && value.period >= 1, '比赛阶段损坏。')
  requireRecovery(['main', 'shot', 'auxiliary', 'sequence'].every(key => finite(value[key]))
    && ['home', 'away'].every(team => finite(value.scores?.[team])
      && Number.isInteger(value.timeouts?.[team]) && value.timeouts[team] >= 0), '时钟或比分损坏。')
  requireRecovery(value.main <= (isOvertime(value) ? value.rules.overtime : value.rules.period)
    && value.shot <= value.rules.shot
    && (value.clear === null || finite(value.clear)), '时钟范围损坏。')
  requireRecovery(['dead', 'live'].includes(value.ball)
    && [null, 'home', 'away'].includes(value.possession)
    && [null, 'home', 'away'].includes(value.alternate)
    && ['running', 'auxiliaryRunning', 'resetOnResume', 'frontcourt', 'goldenGoal']
      .every(key => typeof value[key] === 'boolean'), '球权状态损坏。')
  requireRecovery(Array.isArray(value.penalties) && value.penalties.every(penalty => finite(penalty.remaining)
    && finite(penalty.id)
    && ['home', 'away'].includes(penalty.team)
    && typeof penalty.player === 'string'
    && typeof penalty.server === 'string'
    && ['minor', 'major', 'expulsion'].includes(penalty.kind)
    && ['player', 'goalie', 'official'].includes(penalty.subject)
    && ['seated', 'started', 'released'].every(key => typeof penalty[key] === 'boolean')), '罚时损坏。')
  requireRecovery(Array.isArray(value.events)
    && value.events.every(event => event
      && typeof event.text === 'string'
      && typeof event.time === 'string'
      && typeof event.period === 'string'
      && finite(event.id))
    && Array.isArray(value.disqualified)
    && value.disqualified.every(key => typeof key === 'string')
    && value.majors && typeof value.majors === 'object', '比赛记录损坏。')
  requireRecovery(value.timeoutContext === null
    || (value.phase === 'timeout'
      && ['ready', 'period', 'break'].includes(value.timeoutContext?.phase)
      && finite(value.timeoutContext.auxiliary)), '暂停状态损坏。')

  if (value.pendingGoal) validatePendingGoal(value, finite)
  requireRecovery(value.goldenConflict === undefined || typeof value.goldenConflict === 'boolean', '金球核对状态损坏。')
  requireRecovery(value.events.every(event => event.category !== 'goal-ruling'
    || (Array.isArray(event.invalidGoalIds)
      && event.invalidGoalIds.every(id => Number.isInteger(id) && id > 0))), '金球核对记录损坏。')
  requireRecovery(value.periodStarted === undefined || typeof value.periodStarted === 'boolean', '本节开赛状态损坏。')

  const restored = structuredClone(value)
  restored.periodStarted = periodHasStarted(restored)
  if (restored.goldenConflict) holdGoldenConflict(restored)
  reconcileMatch(restored)
  freeze(restored)
  restored.undo = null
  restored.notice = '已恢复本场比赛，所有时钟暂停；请核对后继续。'
  restored.rule = '本地恢复'
  return restored
}

function sameRuleFields(expected, actual) {
  if (!actual || typeof actual !== 'object' || Array.isArray(actual)) return false
  const expectedKeys = Object.keys(expected)
  const actualKeys = Object.keys(actual)
  return expectedKeys.length === actualKeys.length
    && expectedKeys.every(key => Object.hasOwn(actual, key) && actual[key] === expected[key])
}

function validatePendingGoal(value, finite) {
  const draft = value.pendingGoal
  requireRecovery(Number.isInteger(draft.id)
    && draft.id > 0
    && draft.id <= value.sequence
    && ['home', 'away'].includes(draft.team)
    && draft.period === value.period
    && finite(draft.main)
    && draft.main <= (draft.period > value.rules.periods ? value.rules.overtime : value.rules.period)
    && ['player', 'assist'].every(key => typeof draft[key] === 'string' && draft[key].length <= 20)
    && (draft.returnState === undefined
      || (typeof draft.returnState.running === 'boolean'
        && ['dead', 'live'].includes(draft.returnState.ball)
        && (draft.returnState.clear === null || finite(draft.returnState.clear))
        && typeof draft.returnState.frontcourt === 'boolean')), '待确认进球损坏。')
}

function requireRecovery(ok, message) {
  if (!ok) throw new Error(message)
}
