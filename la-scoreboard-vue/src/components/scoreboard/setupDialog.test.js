import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from '../../core/matchModel.js'
import { groupedRuleFields, ruleFormValues, rulesFromForm } from './setupDialog.js'

test('规则表单使用分钟和秒，提交时恢复引擎毫秒', () => {
  const rules = createMatch({ preset: 'sll-sixes' }).rules
  const values = ruleFormValues(rules)
  assert.equal(values.period, 8)
  assert.equal(values.shot, 30)

  values.period = 10
  values.shot = 45
  const restored = rulesFromForm(values, rules)
  assert.equal(restored.period, 600_000)
  assert.equal(restored.shot, 45_000)
})

test('布尔和代码规则保持原业务类型', () => {
  const rules = createMatch({ preset: 'sll-sixes' }).rules
  const values = ruleFormValues(rules)
  values.allDeadStop = 'true'
  values.overtimePolicy = 'none'

  const restored = rulesFromForm(values, rules)
  assert.equal(restored.allDeadStop, true)
  assert.equal(restored.overtimePolicy, 'none')
  assert.ok(groupedRuleFields().some(group => group.name === '判罚与球权'))
})
