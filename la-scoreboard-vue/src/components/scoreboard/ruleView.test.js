import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from '../../core/matchModel.js'
import { ruleSummaryItems } from './ruleView.js'

test('规则摘要将内部毫秒转换为页面使用的分钟或秒', () => {
  const match = createMatch({ preset: 'sll-sixes' })
  const items = new Map(ruleSummaryItems(match.rules).map(item => [item.key, item.value]))

  assert.equal(items.get('period'), 8)
  assert.equal(items.get('shot'), 30)
  assert.equal(items.get('breakMs'), 2)
})

test('规则摘要将固定代码转换为中文含义', () => {
  const match = createMatch({ preset: 'sll-sixes' })
  const items = new Map(ruleSummaryItems(match.rules).map(item => [item.key, item.value]))

  assert.equal(items.get('overtimePolicy'), '所有比赛均加时')
  assert.equal(items.get('alternateFirst'), '由裁判确认')
})
