import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from './matchModel.js'
import { act } from './scoreboardActions.js'

function createSixes() {
  return createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
}

function liveMatch() {
  const match = createSixes()
  match.phase = 'period'
  match.periodStarted = true
  match.running = true
  match.ball = 'live'
  match.possession = 'home'
  return match
}

test('鸣哨复赛重置待恢复进攻钟并启动已就座罚时', () => {
  const match = createSixes()
  match.possession = 'home'
  match.shot = 0
  match.resetOnResume = true
  match.penalties.push({ id: 1, seated: true, remaining: 30000, started: false, released: false })

  act(match, 'resume')

  assert.equal(match.phase, 'period')
  assert.equal(match.periodStarted, true)
  assert.equal(match.running, true)
  assert.equal(match.ball, 'live')
  assert.equal(match.shot, match.rules.shot)
  assert.equal(match.resetOnResume, false)
  assert.equal(match.penalties[0].started, true)
  assert.equal(match.events.at(-1).eventType, 'MATCH_START')
  assert.equal(match.events.at(-1).team, 'home')
  assert.equal(match.events.at(-1).periodNumber, 1)
  assert.equal(match.events.at(-1).remaining, match.rules.period)
})

test('首次鸣哨前先确认开场球权且不提前生成比赛事件', () => {
  const match = createSixes()

  assert.throws(() => act(match, 'resume'), /请先确认本节开场球权/)
  act(match, 'possession', { team: 'away' })

  assert.equal(match.possession, 'away')
  assert.equal(match.events.length, 0)
  assert.match(match.notice, /等待鸣哨/)

  act(match, 'resume')
  assert.equal(match.events[0].eventType, 'MATCH_START')
  assert.equal(match.events[0].team, 'away')
})

test('新节首次鸣哨和节内复赛使用不同事件类型', () => {
  const match = createSixes()
  match.period = 2
  match.possession = 'away'

  act(match, 'resume')
  assert.equal(match.events.at(-1).eventType, 'PERIOD_START')

  act(match, 'pause')
  act(match, 'resume')
  assert.equal(match.events.at(-1).eventType, 'SYSTEM')
  assert.equal(match.events.at(-1).team, undefined)
})

test('进攻钟到期且没有待重置时禁止复赛', () => {
  const match = createSixes()
  match.possession = 'home'
  match.shot = 0

  assert.throws(() => act(match, 'resume'), /进攻钟已到期/)
  assert.equal(match.phase, 'ready')
})

test('普通死球是否停表取决于当前停表规则', () => {
  const ordinary = liveMatch()
  act(ordinary, 'whistle')
  assert.equal(ordinary.ball, 'dead')
  assert.equal(ordinary.running, true)
  assert.match(ordinary.notice, /普通死球/)

  const finalWindow = liveMatch()
  finalWindow.period = finalWindow.rules.periods
  finalWindow.main = finalWindow.rules.stopWindow
  act(finalWindow, 'whistle')
  assert.equal(finalWindow.running, false)
  assert.match(finalWindow.notice, /死球停表/)
})

test('裁判暂停强制停止比赛钟并保持进攻时间', () => {
  const match = liveMatch()
  match.shot = 17000

  act(match, 'pause')

  assert.equal(match.running, false)
  assert.equal(match.ball, 'dead')
  assert.equal(match.shot, 17000)
})

test('比赛首次鸣哨前和已经停表时不能裁判暂停', () => {
  const ready = createSixes()
  assert.throws(() => act(ready, 'pause'), /请先鸣哨开赛/)

  const stopped = liveMatch()
  stopped.running = false
  assert.throws(() => act(stopped, 'pause'), /已经暂停/)
})

test('换队球权重置进攻钟并清除原球队区域状态', () => {
  const match = liveMatch()
  match.shot = 12000
  match.frontcourt = true
  match.clear = 5000

  act(match, 'possession', { team: 'away' })

  assert.equal(match.possession, 'away')
  assert.equal(match.shot, match.rules.shot)
  assert.equal(match.frontcourt, false)
  assert.equal(match.clear, null)
  assert.equal(match.events.at(-1).eventType, 'POSSESSION')
  assert.equal(match.events.at(-1).team, 'away')
})

test('进攻钟重置和射偏记录不改变当前球权', () => {
  const match = liveMatch()
  match.shot = 5000

  act(match, 'reset-shot')
  assert.equal(match.shot, match.rules.shot)
  assert.equal(match.possession, 'home')

  act(match, 'wide')
  assert.equal(match.shot, match.rules.shot)
  assert.equal(match.possession, 'home')
  assert.match(match.notice, /不自动重置进攻钟/)
})

test('后场推进提示在进入前场后停止', () => {
  const match = liveMatch()

  act(match, 'backcourt')
  assert.equal(match.clear, match.rules.clearMs)
  assert.equal(match.frontcourt, false)

  act(match, 'frontcourt')
  assert.equal(match.clear, null)
  assert.equal(match.frontcourt, true)
})

test('交替球权判给当前球队后翻转下一次归属并停表', () => {
  const match = createSixes()
  act(match, 'alternate-set', { team: 'home' })
  match.phase = 'period'
  match.periodStarted = true
  match.running = true
  match.ball = 'live'

  act(match, 'alternate-award')

  assert.equal(match.possession, 'home')
  assert.equal(match.alternate, 'away')
  assert.equal(match.running, false)
  assert.equal(match.ball, 'dead')
  assert.equal(match.shot, match.rules.shot)
  assert.equal(match.events.at(-1).eventType, 'POSSESSION')
  assert.equal(match.events.at(-1).team, 'home')
})
