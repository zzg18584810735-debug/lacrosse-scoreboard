const commonRules = {
  breakMs: 120000,
  halftimeMs: 300000,
  overtimeBreak: 120000,
  timeoutsPerHalf: 1,
  overtimeTimeouts: 0,
  timeout: 60000,
  allDeadStop: false,
  shotLastOff: true,
  minor: 30000,
  major: 60000,
  expulsion: 120000,
  majorLimit: 3,
  points: 1,
}

export const RULES = Object.freeze({
  ...commonRules,
  id: 'sixes-cn-2026-v1',
  name: '六人制',
  family: 'sixes',
  players: 6,
  periods: 4,
  halftimeAfter: 2,
  period: 480000,
  overtime: 180000,
  shot: 30000,
  clearMs: 10000,
  stopWindow: 120000,
  overtimeStopAll: true,
  goldenGoal: false,
  overtimePolicy: 'always',
  foulStop: false,
  passRequired: false,
  alternateFirst: 'manual',
  alternateReset: false,
  goalieProxy: true,
})

function youthRules(group, minutes) {
  return Object.freeze({
    ...commonRules,
    id: `china-${group}-2026`,
    name: group.toUpperCase(),
    family: 'youth',
    players: 4,
    periods: 2,
    halftimeAfter: 1,
    period: minutes * 60000,
    overtime: 240000,
    shot: 0,
    clearMs: 0,
    stopWindow: 60000,
    overtimeStopAll: false,
    goldenGoal: true,
    overtimePolicy: 'knockout',
    foulStop: true,
    passRequired: true,
    alternateFirst: 'away',
    alternateReset: true,
    goalieProxy: false,
  })
}

export const PRESETS = Object.freeze({
  'sll-u8': youthRules('u8', 8),
  'sll-u10': youthRules('u10', 10),
  'sll-u12': youthRules('u12', 10),
  'sll-sixes': RULES,
})

// 表单展示、单位换算和规则校验共用同一份字段定义。
export const RULE_FIELDS = [
  ['players','每队上场人数',1,20,1,'基本'], ['periods','常规节数',1,12,1,'基本'], ['period','每节时长（分）',0.1,60,60000,'基本'], ['halftimeAfter','第几节后为中场（0=无）',0,11,1,'基本'],
  ['shot','进攻钟（秒，0=关闭）',0,180,1000,'基本'], ['clearMs','推进提示（秒，0=关闭）',0,120,1000,'基本'], ['points','每个有效进球得分',1,10,1,'基本'],
  ['breakMs','普通节间休息（分）',0,30,60000,'休息与暂停'], ['halftimeMs','中场休息（分）',0,60,60000,'休息与暂停'], ['timeout','球队暂停时长（秒）',1,600,1000,'休息与暂停'], ['timeoutsPerHalf','每队每半场暂停次数',0,10,1,'休息与暂停'], ['overtimeTimeouts','每队每节加时暂停次数',0,10,1,'休息与暂停'],
  ['stopWindow','末节／加时死球停表窗口（秒）',0,3600,1000,'停表'], ['allDeadStop','普通阶段死球也停表',{false:'跑表',true:'停表'},'停表'], ['foulStop','无罚时犯规鸣哨',{false:'按死球窗口处理',true:'始终停表'},'停表'], ['overtimeStopAll','整个加时死球停表',{false:'仅按最后停表窗口',true:'整个加时停表'},'停表'], ['shotLastOff','比赛钟不足一个进攻周期',{true:'关闭进攻钟',false:'继续使用进攻钟'},'停表'],
  ['overtimePolicy','平局处理',{always:'所有比赛均加时',knockout:'仅淘汰／名次赛加时',none:'不加时，允许平局'},'加时'], ['overtime','每节加时（分）',0.1,30,60000,'加时'], ['goldenGoal','加时获胜方式',{false:'打满本节，领先获胜',true:'金球，先入有效球即胜'},'加时'], ['overtimeBreak','加时前／加时之间休息（分）',0,30,60000,'加时'],
  ['minor','绿牌罚时（秒）',1,600,1000,'判罚与球权'], ['major','黄牌罚时（秒）',1,600,1000,'判罚与球权'], ['expulsion','红牌代罚（秒）',1,600,1000,'判罚与球权'], ['majorLimit','累计几次重大犯规罚退',1,10,1,'判罚与球权'],
  ['goalieProxy','守门员罚时',{true:'必须由其他球员代罚',false:'按普通球员处理'},'判罚与球权'], ['passRequired','首次射门前成功接传',{true:'至少一次，由裁判确认',false:'无此附加要求'},'判罚与球权'], ['alternateFirst','首次交替球权',{manual:'由裁判确认',away:'客队',home:'主队'},'判罚与球权'], ['alternateReset','首次进入加时的交替球权',{true:'重置为首次球队',false:'延续之前顺序'},'判罚与球权'],
]

export function resolveRules(preset = 'sll-sixes', overrides = {}) {
  const base = preset === 'custom'
    ? { ...youthRules('u8', 8), id: 'custom', name: '自定义', family: 'custom' }
    : PRESETS[preset]
  requireRule(base, '请选择有效的规则模板。')
  const rules = { ...base }
  for (const [key, label, min, max, scale] of RULE_FIELDS) {
    const value = Object.hasOwn(overrides, key) ? overrides[key] : base[key]
    if (typeof min === 'object') {
      requireRule(Object.hasOwn(min, String(value)) && typeof value === typeof base[key], `${label}无效。`)
    } else {
      requireRule(Number.isFinite(value) && Number.isInteger(value) && value >= min * scale && value <= max * scale, `${label}超出可用范围。`)
    }
    rules[key] = value
  }
  requireRule(rules.halftimeAfter < rules.periods, '中场位置必须小于常规节数；不设中场请填 0。')
  return rules
}

export const isYouth = match => match.rules.family === 'youth'

export const isCustom = match => match.config.preset === 'custom'
  || RULE_FIELDS.some(([key]) => match.rules[key] !== PRESETS[match.config.preset]?.[key])

export function groupLabel(config, rules) {
  return `${rules.name}${config.gender === 'boys' ? ' 男子组' : config.gender === 'girls' ? ' 女子组' : isYouth({ rules }) ? ' 混合组' : '组'}`
}

export function formatRuleReference(match, rule) {
  let formatted = rule
  if (isYouth(match)) {
    formatted = formatted.replace(/6\.10/g, '6.8').replace(/9\.4/g, '13.4').replace(/7\.1/g, '9.2.2').replace(/8\.1–8\.4/g, '9.2.4–9.2.7').replace(/5\.2\.1/g, '5.3.1').replace(/5\.2\.2/g, '5.3.2')
    formatted = formatted.replace(/5\.3\.1 \/ 6\.1 \/ 6\.8\.5/g, '5.3.1 / 6.4 / 6.8.5').replace(/5\.2\.2 \/ 5\.3\.1/g, '5.3.2').replace(/5\.3\.1 \/ 5\.3 \/ 13\.4/g, '5.3.1 / 13.4').replace(/4\.3 \/ 5\.3\.3/g, '4.3')
  }
  return isCustom(match) ? '本场自定义规则' : formatted
}

function requireRule(ok, message) {
  if (!ok) throw new Error(message)
}
