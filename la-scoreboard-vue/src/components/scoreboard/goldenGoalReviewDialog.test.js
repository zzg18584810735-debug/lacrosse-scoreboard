import assert from 'node:assert/strict'
import test from 'node:test'
import {
  createGoldenGoalReviewDraft,
  goldenGoalCandidateLabel,
  goldenGoalCandidates,
} from './goldenGoalReviewDialog.js'

const match = {
  config: { home: '甲队', away: '乙队' },
  scores: { home: 5, away: 4 },
  rules: { periods: 2 },
  penalties: [],
  events: [
    { id: 3, category: 'goal', team: 'away', player: '9', assist: '', period: '加时 1', periodNumber: 3, remaining: 100000, time: '01:40', text: '客队进球' },
    { id: 2, category: 'goal', team: 'home', player: '7', assist: '', period: '加时 1', periodNumber: 3, remaining: 120000, time: '02:00', text: '主队进球' },
    { id: 1, category: 'goal', team: 'home', player: '8', assist: '', period: '下半场', periodNumber: 2, remaining: 0, time: '00:00', text: '常规进球' },
  ],
}

test('金球核对只列出加时进球并按实际发生顺序排列', () => {
  assert.deepEqual(goldenGoalCandidates(match).map(goal => goal.id), [2, 3])
})

test('金球核对草稿复制当前比分', () => {
  assert.deepEqual(createGoldenGoalReviewDraft(match), { goalId: '', home: 5, away: 4, reason: '' })
})

test('金球候选项明确展示记录、时间、球队和球员', () => {
  assert.equal(goldenGoalCandidateLabel(match, match.events[0]), '#3 · 加时 1 01:40 · 乙队 · 9')
})
