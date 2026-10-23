import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from './matchModel.js'
import { advance, clock, periodHasStarted, periodName } from './timing.js'

test('时钟显示按不足一秒向上取整', () => {
  assert.equal(clock(0), '00:00')
  assert.equal(clock(1001), '00:02')
  assert.equal(clock(61000), '01:01')
})

test('节次名称和旧缓存开赛状态可以恢复', () => {
  const match = createMatch({ preset: 'sll-u8', date: '2026-10-21' })
  delete match.periodStarted
  match.events.push({ period: '上半场', text: '鸣哨开赛／复赛。', voided: false })

  assert.equal(periodName(match), '上半场')
  assert.equal(periodHasStarted(match), true)
})

test('球队暂停辅助钟按实际耗时倒计时', () => {
  const match = createMatch({ preset: 'sll-u8', date: '2026-10-21' })
  Object.assign(match, {
    phase: 'timeout',
    auxiliary: 5000,
    auxiliaryRunning: true,
  })

  assert.equal(advance(match, 1200), true)
  assert.equal(match.auxiliary, 3800)
})

test('延迟回调按截止时间顺序推进所有比赛时钟', () => {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  Object.assign(match, {
    phase: 'period',
    periodStarted: true,
    running: true,
    ball: 'live',
    possession: 'home',
    main: 6000,
    shot: 5000,
    clear: 3000,
  })
  match.rules.shotLastOff = false
  match.penalties.push({ team: 'away', server: '8', remaining: 4000, started: true })

  advance(match, 6000)

  assert.equal(match.phase, 'period-end')
  assert.equal(match.main, 0)
  assert.equal(match.shot, 0)
  assert.equal(match.clear, null)
  assert.equal(match.penalties[0].remaining, 0)
  assert.equal(match.penalties[0].released, true)
  assert.deepEqual(match.events.map(event => event.text), [
    '10 秒推进提示到期，请裁判确认是否违例。',
    '蓝队 8 号罚时结束，已自动释放。',
    '进攻钟到期：等待裁判确认射门或违例，不自动判换球权。',
    '第 1 节时间到，请核对最后一次射门与比分。',
  ])
  assert.equal(match.events.at(-1).eventType, 'PERIOD_END')
})

test('罚时倒计时归零后自动释放并离开待处理状态', () => {
  const match = createMatch({ preset: 'sll-sixes', date: '2026-10-21' })
  Object.assign(match, {
    phase: 'period',
    periodStarted: true,
    running: true,
    ball: 'live',
    main: 60_000,
  })
  match.penalties.push({
    id: 1,
    team: 'home',
    server: '7',
    remaining: 1_000,
    seated: true,
    started: true,
    released: false,
  })

  advance(match, 1_000)

  assert.equal(match.penalties[0].remaining, 0)
  assert.equal(match.penalties[0].released, true)
  assert.equal(match.events.at(-1).text, '红队 7 号罚时结束，已自动释放。')
})
