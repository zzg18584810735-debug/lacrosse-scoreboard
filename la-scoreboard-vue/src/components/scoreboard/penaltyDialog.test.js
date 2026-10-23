import assert from 'node:assert/strict'
import test from 'node:test'
import { createPenaltyDraft, penaltyKindOptions, requiresPenaltyServer } from './penaltyDialog.js'

test('判罚表单沿用入口指定的球队和牌型', () => {
  assert.deepEqual(createPenaltyDraft({ team: 'away', kind: 'minor' }), {
    team: 'away', player: '', subject: 'player', kind: 'minor', reason: '', server: '', awarded: 'opponent',
  })
})

test('红牌、官员和需要代理的守门员判罚必须填写代罚球员', () => {
  const match = { rules: { goalieProxy: true } }
  assert.equal(requiresPenaltyServer(match, { kind: 'expulsion', subject: 'player' }), true)
  assert.equal(requiresPenaltyServer(match, { kind: 'major', subject: 'official' }), true)
  assert.equal(requiresPenaltyServer(match, { kind: 'major', subject: 'goalie' }), true)
  assert.equal(requiresPenaltyServer(match, { kind: 'minor', subject: 'player' }), false)
  assert.equal(requiresPenaltyServer(match, { kind: 'minor-turnover', subject: 'official' }), false)
})

test('判罚选项使用本场规则中的罚时时长', () => {
  const options = penaltyKindOptions({ rules: { minor: 30000, major: 60000, expulsion: 120000 } })
  assert.deepEqual(options.map(option => option.label), [
    '轻微犯规 · 仅换球权，无罚时', '绿牌 · 30 秒', '黄牌 · 60 秒', '红牌 · 驱逐 + 代罚 120 秒',
  ])
})
