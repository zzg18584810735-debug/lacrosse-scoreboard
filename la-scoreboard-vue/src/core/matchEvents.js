import { matchRecords } from './matchRecords.js'
import { resolveEventTitle } from './eventTypes.js'
import { formatRuleReference } from './rules.js'
import { clock, periodName } from './timing.js'

export function recordMatchEvent(match, text, rule = '', details = {}) {
  const formattedRule = formatRuleReference(match, rule)
  match.notice = text
  match.rule = formattedRule
  const event = {
    id: ++match.sequence,
    period: periodName(match),
    periodNumber: match.period,
    time: clock(match.main),
    remaining: match.main,
    text,
    rule: formattedRule,
    category: 'system',
    ...details,
  }
  event.title = resolveEventTitle(event, match)
  match.events.push(event)
}

export function recordGoal(match, payload, occurred, extra = {}) {
  const players = goalPlayers(payload)
  recordMatchEvent(
    match,
    `${match.config[payload.team]} · ${players.player} 进球${extra.scoreAdjustment === 0 ? '（比分已计入）' : ` +${match.rules.points}`}。${players.assist ? `助攻：${players.assist}。` : ''}${extra.backfilled ? `补录依据：${extra.reason}。` : '球权默认交给失球方；延缓严重／驱逐犯规时按裁判报告更正球权。'}`,
    '6.6 / 9.4',
    {
      category: 'goal',
      team: payload.team,
      ...players,
      points: match.rules.points,
      period: periodName({ ...match, period: occurred.period }),
      time: clock(occurred.main),
      periodNumber: occurred.period,
      remaining: occurred.main,
      ...extra,
    },
  )
}

export function penaltyData(match, payload, checkServing = true) {
  const data = {
    team: payload.team,
    player: String(payload.player || '').trim(),
    subject: payload.subject || 'player',
    kind: payload.kind,
    server: String(payload.server || '').trim() || String(payload.player || '').trim(),
    reason: String(payload.reason || '').trim(),
  }
  requireEvent(['home', 'away'].includes(data.team)
    && data.player
    && data.player.length <= 12
    && data.server.length <= 12, '请填写球队及 12 字以内的号码／姓名。')
  requireEvent(['minor-turnover', 'minor', 'major', 'expulsion'].includes(data.kind)
    && ['player', 'goalie', 'official'].includes(data.subject), '请选择有效的犯规主体与判罚类型。')
  requireEvent(data.reason.length <= 120, '犯规备注请填写 120 字以内。')

  if (data.kind !== 'minor-turnover') {
    const requiresProxy = data.kind === 'expulsion'
      || (data.subject === 'goalie' && match.rules.goalieProxy)
      || data.subject === 'official'
    requireEvent(!(requiresProxy
      && (!String(payload.server || '').trim() || data.server === data.player)), '红牌、官员或需要代罚的守门员判罚，请填写另一名代罚球员。')
    if (checkServing) {
      requireEvent(!match.disqualified.includes(`${data.team}:${data.server}`), '该球员已被罚退，不能承担罚时。')
      requireEvent(!match.penalties.some(penalty => penalty.team === data.team
        && penalty.server === data.server
        && !penalty.released), '该球员仍有未释放的罚时；请由裁判核定合并时长后更正已有记录。')
    }
  }
  return data
}

export function rebuildDiscipline(match) {
  match.majors = {}
  match.disqualified = []
  for (const penalty of match.penalties) {
    if (penalty.subject === 'official') continue
    const key = `${penalty.team}:${penalty.player}`
    if (penalty.kind === 'major') match.majors[key] = (match.majors[key] || 0) + 1
    if ((penalty.kind === 'expulsion' || match.majors[key] >= match.rules.majorLimit)
      && !match.disqualified.includes(key)) {
      match.disqualified.push(key)
    }
  }
}

export function timeoutBucket(match, period = match.period) {
  if (period > match.rules.periods) return `overtime-${period}`
  return match.rules.halftimeAfter > 0 && period > match.rules.halftimeAfter ? 'second' : 'first'
}

export function recordTimeoutBucket(match, event) {
  if (event.timeoutBucket) return event.timeoutBucket
  // 旧版 v4 暂停记录只有本地化节次名称，需要据此恢复暂停额度归属。
  const period = event.periodNumber
    || (event.period === '上半场'
      ? 1
      : event.period === '下半场'
        ? 2
        : /^第 \d+ 节$/.test(event.period)
          ? Number(event.period.match(/\d+/)[0])
          : /^加时 \d+$/.test(event.period)
            ? match.rules.periods + Number(event.period.match(/\d+/)[0])
            : null)
  requireEvent(period, '旧暂停记录缺少节次，无法确定所属额度。')
  return timeoutBucket(match, period)
}

export function correctionRecord(match, payload, category) {
  requireEvent(String(payload.correctionReason || '').trim()
    && String(payload.correctionReason).length <= 200, '请填写 200 字以内的更正依据。')
  const event = matchRecords(match, category)
    .find(item => item.id === Number(payload.id) && !item.voided)
  requireEvent(event, '该记录不存在或已作废。')
  requireEvent(['edit', 'cancel'].includes(payload.mode), '请选择更正或作废。')
  return event
}

export function goalPlayers(payload) {
  const player = String(payload.player || '').trim()
  const assist = String(payload.assist || '').trim()
  requireEvent(player, '请填写进球球员号码或姓名。')
  requireEvent(player.length <= 20 && assist.length <= 20, '球员及助攻信息请各填写 20 字以内。')
  return { player, assist }
}

function requireEvent(ok, message) {
  if (!ok) throw new Error(message)
}
