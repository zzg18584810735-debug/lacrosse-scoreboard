import { applyCorrectionAction } from './correctionActions.js'
import { applyFieldAction } from './fieldActions.js'
import { applyGoalAction } from './goalActions.js'
import { applyMatchInfoAction } from './matchInfoActions.js'
import { recordMatchEvent } from './matchEvents.js'
import { GOLDEN_CONFLICT_HINT, reconcileMatch } from './matchOutcome.js'
import { applyPenaltyAction } from './penaltyActions.js'
import { applyPeriodAction } from './periodActions.js'
import { applyTimeoutAction } from './timeoutActions.js'
import { freeze, isPeriodActive } from './timing.js'

const LIVE_ACTIONS = ['whistle', 'goal', 'reset-shot', 'wide', 'backcourt', 'frontcourt']

export function requireState(ok, message) {
  if (!ok) throw new Error(message)
}

/** 以副本执行单次计分动作，校验失败时保持原比赛状态不变。 */
export function act(match, type, payload = {}) {
  requireState(!match.goldenConflict
    || ['undo', 'resolve-golden', 'match-info', 'cancel-goal'].includes(type), GOLDEN_CONFLICT_HINT)
  requireState(!match.pendingGoal
    || ['confirm-goal', 'cancel-goal'].includes(type), '请先确认或取消正在录入的进球。')

  if (type === 'undo') {
    undo(match)
    return
  }

  const next = structuredClone(match)
  next.undo = null
  applyAction(next, type, payload)
  reconcileMatch(next)

  const previous = structuredClone(match)
  previous.undo = null
  previous.events = []
  previous.pendingGoal = null
  next.undo = previous
  Object.assign(match, next)
}

function undo(match) {
  requireState(match.undo, '还没有可撤销的操作。')
  const events = match.events
  const sequence = match.sequence
  const previous = match.undo
  for (const event of events) {
    if (event.id > previous.sequence) event.voided = true
  }
  Object.assign(match, structuredClone(previous))
  match.events = events
  match.sequence = sequence
  match.undo = null
  freeze(match)
  recordMatchEvent(match, '已撤销上一步，恢复该操作前的比分、球权与时钟。现在全部停表，请核对时间再恢复。', '4.3')
}

function applyAction(match, type, payload) {
  if (LIVE_ACTIONS.includes(type)) {
    requireState(isPeriodActive(match), '请先开始本节；节间或暂停中不能执行这个操作。')
  }
  if (payload.team !== undefined) {
    requireState(['home', 'away'].includes(payload.team), '请选择有效球队。')
  }

  if (applyGoalAction(match, type, payload)) return
  if (applyTimeoutAction(match, type, payload,
    (nestedType, nestedPayload) => applyAction(match, nestedType, nestedPayload))) return
  if (applyPenaltyAction(match, type, payload)) return
  if (applyFieldAction(match, type, payload)) return
  if (applyPeriodAction(match, type, payload)) return
  if (applyCorrectionAction(match, type, payload)) return
  if (applyMatchInfoAction(match, type, payload)) return
  throw new Error('未知操作。')
}
