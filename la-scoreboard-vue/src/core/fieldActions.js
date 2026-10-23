import { recordMatchEvent } from './matchEvents.js'
import { MATCH_EVENT_TYPE } from './eventTypes.js'
import { isYouth } from './rules.js'
import { isPeriodActive, shotOff, stopTime } from './timing.js'

const FIELD_ACTIONS = new Set([
  'resume',
  'pause',
  'whistle',
  'possession',
  'reset-shot',
  'wide',
  'backcourt',
  'frontcourt',
  'alternate-set',
  'alternate-award',
])

export function applyFieldAction(match, type, payload = {}) {
  if (!FIELD_ACTIONS.has(type)) return false

  switch (type) {
    case 'resume':
      resumeMatch(match)
      break
    case 'pause':
      pauseMatch(match)
      break
    case 'whistle':
      whistleDeadBall(match)
      break
    case 'possession':
      setPossession(match, payload)
      break
    case 'reset-shot':
      resetShotClock(match)
      break
    case 'wide':
      recordWideShot(match)
      break
    case 'backcourt':
      enterBackcourt(match)
      break
    case 'frontcourt':
      enterFrontcourt(match, payload)
      break
    case 'alternate-set':
      setAlternatePossession(match, payload)
      break
    case 'alternate-award':
      awardAlternatePossession(match)
      break
  }
  return true
}

function resumeMatch(match) {
  requireField(['ready', 'period'].includes(match.phase) && match.main > 0, '请先结束暂停或准备下一节。')
  requireField(!(match.running && match.ball === 'live'), '比赛已经在进行。')
  requireField(!match.possession
    || match.shot > 0
    || match.resetOnResume
    || shotOff(match), '进攻钟已到期，请先由裁判确认换球权、重置或更正时间。')
  const firstWhistleOfPeriod = !match.periodStarted
  requireField(!firstWhistleOfPeriod || match.possession, '请先确认本节开场球权。')
  const eventType = firstWhistleOfPeriod
    ? (match.period === 1 && !match.events.some(event => event.eventType === MATCH_EVENT_TYPE.MATCH_START)
        ? MATCH_EVENT_TYPE.MATCH_START
        : MATCH_EVENT_TYPE.PERIOD_START)
    : MATCH_EVENT_TYPE.SYSTEM
  match.phase = 'period'
  match.periodStarted = true
  match.running = true
  match.ball = 'live'
  match.alert = ''
  if (match.resetOnResume) {
    resetShot(match)
    match.resetOnResume = false
  }
  for (const penalty of match.penalties) {
    if (penalty.seated && penalty.remaining > 0) penalty.started = true
  }
  recordMatchEvent(
    match,
    `鸣哨开赛／复赛。比赛钟恢复；已就座罚时同步计时。${match.rules.shot ? '获得球权后启动进攻钟。' : '本场不使用进攻钟。'}`,
    '5.2.1 / 5.3 / 9.4',
    {
      eventType,
      // 开场球队用于保存本节开场球权；节内复赛属于全场节点，不关联球队。
      team: firstWhistleOfPeriod ? match.possession || undefined : undefined,
    },
  )
}

function pauseMatch(match) {
  requireField(match.phase === 'period' && match.periodStarted, '请先鸣哨开赛。')
  requireField(match.running, '比赛钟已经暂停，请鸣哨复赛。')
  deadBall(match, true)
  recordMatchEvent(
    match,
    isYouth(match)
      ? '裁判暂停：比赛钟与罚时全部停止。'
      : '裁判暂停：比赛钟、进攻钟、罚时全部停止；进攻时间保留。',
    '5.2.2 / 5.3.1',
  )
}

function whistleDeadBall(match) {
  requireField(isPeriodActive(match), '请先开始本节；节间或暂停中不能执行这个操作。')
  requireField(match.ball === 'live', '当前已经是死球，请先复赛。')
  deadBall(match)
  recordMatchEvent(
    match,
    stopTime(match)
      ? '死球停表：比赛钟与罚时暂停。'
      : '普通死球：比赛钟和已开始的罚时继续，进攻钟暂停。',
    '5.2.2 / 5.3.2',
  )
}

function setPossession(match, payload) {
  requireTeam(payload.team)
  if (match.phase === 'ready') {
    awardPossession(match, payload.team)
    resetShot(match)
    match.notice = `已确认${match.config[payload.team]}获得本节开场球权，等待鸣哨。`
    match.rule = ''
    return
  }
  requireActivePeriod(match)
  if (match.possession !== payload.team) {
    awardPossession(match, payload.team)
    resetShot(match)
  }
  if (payload.recordEvent === false) return
  recordMatchEvent(
    match,
    `${match.config[payload.team]}获得球权。${match.rules.shot ? `换队后进攻钟重置 ${match.rules.shot / 1000} 秒。` : '本场无进攻钟。'}${match.rules.passRequired ? '首次射门须完成至少一次成功接传。' : ''}`,
    isYouth(match) ? '6.3 / 6.6' : '5.3.1',
    { eventType: MATCH_EVENT_TYPE.POSSESSION, team: payload.team },
  )
}

function resetShotClock(match) {
  requireActivePeriod(match)
  requireField(match.rules.shot > 0, '本场不使用进攻钟。')
  requireField(match.possession, '请先记录球权。')
  resetShot(match)
  recordMatchEvent(
    match,
    `裁判确认重置：进攻钟恢复 ${match.rules.shot / 1000} 秒，球权未改变。`,
    '5.3.1',
  )
}

function recordWideShot(match) {
  requireActivePeriod(match)
  requireField(match.rules.shot > 0, '本组不使用进攻钟，无需操作射门倒计时。')
  recordMatchEvent(
    match,
    '射偏、碰圈外守门员或防守球员：不自动重置进攻钟。若球出界，请再记录出界。',
    '5.3.1',
  )
}

function enterBackcourt(match) {
  requireActivePeriod(match)
  requireField(match.rules.clearMs > 0, '本组不使用六人制 10 秒推进提示。')
  requireField(match.possession && match.ball === 'live', '请先复赛并记录获得球权的球队。')
  match.clear = match.rules.clearMs
  match.frontcourt = false
  recordMatchEvent(
    match,
    `后场控球，启动 ${match.rules.clearMs / 1000} 秒推进提示。进攻钟保留剩余；违例由裁判判定。`,
    '6.9',
  )
}

function enterFrontcourt(match, payload) {
  requireActivePeriod(match)
  requireField(match.possession, '请先记录球权。')
  match.clear = null
  match.frontcourt = true
  if (payload.recordEvent === false) return
  recordMatchEvent(
    match,
    '已进入前场，停止推进提示；符合条件时可申请本队暂停。',
    isYouth(match) ? '6.8.4' : '6.9',
  )
}

function setAlternatePossession(match, payload) {
  requireField(['ready', 'period', 'break'].includes(match.phase) && ['home', 'away'].includes(payload.team), '当前不能设置交替球权。')
  match.alternate = payload.team
  recordMatchEvent(
    match,
    `裁判确认下一次交替球权为${match.config[payload.team]}。`,
    isYouth(match) ? '6.11' : '6.12',
  )
}

function awardAlternatePossession(match) {
  requireField(isPeriodActive(match) && match.alternate, '请先开赛并确认首次交替球权。')
  deadBall(match, true)
  awardPossession(match, match.alternate)
  match.alternate = otherTeam(match.alternate)
  resetShot(match)
  match.resetOnResume = false
  recordMatchEvent(
    match,
    `裁判判交替球权给${match.config[match.possession]}，下次为${match.config[match.alternate]}。当前停表等待复赛。`,
    isYouth(match) ? '6.11' : '6.12',
    { eventType: MATCH_EVENT_TYPE.POSSESSION, team: match.possession },
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

function requireActivePeriod(match) {
  requireField(isPeriodActive(match), '请先开始本节；节间或暂停中不能执行这个操作。')
}

function requireTeam(team) {
  requireField(['home', 'away'].includes(team), '请选择球权归属。')
}

function requireField(ok, message) {
  if (!ok) throw new Error(message)
}
