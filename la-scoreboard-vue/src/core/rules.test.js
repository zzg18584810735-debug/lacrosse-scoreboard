import assert from 'node:assert/strict'
import test from 'node:test'
import { PRESETS, resolveRules } from './rules.js'

test('规则模板解析返回可独立修改的本场规则', () => {
  const rules = resolveRules('sll-sixes', { period: 600000 })

  assert.equal(rules.period, 600000)
  assert.equal(PRESETS['sll-sixes'].period, 480000)
})

test('规则模板解析拒绝无效字段值', () => {
  assert.throws(() => resolveRules('sll-sixes', { periods: 0 }), /常规节数超出可用范围/)
})
