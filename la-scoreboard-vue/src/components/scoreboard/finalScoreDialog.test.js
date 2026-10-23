import assert from 'node:assert/strict'
import test from 'node:test'
import { createFinalScoreDraft, finalScoreChanged } from './finalScoreDialog.js'

const match = { scores: { home: 8, away: 7 } }

test('赛后比分更正表单复制当前最终比分', () => {
  assert.deepEqual(createFinalScoreDraft(match), { home: 8, away: 7, reason: '' })
})

test('赛后比分更正识别主队或客队分数变化', () => {
  assert.equal(finalScoreChanged(match, { home: '8', away: '7' }), false)
  assert.equal(finalScoreChanged(match, { home: '9', away: '7' }), true)
  assert.equal(finalScoreChanged(match, { home: '8', away: '6' }), true)
})
