import { RULE_FIELDS } from '../../core/rules.js'

export function ruleFormValues(rules) {
  return Object.fromEntries(RULE_FIELDS.map(([key, , min, , scale]) => [
    key,
    typeof min === 'object' ? String(rules[key]) : rules[key] / scale,
  ]))
}

export function rulesFromForm(values, referenceRules) {
  return Object.fromEntries(RULE_FIELDS.map(([key, , min, , scale]) => {
    if (typeof min === 'object') {
      const value = String(values[key])
      return [key, typeof referenceRules[key] === 'boolean' ? value === 'true' : value]
    }
    const scaled = Number(values[key]) * scale
    return [key, scale === 60_000 ? Math.round(scaled) : scaled]
  }))
}

export function groupedRuleFields() {
  const groups = new Map()
  for (const [key, label, min, max, scale, numericGroup] of RULE_FIELDS) {
    const group = typeof min === 'object' ? max : numericGroup
    const field = {
      key,
      label,
      min: typeof min === 'object' ? undefined : min,
      max: typeof min === 'object' ? undefined : max,
      step: typeof min === 'object' ? undefined : scale === 60_000 ? 0.1 : 1,
      options: typeof min === 'object' ? Object.entries(min) : null,
    }
    if (!groups.has(group)) groups.set(group, [])
    groups.get(group).push(field)
  }
  return [...groups].map(([name, fields]) => ({ name, fields }))
}
