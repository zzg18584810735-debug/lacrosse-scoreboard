import { RULE_FIELDS } from '../../core/rules.js'

export function ruleSummaryItems(rules) {
  return RULE_FIELDS.map(([key, label, min, , scale]) => ({
    key,
    label,
    value: typeof min === 'object' ? min[String(rules[key])] : rules[key] / scale,
  }))
}
