import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from './matchModel.js'
import { matchRecords } from './matchRecords.js'
import { act } from './scoreboardActions.js'

function liveMatch() {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  match.phase = 'period'
  match.periodStarted = true
  match.running = true
  match.ball = 'live'
  match.possession = 'home'
  return match
}

test('比赛开始前不能记录判罚', () => {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  const before = structuredClone(match)

  assert.throws(() => act(match, 'penalty', {
    team: 'home',
    player: '7',
    subject: 'player',
    kind: 'major',
    reason: '非法身体接触',
    awarded: 'opponent',
  }), /请先鸣哨开赛/)
  assert.deepEqual(match, before)
})

test('轻微犯规只换球权且不创建罚时', () => {
  const match = liveMatch()
  match.shot = 12000

  act(match, 'penalty', {
    team: 'home',
    player: '7',
    subject: 'player',
    kind: 'minor-turnover',
    reason: '推人',
    awarded: 'opponent',
  })

  assert.equal(match.penalties.length, 0)
  assert.equal(match.possession, 'away')
  assert.equal(match.shot, match.rules.shot)
  assert.equal(matchRecords(match, 'penalty')[0].kind, 'minor-turnover')
})

test('有罚时判罚停表并创建等待就座的罚时', () => {
  const match = liveMatch()

  act(match, 'penalty', {
    team: 'home',
    player: '7',
    subject: 'player',
    kind: 'major',
    reason: '非法身体接触',
    awarded: 'opponent',
  })

  const penalty = match.penalties[0]
  assert.equal(match.running, false)
  assert.equal(match.ball, 'dead')
  assert.equal(match.possession, 'away')
  assert.equal(match.resetOnResume, true)
  assert.equal(penalty.remaining, match.rules.major)
  assert.equal(penalty.seated, false)
  assert.equal(match.majors['home:7'], 1)
  assert.equal(match.events.length, 1)
  assert.equal(match.events[0].category, 'penalty')
  assert.match(match.events[0].text, /计时停止于 08:00/)
  assert.doesNotMatch(match.events[0].text, /裁判暂停/)
})

test('无罚时犯规按规则停表时写入同一条判罚详情', () => {
  const match = liveMatch()
  match.rules.foulStop = true

  act(match, 'penalty', {
    team: 'away',
    player: '9',
    subject: 'player',
    kind: 'minor-turnover',
    reason: '推人',
    awarded: 'home',
  })

  assert.equal(match.running, false)
  assert.equal(match.events.length, 1)
  assert.match(match.events[0].text, /计时停止于 08:00/)
})

test('球队官员判罚要求代罚但不累计球员重大犯规', () => {
  const match = liveMatch()

  act(match, 'penalty', {
    team: 'away',
    player: '教练',
    server: '8',
    subject: 'official',
    kind: 'major',
    reason: '不当行为',
    awarded: 'home',
  })

  assert.deepEqual(match.majors, {})
  assert.deepEqual(match.disqualified, [])
  assert.equal(match.penalties[0].server, '8')
  assert.match(match.events.at(-1).text, /官员犯规的代罚不累计球员黄牌/)
})

test('球员达到重大犯规上限后进入罚退名单', () => {
  const match = liveMatch()
  match.rules.majorLimit = 2
  const payload = {
    team: 'home',
    player: '7',
    subject: 'player',
    kind: 'major',
    reason: '',
    awarded: 'opponent',
  }

  act(match, 'penalty', payload)
  match.penalties[0].released = true
  act(match, 'penalty', payload)

  assert.equal(match.majors['home:7'], 2)
  assert.deepEqual(match.disqualified, ['home:7'])
  assert.match(match.events.at(-1).text, /本场不得再上场/)
})

test('罚时就座后随复赛开始并可在到时后释放', () => {
  const match = liveMatch()
  act(match, 'penalty', {
    team: 'home',
    player: '7',
    subject: 'player',
    kind: 'minor',
    reason: '',
    awarded: 'opponent',
  })
  const penaltyId = match.penalties[0].id

  act(match, 'seat', { id: penaltyId })
  let penalty = match.penalties[0]
  assert.equal(penalty.seated, true)
  assert.equal(penalty.started, false)
  assert.equal(match.events.at(-1).team, 'home')
  assert.equal(match.events.at(-1).player, '7')

  act(match, 'resume')
  penalty = match.penalties[0]
  assert.equal(penalty.started, true)
  penalty.remaining = 0
  act(match, 'release', { id: penaltyId })
  assert.equal(match.penalties[0].released, true)
  assert.equal(match.events.at(-1).team, 'home')
  assert.equal(match.events.at(-1).player, '7')
})

test('判罚更正保留同一代罚者就座状态并重算重大犯规', () => {
  const match = liveMatch()
  act(match, 'penalty', {
    team: 'home',
    player: '7',
    subject: 'player',
    kind: 'major',
    reason: '原判罚',
    awarded: 'opponent',
  })
  const penalty = match.penalties[0]
  act(match, 'seat', { id: penalty.id })

  act(match, 'correct-penalty', {
    id: penalty.id,
    mode: 'edit',
    team: 'home',
    player: '7',
    server: '7',
    subject: 'player',
    kind: 'minor',
    reason: '裁判改判绿牌',
    remaining: 20000,
    correctionReason: '裁判重新核对动作',
  })

  const corrected = match.penalties[0]
  assert.equal(corrected.kind, 'minor')
  assert.equal(corrected.remaining, 20000)
  assert.equal(corrected.seated, true)
  assert.deepEqual(match.majors, {})
  assert.equal(matchRecords(match, 'penalty')[0].kind, 'minor')
})

test('作废判罚移除罚时并保留审计记录', () => {
  const match = liveMatch()
  act(match, 'penalty', {
    team: 'away',
    player: '9',
    subject: 'player',
    kind: 'major',
    reason: '',
    awarded: 'home',
  })
  const penaltyId = match.penalties[0].id

  act(match, 'correct-penalty', {
    id: penaltyId,
    mode: 'cancel',
    correctionReason: '裁判确认未构成犯规',
  })

  assert.equal(match.penalties.length, 0)
  assert.deepEqual(match.majors, {})
  assert.equal(matchRecords(match, 'penalty')[0].voided, true)
  assert.equal(match.events.at(-1).category, 'penalty-correction')
})
