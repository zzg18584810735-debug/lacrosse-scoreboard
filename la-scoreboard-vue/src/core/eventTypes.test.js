import test from 'node:test'
import assert from 'node:assert/strict'
import {
  eventCategory,
  isMatchEventType,
  MATCH_EVENT_DEFINITIONS,
  MATCH_EVENT_TYPE,
  resolveEventTitle,
  startsPeriod,
} from './eventTypes.js'

test('事件 enum 与定义集中维护且保持冻结', () => {
  assert.equal(Object.isFrozen(MATCH_EVENT_TYPE), true)
  assert.equal(Object.isFrozen(MATCH_EVENT_DEFINITIONS), true)
  assert.deepEqual(Object.keys(MATCH_EVENT_DEFINITIONS), Object.values(MATCH_EVENT_TYPE))
  assert.equal(isMatchEventType(MATCH_EVENT_TYPE.GOAL), true)
  assert.equal(isMatchEventType('UNKNOWN'), false)
})

test('事件定义提供分类和开节语义', () => {
  assert.equal(eventCategory(MATCH_EVENT_TYPE.GOAL), 'goal')
  assert.equal(eventCategory(MATCH_EVENT_TYPE.TEAM_TIMEOUT), 'timeout')
  assert.equal(eventCategory(MATCH_EVENT_TYPE.PENALTY), 'penalty')
  assert.equal(eventCategory('UNKNOWN'), 'system')
  assert.equal(startsPeriod(MATCH_EVENT_TYPE.MATCH_START), true)
  assert.equal(startsPeriod(MATCH_EVENT_TYPE.PERIOD_START), true)
  assert.equal(startsPeriod(MATCH_EVENT_TYPE.PERIOD_END), false)
})

test('事件标题优先使用结构化事件和球队，系统操作从详细说明提取简要标题', () => {
  const match = { config: { home: '甲队', away: '乙队' } }
  assert.equal(resolveEventTitle({ eventType: MATCH_EVENT_TYPE.POSSESSION, team: 'away' }, match), '乙队获得球权')
  assert.equal(resolveEventTitle({ eventType: MATCH_EVENT_TYPE.SYSTEM, text: '裁判暂停：比赛钟停止。' }, match), '裁判暂停')
})
