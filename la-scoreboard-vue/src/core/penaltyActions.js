import {
  correctionRecord,
  penaltyData,
  rebuildDiscipline,
  recordMatchEvent,
} from './matchEvents.js'
import { MATCH_EVENT_TYPE } from './eventTypes.js'
import { penaltyLabel } from './matchRecords.js'
import { clock, freeze, isPeriodActive, stopTime } from './timing.js'

const PENALTY_ACTIONS = new Set(['penalty', 'correct-penalty', 'seat', 'release'])

export function applyPenaltyAction(match, type, payload = {}) {
  if (!PENALTY_ACTIONS.has(type)) return false

  switch (type) {
    case 'penalty':
      addPenalty(match, payload)
      break
    case 'correct-penalty':
      correctPenalty(match, payload)
      break
    case 'seat':
      seatPenalty(match, payload)
      break
    case 'release':
      releasePenalty(match, payload)
      break
  }
  return true
}

function addPenalty(match, payload) {
  requirePenalty(isPeriodActive(match) && match.periodStarted, '请先鸣哨开赛，再记录判罚。')
  const data = penaltyData(match, payload)
  const { player, server } = data
  const key = `${data.team}:${player}`

  if (data.kind === 'minor-turnover') {
    deadBall(match, match.rules.foulStop)
    const clockStopped = !match.running
    awardPossession(match, otherTeam(data.team))
    resetShot(match)
    recordMatchEvent(
      match,
      `${match.config[data.team]} ${player} 号轻微犯规：裁判判换球权，无罚时。${clockStopped ? `计时停止于 ${clock(match.main)}。` : ''}`,
      '7.1',
      { category: 'penalty', ...data, clockStopped },
    )
    return
  }

  deadBall(match, true)
  match.resetOnResume = Boolean(match.rules.shot)
  const seconds = match.rules[data.kind] / 1000
  if (data.kind === 'major' && data.subject !== 'official') {
    match.majors[key] = (match.majors[key] || 0) + 1
  }
  const expelled = (data.kind === 'expulsion' || match.majors[key] >= match.rules.majorLimit)
    && data.subject !== 'official'
  if (expelled && !match.disqualified.includes(key)) match.disqualified.push(key)

  match.penalties.push({
    id: match.sequence + 1,
    ...data,
    remaining: seconds * 1000,
    seated: false,
    started: false,
    released: false,
  })
  awardPossession(
    match,
    payload.awarded === 'none'
      ? null
      : (['home', 'away'].includes(payload.awarded) ? payload.awarded : otherTeam(data.team)),
  )
  recordMatchEvent(
    match,
    `${match.config[data.team]} ${data.subject === 'official' ? `官员 ${player}` : `${player} 号`}${data.kind === 'minor' ? '绿牌' : data.kind === 'major' ? '黄牌' : '红牌'}，${server} 号承担 ${seconds} 秒。${data.subject === 'official' ? data.kind === 'expulsion' ? '官员被驱逐；代罚不累计球员黄牌。' : '官员犯规的代罚不累计球员黄牌。' : expelled ? '犯规球员本场不得再上场。' : ''}先确认就座，再鸣哨复赛。球权按裁判指定。计时停止于 ${clock(match.main)}。`,
    '7.1 / 8.1–8.4 / 9.4',
    { category: 'penalty', ...data, clockStopped: true },
  )
}

function correctPenalty(match, payload) {
  const event = correctionRecord(match, payload, 'penalty')
  const previous = match.penalties.find(item => item.id === event.id)
  match.penalties = match.penalties.filter(item => item.id !== event.id)
  rebuildDiscipline(match)

  let changes = {
    team: event.team,
    player: event.player,
    kind: event.kind,
    subject: event.subject,
    server: event.server,
    text: event.text,
    cancelled: true,
    reason: event.reason || previous?.reason || '',
  }
  if (payload.mode === 'edit') {
    const sameAssignment = previous
      && ['team', 'player', 'subject', 'kind'].every(key => previous[key] === payload[key])
      && previous.server === String(payload.server || payload.player || '').trim()
      && !previous.released
    const data = penaltyData(match, payload, payload.remaining !== 0 && !sameAssignment)
    if (data.kind !== 'minor-turnover') {
      requirePenalty(Number.isFinite(payload.remaining)
        && payload.remaining >= 0
        && payload.remaining <= 600000, '请按裁判确认填写 0–600 秒的剩余罚时。')
      const sameServer = previous
        && previous.team === data.team
        && previous.server === data.server
        && !previous.released
      match.penalties.push({
        ...data,
        id: event.id,
        remaining: payload.remaining,
        seated: Boolean(sameServer && previous.seated),
        started: Boolean(sameServer && previous.started),
        released: payload.remaining === 0 && Boolean(previous?.released),
      })
    }
    changes = {
      ...data,
      rule: '裁判更正',
      cancelled: false,
      text: `${match.config[data.team]} ${data.player} · ${penaltyLabel(data.kind)}。${data.kind === 'minor-turnover' ? '无罚时。' : `${data.server} 号承担罚时；更正时剩余 ${clock(payload.remaining)}。`}`,
    }
  }

  rebuildDiscipline(match)
  freeze(match)
  recordMatchEvent(
    match,
    `${payload.mode === 'cancel' ? '作废' : '更正'}判罚 #${event.id}：${payload.correctionReason}。${payload.mode === 'cancel' ? '已移除该条罚时。' : changes.text}${changes.reason ? `犯规备注：${changes.reason}。` : ''}已重算牌数及罚退名单。现场比分、时钟数值与球权不回溯，保持停表；请核对场上人员与复赛球权。`,
    '裁判更正',
    {
      category: 'penalty-correction',
      targetId: event.id,
      before: { ...event, ...previous },
      changes,
      correctionReason: String(payload.correctionReason).trim(),
    },
  )
}

function seatPenalty(match, payload) {
  const penalty = match.penalties.find(item => item.id === Number(payload.id))
  requirePenalty(penalty && !penalty.released && penalty.remaining > 0, '没有可就座的罚时。')
  penalty.seated = true
  // 复赛后罚时跟随比赛钟；普通死球不停比赛钟时，罚时也继续运行。
  if (isPeriodActive(match) && match.running) penalty.started = true
  recordMatchEvent(
    match,
    `${match.config[penalty.team]} ${penalty.server} 号已就座。${penalty.started ? '复赛与就座条件均满足，开始罚时。' : '等待复赛哨。'}`,
    '9.4.1',
    { eventType: MATCH_EVENT_TYPE.SYSTEM, team: penalty.team, player: penalty.server },
  )
}

function releasePenalty(match, payload) {
  const penalty = match.penalties.find(item => item.id === Number(payload.id))
  requirePenalty(penalty && penalty.remaining === 0 && !penalty.released, '罚时尚未完成。')
  penalty.released = true
  recordMatchEvent(
    match,
    `${match.config[penalty.team]} ${penalty.server} 号罚时结束，已确认释放。被驱逐者仍不得返场。`,
    '9.4',
    { eventType: MATCH_EVENT_TYPE.SYSTEM, team: penalty.team, player: penalty.server },
  )
}

function resetShot(match) {
  match.shot = match.rules.shot
  match.alert = ''
}

function awardPossession(match, team) {
  match.possession = team
  match.frontcourt = false
  match.clear = null
}

function deadBall(match, force = false) {
  match.ball = 'dead'
  match.clear = null
  if (force || stopTime(match)) match.running = false
}

function otherTeam(team) {
  return team === 'home' ? 'away' : 'home'
}

function requirePenalty(ok, message) {
  if (!ok) throw new Error(message)
}
