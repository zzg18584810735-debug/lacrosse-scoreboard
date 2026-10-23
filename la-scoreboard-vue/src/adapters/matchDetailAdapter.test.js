import assert from 'node:assert/strict'
import test from 'node:test'
import {
  applyMatchStateSaveResult,
  toMatchCreateRequest,
  toMatchStateSaveRequest,
  toScoreboardRuleRequest,
  toScoreboardMatch,
} from './matchDetailAdapter.js'

test('后端详情转换规则单位、运行状态、事件和罚时', () => {
  const startedAt = '2026-10-21T10:00:00'
  const detail = createDetail({ timerStartedAt: startedAt })

  const match = toScoreboardMatch(detail, {
    now: Date.parse(startedAt) + 5000,
  })

  assert.equal(match.config.matchId, 10)
  assert.equal(match.config.preset, 'sll-sixes')
  assert.equal(match.rules.period, 480000)
  assert.equal(match.rules.shot, 30000)
  assert.equal(match.phase, 'period')
  assert.equal(match.periodStarted, true)
  assert.equal(match.main, 295000)
  assert.equal(match.shot, 15000)
  assert.equal(match.scores.home, 1)
  assert.equal(match.events[1].id, 2)
  assert.equal(match.events[1].category, 'penalty')
  assert.equal(match.events[1].player, '7')
  assert.equal(match.penalties[0].id, 2)
  assert.equal(match.penalties[0].server, '8')
  assert.equal(match.penalties[0].remaining, 25000)
  assert.equal(match.penalties[0].started, true)
})

test('后端未返回组别时按自定义规则恢复比赛', () => {
  const detail = createDetail()
  delete detail.preset

  const match = toScoreboardMatch(detail)

  assert.equal(match.config.preset, 'custom')
  assert.equal(match.rules.period, 480000)
  assert.equal(match.rules.shot, 30000)
})

test('进行中或球队暂停阶段即使缺少旧版开节事件也恢复为本节已开始', () => {
  const periodDetail = createDetail({ phase: 'PERIOD', mainRunning: 0 })
  periodDetail.events = periodDetail.events.filter(event => event.eventType !== 'PERIOD_START')
  assert.equal(toScoreboardMatch(periodDetail).periodStarted, true)

  const timeoutDetail = createDetail({
    phase: 'TIMEOUT',
    mainRunning: 0,
    auxiliaryRunning: 1,
    timeoutReturnPhase: 'PERIOD',
  })
  timeoutDetail.events = timeoutDetail.events.filter(event => event.eventType !== 'PERIOD_START')
  assert.equal(toScoreboardMatch(timeoutDetail).periodStarted, true)
})

test('暂停上下文保留返回阶段并按计时锚点推进辅助钟', () => {
  const startedAt = '2026-10-21T10:00:00'
  const detail = createDetail({
    phase: 'TIMEOUT',
    mainRunning: 0,
    auxiliaryRunning: 1,
    auxiliaryRemainingSeconds: 30,
    timerStartedAt: startedAt,
    timeoutEventId: 102,
    timeoutReturnPhase: 'PERIOD',
    timeoutReturnAuxiliarySeconds: 0,
    timeoutReturnAuxiliaryRunning: 0,
  })
  detail.events.push({
    id: 102,
    sequenceNo: 3,
    eventType: 'TEAM_TIMEOUT',
    periodNo: 1,
    remainingSeconds: 295,
    durationSeconds: 60,
    teamSide: 'home',
    timeoutBucket: 'FIRST_HALF',
    timeoutCondition: 'FRONTCOURT',
    eventStatus: 'VALID',
    eventTitle: '主队暂停（60秒）',
    eventText: '主队在前场控球条件下使用60秒球队暂停。',
    ruleReference: '本场球队暂停规则',
  })

  const match = toScoreboardMatch(detail, { now: Date.parse(startedAt) + 5000 })

  assert.equal(match.phase, 'timeout')
  assert.equal(match.auxiliary, 25000)
  assert.equal(match.timeoutContext.phase, 'period')
  assert.equal(match.timeoutContext.timeoutId, 3)
  assert.equal(match.events.at(-1).condition, 'frontcourt')
  assert.equal(match.events.at(-1).durationSeconds, 60)
  assert.equal(match.events.at(-1).title, '主队暂停（60秒）')
})

test('后端事件引用不存在的名单球员时拒绝载入', () => {
  const detail = createDetail()
  detail.roster = detail.roster.filter(player => player.id !== 1)

  assert.throws(() => toScoreboardMatch(detail), /不存在的本场球员/)
})

test('前端比赛状态转换为后端状态、多事件和罚时请求', () => {
  const detail = createDetail()
  const match = toScoreboardMatch(detail)
  match.main = 290500
  match.penalties[0].remaining = 24500
  match.events.push({
    id: 3,
    category: 'goal',
    team: 'home',
    player: '7',
    assist: '8',
    points: 1,
    periodNumber: 1,
    remaining: 290000,
  })
  match.sequence = 3

  const request = toMatchStateSaveRequest(match, detail)

  assert.equal(request.phase, 'PERIOD')
  assert.equal(request.mainRemainingSeconds, 291)
  assert.equal(request.events.length, 1)
  assert.equal(request.events[0].eventType, 'GOAL')
  assert.equal(request.events[0].rosterId, 1)
  assert.equal(request.events[0].assistRosterId, 2)
  assert.deepEqual(request.penalties, [{
    id: 9,
    remainingSeconds: 25,
    penaltyState: 'ACTIVE',
  }])
})

test('比赛节点和球权使用正式事件类型提交后端', () => {
  const detail = createDetail()
  detail.events = []
  detail.penalties = []
  const match = toScoreboardMatch(detail)
  match.events = [
    { id: 1, eventType: 'MATCH_START', periodNumber: 1, remaining: 480000, text: '比赛开始', rule: '5.3.1' },
    { id: 2, eventType: 'POSSESSION', periodNumber: 1, remaining: 470000, team: 'away', text: '客队获得球权', rule: '6.3' },
    { id: 3, eventType: 'PERIOD_END', periodNumber: 1, remaining: 0, text: '第一节结束' },
    { id: 4, eventType: 'MATCH_END', periodNumber: 4, remaining: 0, text: '比赛结束' },
  ]

  const request = toMatchStateSaveRequest(match, detail)

  assert.deepEqual(request.events.map(event => event.eventType), [
    'MATCH_START', 'POSSESSION', 'PERIOD_END', 'MATCH_END',
  ])
  assert.equal(request.events[1].teamSide, 'away')
  assert.equal(request.events[1].eventTitle, '客队获得球权')
  assert.equal(request.events[1].ruleReference, '6.3')
})

test('罚时执行系统事件往返后端时保留关联球队和人员', () => {
  const detail = createDetail()
  detail.events = [{
    id: 110,
    sequenceNo: 1,
    eventType: 'SYSTEM',
    periodNo: 1,
    remainingSeconds: 260,
    teamSide: 'home',
    subjectName: '8',
    eventStatus: 'VALID',
    eventTitle: '主队8号已就座',
    eventText: '主队8号已就座，等待复赛哨。',
    ruleReference: '9.4.1',
  }]
  detail.penalties = []

  const restored = toScoreboardMatch(detail)
  assert.equal(restored.events[0].team, 'home')
  assert.equal(restored.events[0].player, '8')

  restored.events[0].databaseId = undefined
  const request = toMatchStateSaveRequest(restored, detail)
  assert.equal(request.events[0].teamSide, 'home')
  assert.equal(request.events[0].subjectName, '8')
})

test('状态保存响应会回填新增事件和罚时的数据库 ID', () => {
  const match = toScoreboardMatch(createDetail())
  match.events.push({ id: 3, category: 'penalty', periodNumber: 1 })
  match.penalties.push({ id: 3, remaining: 30000 })

  applyMatchStateSaveResult(match, {
    state: { stateVersion: 4 },
    events: [{
      id: 102,
      sequenceNo: 3,
      eventType: 'PENALTY',
      periodNo: 1,
      remainingSeconds: 290,
      teamSide: 'home',
      rosterId: 1,
      servedByRosterId: 2,
      penaltySubject: 'player',
      penaltyType: 'minor',
      eventStatus: 'VALID',
      eventText: '主队 7号张三被判绿牌。',
      ruleReference: '本场判罚规则',
    }],
    penalties: [{ id: 202, penaltyEventId: 102 }],
  }, createDetail())

  assert.equal(match.events.at(-1).databaseId, 102)
  assert.equal(match.events.at(-1).text, '主队 7号张三被判绿牌。')
  assert.equal(match.events.at(-1).player, '7')
  assert.equal(match.penalties.at(-1).databaseId, 202)
})

test('状态保存后立即采用数据库事件快照避免刷新前后变化', () => {
  const detail = createDetail()
  detail.roster = []
  detail.events = []
  detail.penalties = []
  const match = toScoreboardMatch(detail)
  match.events.push({
    id: 1,
    category: 'goal',
    team: 'home',
    player: '7',
    points: 1,
    periodNumber: 1,
    remaining: 290000,
    text: '前端临时进球说明',
    rule: '前端临时依据',
  })

  applyMatchStateSaveResult(match, {
    state: { stateVersion: 4 },
    events: [{
      id: 102,
      sequenceNo: 1,
      eventType: 'GOAL',
      periodNo: 1,
      remainingSeconds: 290,
      teamSide: 'home',
      subjectName: '7',
      assistName: '12号李四',
      points: 1,
      eventStatus: 'VALID',
      eventTitle: '主队进球',
      eventText: '主队 7进球，增加1分。',
      ruleReference: '裁判进球判定',
    }],
    penalties: [],
  }, detail)

  assert.equal(match.events[0].text, '主队 7进球，增加1分。')
  assert.equal(match.events[0].title, '主队进球')
  assert.equal(match.events[0].rule, '裁判进球判定')
  assert.equal(match.events[0].team, 'home')
  assert.equal(match.events[0].player, '7')
  assert.equal(match.events[0].assist, '12号李四')
})

test('本场名单为空时用输入的号码或姓名保存进球快照', () => {
  const detail = createDetail()
  detail.roster = []
  detail.events = []
  detail.penalties = []
  const match = toScoreboardMatch(detail)
  match.events.push({
    id: 3,
    category: 'goal',
    team: 'home',
    player: '8号张三',
    assist: '12号李四',
    points: 1,
    periodNumber: 1,
    remaining: 290000,
  })

  const request = toMatchStateSaveRequest(match, detail)

  assert.equal(request.events[0].eventType, 'GOAL')
  assert.equal(request.events[0].rosterId, null)
  assert.equal(request.events[0].subjectName, '8号张三')
  assert.equal(request.events[0].assistRosterId, null)
  assert.equal(request.events[0].assistName, '12号李四')
})

test('本场名单为空时允许填写判罚和服罚球员快照', () => {
  const detail = createDetail()
  detail.roster = []
  detail.events = []
  detail.penalties = []
  const match = toScoreboardMatch(detail)
  match.events.push({
    id: 1,
    category: 'penalty',
    team: 'away',
    player: '临时7号',
    server: '临时8号',
    subject: 'player',
    kind: 'major',
    reason: '非法身体接触',
    periodNumber: 1,
    remaining: 290000,
  })

  const request = toMatchStateSaveRequest(match, detail)

  assert.equal(request.events[0].eventType, 'PENALTY')
  assert.equal(request.events[0].rosterId, null)
  assert.equal(request.events[0].subjectName, '临时7号')
  assert.equal(request.events[0].servedByRosterId, null)
  assert.equal(request.events[0].servedByName, '临时8号')
  assert.equal(request.events[0].description, '计时停止')
})

test('后端判罚文字快照恢复后仍显示被罚和服罚人员', () => {
  const detail = createDetail()
  detail.roster = []
  detail.events = [{
    id: 101,
    sequenceNo: 1,
    eventType: 'PENALTY',
    periodNo: 1,
    remainingSeconds: 290,
    teamSide: 'away',
    subjectName: '临时7号',
    servedByName: '临时8号',
    penaltySubject: 'player',
    penaltyType: 'major',
    penaltySeconds: 60,
    eventStatus: 'VALID',
    eventTitle: '客队判罚',
    eventText: '客队 临时7号被判黄牌，罚时60秒，由临时8号服罚。',
  }]
  detail.penalties = [{
    id: 9,
    penaltyEventId: 101,
    remainingSeconds: 60,
    penaltyState: 'WAITING',
  }]

  const match = toScoreboardMatch(detail)

  assert.equal(match.events[0].player, '临时7号')
  assert.equal(match.events[0].server, '临时8号')
  assert.equal(match.penalties[0].player, '临时7号')
  assert.equal(match.penalties[0].server, '临时8号')
})

test('新建比赛提交组别规则 ID 并将规则单位转换为分钟和秒', () => {
  const match = toScoreboardMatch(createDetail())
  const rules = toScoreboardRuleRequest(match.rules)
  const request = toMatchCreateRequest({
    title: '测试比赛',
    date: '2026-10-21',
    ageGroup: 'sll-sixes',
    groupRuleId: '6',
    stage: 'league',
    home: '甲队',
    away: '乙队',
    rules: match.rules,
  })

  assert.equal(rules.periodMinutes, 8)
  assert.equal(rules.shotClockSeconds, 30)
  assert.equal(request.groupRuleId, 6)
  assert.equal(request.ageGroup, 'sll-sixes')
  assert.equal(request.saveAsTemplate, false)
  assert.deepEqual(request.roster, [])
  assert.equal(request.rules.timeoutSeconds, 60)
  assert.equal(request.rules.alternateFirst, match.rules.alternateFirst)
})

test('新建比赛允许自定义规则仅用于本场或另存为模板', () => {
  const match = toScoreboardMatch(createDetail())
  const request = toMatchCreateRequest({
    title: '自定义规则比赛',
    date: '2026-10-21',
    ageGroup: 'U12',
    groupRuleId: '',
    saveAsTemplate: true,
    templateName: 'U12 淘汰赛模板',
    stage: 'knockout',
    home: '甲队',
    away: '乙队',
    rules: match.rules,
  })

  assert.equal(request.groupRuleId, null)
  assert.equal(request.ageGroup, 'U12')
  assert.equal(request.saveAsTemplate, true)
  assert.equal(request.templateName, 'U12 淘汰赛模板')
})

test('作废判罚同时提交原事件状态和更正审计事件', () => {
  const detail = createDetail()
  const match = toScoreboardMatch(detail)
  match.penalties = []
  match.events.push({
    id: 3,
    category: 'penalty-correction',
    targetId: 2,
    periodNumber: 1,
    remaining: 300000,
    before: { team: 'home', player: '7' },
    changes: { cancelled: true, team: 'home' },
    correctionReason: '裁判确认原判无效',
  })

  const request = toMatchStateSaveRequest(match, detail)

  assert.deepEqual(request.eventStatuses, [{ id: 101, eventStatus: 'CANCELLED' }])
  assert.equal(request.events[0].eventType, 'CORRECTION')
  assert.equal(request.events[0].relatedEventId, 101)
})

function createDetail(stateOverrides = {}) {
  return {
    match: {
      id: 10,
      title: '测试比赛',
      matchNo: 'M-10',
      matchDate: '2026-10-21',
      matchTime: '10:00:00',
      venueName: '一号场',
      groupRuleId: 1,
      stage: 'league',
      homeTeamId: 11,
      homeTeamName: '主队',
      homeTeamColor: 'red',
      awayTeamId: 12,
      awayTeamName: '客队',
      awayTeamColor: 'blue',
      status: 'ONGOING',
      homeScore: 1,
      awayScore: 0,
    },
    preset: 'sll-sixes',
    rule: createRule(),
    state: {
      matchId: 10,
      phase: 'PERIOD',
      currentPeriod: 1,
      mainRemainingSeconds: 300,
      shotRemainingSeconds: 20,
      clearRemainingSeconds: null,
      auxiliaryType: null,
      auxiliaryRemainingSeconds: 0,
      timeoutEventId: null,
      timeoutReturnPhase: null,
      timeoutReturnAuxiliaryType: null,
      timeoutReturnAuxiliarySeconds: null,
      timeoutReturnAuxiliaryRunning: null,
      mainRunning: 1,
      auxiliaryRunning: 0,
      timerStartedAt: null,
      ballState: 'live',
      possessionSide: 'home',
      frontcourt: 0,
      alternateSide: 'away',
      resetShotOnResume: 0,
      goldenGoal: 0,
      goldenConflict: 0,
      pendingGoalSequenceNo: null,
      homeTimeoutsRemaining: 1,
      awayTimeoutsRemaining: 1,
      stateVersion: 3,
      ...stateOverrides,
    },
    roster: [
      { id: 1, teamSide: 'home', jerseyNo: 7, playerName: '张三' },
      { id: 2, teamSide: 'home', jerseyNo: 8, playerName: '李四' },
    ],
    events: [
      {
        id: 100,
        sequenceNo: 1,
        eventType: 'PERIOD_START',
        periodNo: 1,
        remainingSeconds: 480,
        eventStatus: 'VALID',
        eventText: '第1节开始。',
        ruleReference: '本场比赛规则',
      },
      {
        id: 101,
        sequenceNo: 2,
        eventType: 'PENALTY',
        periodNo: 1,
        remainingSeconds: 300,
        teamSide: 'home',
        rosterId: 1,
        servedByRosterId: 2,
        penaltySubject: 'player',
        penaltyType: 'major',
        foulItem: '非法身体接触',
        eventStatus: 'VALID',
        eventText: '主队7号球员被判黄牌。',
        ruleReference: '本场判罚规则',
      },
    ],
    penalties: [{
      id: 9,
      penaltyEventId: 101,
      remainingSeconds: 30,
      penaltyState: 'ACTIVE',
    }],
    coaches: [],
    officials: [],
  }
}

function createRule() {
  return {
    playersPerTeam: 6,
    regularPeriods: 4,
    periodMinutes: 8,
    halftimeAfterPeriod: 2,
    shotClockSeconds: 30,
    clearSeconds: 10,
    pointsPerGoal: 1,
    periodBreakMinutes: 2,
    halftimeMinutes: 5,
    timeoutSeconds: 60,
    timeoutsPerHalf: 1,
    overtimeTimeouts: 0,
    stopWindowSeconds: 120,
    allDeadStop: 0,
    foulStop: 0,
    overtimeStopAll: 1,
    shotLastOff: 1,
    overtimePolicy: 'always',
    overtimeMinutes: 3,
    goldenGoal: 0,
    overtimeBreakMinutes: 2,
    minorPenaltySeconds: 30,
    majorPenaltySeconds: 60,
    expulsionPenaltySeconds: 120,
    majorLimit: 3,
    goalieProxy: 1,
    passRequired: 0,
    alternateFirst: 'manual',
    alternateReset: 0,
  }
}
