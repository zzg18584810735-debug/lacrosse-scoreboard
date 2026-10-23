import { resolveRules } from './rules.js'

export function createMatch(config = {}) {
  const preset = config.preset || 'sll-sixes'
  const rules = resolveRules(preset, config.rules)
  const date = config.date || localDate()
  const stage = config.stage || 'league'
  validateMatchDate(date)
  requireModel(['league', 'knockout'].includes(stage), '请选择常规赛或淘汰／名次赛。')
  const separated = preset === 'sll-u12' && date >= '2027-01-01'
  requireModel(!separated || ['boys', 'girls'].includes(config.gender), '2027 年起 U12 男女分组，请选择男子组或女子组。')
  const selected = {
    title: 'SLL 比赛',
    home: '红队',
    away: '蓝队',
    ...config,
    preset,
    date,
    stage,
    gender: separated ? config.gender : 'mixed',
  }
  delete selected.rules
  return {
    version: 4,
    rules: structuredClone(rules),
    config: selected,
    period: 1,
    phase: 'ready',
    periodStarted: false,
    main: rules.period,
    running: false,
    ball: 'dead',
    // 按新建比赛的本场规则配置初始球权；manual 仍等待裁判在页面选择。
    possession: rules.alternateFirst === 'manual' ? null : rules.alternateFirst,
    shot: rules.shot,
    resetOnResume: false,
    clear: null,
    frontcourt: false,
    auxiliary: 0,
    auxiliaryRunning: false,
    timeoutContext: null,
    goldenGoal: false,
    goldenConflict: false,
    alternate: rules.alternateFirst === 'manual' ? null : rules.alternateFirst,
    scores: { home: 0, away: 0 },
    timeouts: { home: rules.timeoutsPerHalf, away: rules.timeoutsPerHalf },
    penalties: [],
    majors: {},
    disqualified: [],
    events: [],
    sequence: 0,
    undo: null,
    pendingGoal: null,
    notice: '本场规则已就绪。确认双方队名与规则后，鸣哨开赛并记录球权。',
    rule: '本场规则',
    alert: '',
  }
}

/** 页面尚无服务器比赛时使用的只读空状态。 */
export function createEmptyMatch() {
  const match = createMatch({
    title: '暂无比赛',
    home: '主队',
    away: '客队',
    preset: 'sll-sixes',
  })
  // 占位页面没有本场规则，只保留中央比赛钟，避免展示虚假的进攻钟和推进钟。
  match.rules.shot = 0
  match.rules.clearMs = 0
  match.main = 0
  match.shot = 0
  match.clear = 0
  match.auxiliary = 0
  match.scores = { home: 0, away: 0 }
  match.possession = null
  match.timeouts = { home: 0, away: 0 }
  match.alternate = null
  match.notice = '暂无比赛，请先新建比赛。'
  match.rule = '暂无比赛'
  return match
}

function localDate() {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

export function validateMatchDate(date) {
  requireModel(/^\d{4}-\d{2}-\d{2}$/.test(date)
    && Number.isFinite(Date.parse(date))
    && new Date(date).toISOString().slice(0, 10) === date, '请选择有效比赛日期。')
}

function requireModel(ok, message) {
  if (!ok) throw new Error(message)
}
