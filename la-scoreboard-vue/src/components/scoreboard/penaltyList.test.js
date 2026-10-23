import assert from 'node:assert/strict'
import test from 'node:test'
import {
  activePenalties,
  penaltyDetails,
  penaltyStatus,
} from './penaltyList.js'

const match = {
  running: false,
  rules: { minor: 30_000, major: 60_000 },
  disqualified: ['home:12'],
  penalties: [],
}

test('罚时区不展示已经释放的罚时', () => {
  const active = { id: 1, released: false }
  match.penalties = [active, { id: 2, released: true }]
  assert.deepEqual(activePenalties(match), [active])
})

test('罚时状态按到时、就座、复赛和运行顺序判断', () => {
  assert.equal(penaltyStatus(match, { remaining: 0, seated: true, started: true }), '罚时到，等待释放')
  assert.equal(penaltyStatus(match, { remaining: 10_000, seated: false, started: false }), '等待就座')
  assert.equal(penaltyStatus(match, { remaining: 10_000, seated: true, started: false }), '等待复赛哨')
  assert.equal(penaltyStatus(match, { remaining: 10_000, seated: true, started: true }), '暂停')
})

test('罚时说明包含牌型、原因、代罚和罚退状态', () => {
  const penalty = {
    team: 'home',
    player: '12',
    server: '8',
    kind: 'major',
    reason: '非法身体接触',
  }
  assert.equal(
    penaltyDetails(match, penalty),
    '黄牌 · 60 秒 / 非法身体接触 / 8 号代罚 / 犯规者本场禁入',
  )
})
