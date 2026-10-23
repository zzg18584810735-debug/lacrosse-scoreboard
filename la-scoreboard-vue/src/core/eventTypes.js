/**
 * 比赛事件类型。Vue 使用冻结对象模拟 enum，新增或删除事件节点时统一修改本文件。
 */
export const MATCH_EVENT_TYPE = Object.freeze({
  MATCH_START: 'MATCH_START',
  PERIOD_START: 'PERIOD_START',
  PERIOD_END: 'PERIOD_END',
  MATCH_END: 'MATCH_END',
  GOAL: 'GOAL',
  TEAM_TIMEOUT: 'TEAM_TIMEOUT',
  PENALTY: 'PENALTY',
  POSSESSION: 'POSSESSION',
  CORRECTION: 'CORRECTION',
  SYSTEM: 'SYSTEM',
})

/** 事件展示与状态恢复所需的集中配置。 */
export const MATCH_EVENT_DEFINITIONS = Object.freeze({
  [MATCH_EVENT_TYPE.MATCH_START]: definition('比赛开始', 'system', true),
  [MATCH_EVENT_TYPE.PERIOD_START]: definition('本节开始', 'system', true),
  [MATCH_EVENT_TYPE.PERIOD_END]: definition('本节结束', 'system'),
  [MATCH_EVENT_TYPE.MATCH_END]: definition('比赛结束', 'system'),
  [MATCH_EVENT_TYPE.GOAL]: definition('进球', 'goal'),
  [MATCH_EVENT_TYPE.TEAM_TIMEOUT]: definition('球队暂停', 'timeout'),
  [MATCH_EVENT_TYPE.PENALTY]: definition('判罚', 'penalty'),
  [MATCH_EVENT_TYPE.POSSESSION]: definition('球权', 'system'),
  [MATCH_EVENT_TYPE.CORRECTION]: definition('更正', 'system'),
  [MATCH_EVENT_TYPE.SYSTEM]: definition('系统记录', 'system'),
})

export function isMatchEventType(value) {
  return Object.hasOwn(MATCH_EVENT_DEFINITIONS, value)
}

export function eventCategory(value) {
  return MATCH_EVENT_DEFINITIONS[value]?.category || 'system'
}

export function startsPeriod(value) {
  return Boolean(MATCH_EVENT_DEFINITIONS[value]?.startsPeriod)
}

export function resolveEventTitle(event, match) {
  if (String(event.title || '').trim()) return String(event.title).trim()
  const teamName = event.team && match?.config?.[event.team]
  if (event.category === 'goal') return `${teamName || '球队'}进球`
  if (event.category === 'timeout') {
    const duration = Number(event.durationSeconds) > 0 ? `（${event.durationSeconds}秒）` : ''
    return `${teamName || '球队'}暂停${duration}`
  }
  if (event.category === 'penalty') return `${teamName || '球队'}判罚`
  if (event.eventType === MATCH_EVENT_TYPE.POSSESSION) return `${teamName || '球队'}获得球权`
  const configured = MATCH_EVENT_DEFINITIONS[event.eventType]?.label
  if (configured && event.eventType !== MATCH_EVENT_TYPE.SYSTEM) return configured
  const brief = String(event.text || '').split(/[：。；，]/, 1)[0].trim()
  return [...brief].slice(0, 30).join('') || '比赛操作'
}

function definition(label, category, startsPeriod = false) {
  return Object.freeze({ label, category, startsPeriod })
}
