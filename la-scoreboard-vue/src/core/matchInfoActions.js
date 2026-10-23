import { validateMatchDate } from './matchModel.js'
import { recordMatchEvent } from './matchEvents.js'

const INFO_FIELDS = ['title', 'date', 'home', 'away']
const INFO_LABELS = { title: '名称', date: '日期', home: '主队', away: '客队' }

/** 修改比赛基础信息，不改变本场规则、比分和计时状态。 */
export function applyMatchInfoAction(match, type, payload) {
  if (type !== 'match-info') return false

  const changes = Object.fromEntries(INFO_FIELDS.map(field => [field, String(payload[field] || '').trim()]))
  requireInfo(changes.title && changes.title.length <= 80
    && ['home', 'away'].every(field => changes[field] && changes[field].length <= 40),
  '请填写比赛名称（80 字以内）和两队名称（各 40 字以内）。')
  validateMatchDate(changes.date)
  requireInfo(match.config.preset !== 'sll-u12'
    || (match.config.date >= '2027-01-01') === (changes.date >= '2027-01-01'),
  'U12 日期跨越 2027 年会涉及男女分组变化。请通过新建比赛核对组别；本入口只修改信息，不改变本场规则。')

  const before = Object.fromEntries(INFO_FIELDS.map(field => [field, match.config[field]]))
  const changedFields = INFO_FIELDS.filter(field => changes[field] !== before[field])
  requireInfo(changedFields.length > 0, '比赛信息没有变化。')

  Object.assign(match.config, changes)
  const details = changedFields.map(field => `${INFO_LABELS[field]}“${before[field]}”→“${changes[field]}”`).join('；')
  recordMatchEvent(match, `比赛信息修改：${details}。比分、规则和计时状态保持不变。`, '比赛信息', {
    category: 'match-info',
    before,
    changes,
  })
  return true
}

function requireInfo(ok, message) {
  if (!ok) throw new Error(message)
}
