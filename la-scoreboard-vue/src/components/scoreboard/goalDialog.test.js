import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from '../../core/matchModel.js'
import { createGoalDialogDraft, goalDialogDraftKey, otherGoalTeam } from './goalDialog.js'

test('进球球队可在主客队之间切换', () => {
  assert.equal(otherGoalTeam('home'), 'away')
  assert.equal(otherGoalTeam('away'), 'home')
})

test('即时进球表单沿用尚未确认的草稿', () => {
  const match = createMatch({ home: '甲队', away: '乙队' })
  match.pendingGoal = { id: 7, team: 'away', period: 1, main: 123000, player: '9', assist: '3' }
  assert.deepEqual(createGoalDialogDraft(match, 'entry'), {
    team: 'away', goalId: 7, player: '9', assist: '3', period: 1,
    minutes: 2, seconds: 3, scoreMode: 'add', reason: '', latestPeriod: 0, main: 123000,
  })
})

test('比赛倒计时刷新时不会重置同一个进球草稿', () => {
  const match = createMatch({ home: '甲队', away: '乙队' })
  match.pendingGoal = { id: 7, team: 'home', period: 1, main: 123000, player: '', assist: '' }
  const before = goalDialogDraftKey(match, 'entry')

  match.main -= 100

  assert.equal(goalDialogDraftKey(match, 'entry'), before)
})

test('补录进球默认使用最近已开赛节次和整秒时间', () => {
  const match = createMatch({ home: '甲队', away: '乙队' })
  match.periodStarted = true
  match.main = 122001
  assert.deepEqual(createGoalDialogDraft(match, 'backfill', 'away'), {
    team: 'away', goalId: '', player: '', assist: '', period: 1,
    minutes: 2, seconds: 3, scoreMode: 'add', reason: '', latestPeriod: 1, main: 123000,
  })
})
