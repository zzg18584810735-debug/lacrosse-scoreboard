import { goalPlayers, recordGoal, recordMatchEvent } from './matchEvents.js'
import { matchRecords } from './matchRecords.js'
import { confirmGoldenGoal, holdGoldenConflict, overtimeGoals } from './matchOutcome.js'
import { freeze, isOvertime, isPeriodActive, lastStartedPeriod, stopTime } from './timing.js'

const GOAL_ACTIONS = new Set([
  'begin-goal',
  'cancel-goal',
  'confirm-goal',
  'goal',
  'backfill-goal',
  'goal-details',
])

export function applyGoalAction(match, type, payload = {}) {
  if (!GOAL_ACTIONS.has(type)) return false

  switch (type) {
    case 'begin-goal':
      beginGoal(match, payload)
      break
    case 'cancel-goal':
      cancelGoal(match)
      break
    case 'confirm-goal':
      confirmGoal(match, payload)
      break
    case 'goal':
      addGoal(match, payload)
      break
    case 'backfill-goal':
      backfillGoal(match, payload)
      break
    case 'goal-details':
      addGoalDetails(match, payload)
      break
  }
  return true
}

function beginGoal(match, payload) {
  requireGoal(isPeriodActive(match) && match.ball === 'live' && match.running, '请在比赛进行中记录进球；漏记请用补录进球。')
  requireTeam(payload.team)
  match.pendingGoal = {
    id: match.sequence + 1,
    team: payload.team,
    period: match.period,
    main: match.main,
    player: '',
    assist: '',
    returnState: {
      running: match.running,
      ball: match.ball,
      clear: match.clear,
      frontcourt: match.frontcourt,
    },
  }
  deadBall(match, match.rules.goldenGoal && isOvertime(match))
  // 进球确认属于明确停表节点，统一复用计时模块的冻结能力。
  freeze(match)
  // 打开录入弹窗只是待确认状态，不属于正式比赛事件。
  match.notice = '正在录入进球，尚未计分；比赛计时已暂停。'
  match.rule = ''
}

function cancelGoal(match) {
  requireGoal(match.pendingGoal, '没有待确认的进球。')
  const returnState = match.pendingGoal.returnState
  match.pendingGoal = null
  if (returnState) {
    match.running = returnState.running
    match.ball = returnState.ball
    match.clear = returnState.clear
    match.frontcourt = returnState.frontcourt
  } else if (match.phase === 'period') {
    // 服务器只保存待确认进球字段，不保存弹窗打开前的临时快照；刷新后取消时
    // 仍可依据 begin-goal 的前置条件恢复为活球计时。
    match.running = true
    match.ball = 'live'
  }
  // 取消未确认录入只恢复现场状态，不污染正式比赛记录。
  match.notice = '已取消进球录入，未加分；已恢复打开录入弹窗前的场上状态。'
  match.rule = ''
}

function confirmGoal(match, payload) {
  const draft = match.pendingGoal
  requireGoal(draft && draft.id === Number(payload.id), '该进球已处理，请勿重复确认。')
  requireTeam(payload.team)
  goalPlayers(payload)
  match.scores[payload.team] += match.rules.points
  awardPossession(match, otherTeam(payload.team))
  match.resetOnResume = Boolean(match.rules.shot)
  match.pendingGoal = null
  if (match.rules.goldenGoal && isOvertime(match)) confirmGoldenGoal(match)
  recordGoal(match, payload, draft)
}

function addGoal(match, payload) {
  requireGoal(isPeriodActive(match) && match.ball === 'live' && match.running, '请在比赛进行中确认进球；漏记请用补录进球。')
  requireTeam(payload.team)
  goalPlayers(payload)
  match.scores[payload.team] += match.rules.points
  deadBall(match)
  freeze(match)
  awardPossession(match, otherTeam(payload.team))
  match.resetOnResume = Boolean(match.rules.shot)
  if (match.rules.goldenGoal && isOvertime(match)) confirmGoldenGoal(match)
  recordGoal(match, payload, match)
}

function backfillGoal(match, payload) {
  requireGoal(lastStartedPeriod(match) >= 1, '比赛尚未开赛，暂无可补录的节次。')
  requireTeam(payload.team)
  goalPlayers(payload)
  requireGoal(['add', 'included'].includes(payload.scoreMode), '请选择比分是否已经计入。')
  requireGoal(Number.isInteger(payload.period)
    && payload.period >= 1
    && payload.period <= lastStartedPeriod(match), '只能补录已经开赛的节次；当前待开赛节次不能补录。')
  const duration = payload.period > match.rules.periods ? match.rules.overtime : match.rules.period
  requireGoal(Number.isInteger(payload.main)
    && payload.main >= 0
    && payload.main <= duration, '进球时间超出该节范围。')
  requireGoal(payload.period !== match.period || payload.main >= match.main, '进球时间不能晚于当前比赛时间。')
  const reason = String(payload.reason || '').trim()
  requireGoal(reason && reason.length <= 200, '请填写 200 字以内的补录依据。')

  if (payload.scoreMode === 'included') {
    const recorded = matchRecords(match, 'goal')
      .filter(event => event.team === payload.team && !event.voided)
      .reduce((sum, event) => sum + (event.points ?? match.rules.points), 0)
    requireGoal(match.scores[payload.team] - recorded >= match.rules.points, '当前比分没有尚未对应进球记录的分数，请检查是否已记过这球。')
  } else {
    match.scores[payload.team] += match.rules.points
    if (match.phase === 'finished') {
      match.phase = 'period-end'
      match.alert = '已补加进球，请重新核对比分和比赛结束状态。'
    }
  }

  freeze(match)
  recordGoal(match, payload, payload, {
    backfilled: true,
    reason,
    scoreAdjustment: payload.scoreMode === 'add' ? match.rules.points : 0,
  })
  reconcileGoldenGoal(match, payload)
}

function addGoalDetails(match, payload) {
  const goal = match.events.find(event => event.id === Number(payload.id)
    && event.category === 'goal'
    && !event.voided)
  requireGoal(goal, '该进球记录不存在或已撤销。')
  const { player, assist } = goalPlayers(payload)
  // 原进球记录不可修改；补充记录与撤销轨迹均保留在审计时间线中。
  recordMatchEvent(
    match,
    `${match.config[goal.team]}进球 #${goal.id} 球员信息：${player || '未填写'}；助攻：${assist || '未填写'}。比分与时钟未更改。`,
    '记录补充',
    { category: 'goal-detail', targetId: goal.id, team: goal.team, player, assist },
  )
}

function reconcileGoldenGoal(match, payload) {
  if (!match.rules.goldenGoal || !isOvertime(match)) return
  if (overtimeGoals(match).length > 1 || (match.goldenGoal && match.scores.home === match.scores.away)) {
    holdGoldenConflict(match)
  } else if (payload.period > match.rules.periods
    && match.scores.home !== match.scores.away
    && match.phase !== 'finished') {
    confirmGoldenGoal(match)
  }
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

function requireTeam(team) {
  requireGoal(['home', 'away'].includes(team), '请选择进球队伍。')
}

function requireGoal(ok, message) {
  if (!ok) throw new Error(message)
}
