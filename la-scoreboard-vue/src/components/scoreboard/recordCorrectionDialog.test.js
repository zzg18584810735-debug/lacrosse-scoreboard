import assert from 'node:assert/strict'
import test from 'node:test'
import { createRecordCorrectionDraft, findCorrectionEvent } from './recordCorrectionDialog.js'

const match = {
  events: [
    { id: 1, category: 'penalty', team: 'home', player: '7', subject: 'player', kind: 'major', server: '7', reason: '挥杆', period: '第 1 节', time: '06:00', text: '判罚' },
    { id: 2, category: 'timeout', team: 'away', period: '第 1 节', time: '05:00', text: '暂停' },
    { id: 3, category: 'penalty', team: 'away', voided: true, period: '第 1 节', time: '04:00', text: '已作废' },
  ],
  penalties: [{ id: 1, team: 'home', player: '7', subject: 'player', kind: 'major', server: '8', remaining: 59999 }],
}

test('更正只能读取对应类型的有效原记录', () => {
  assert.equal(findCorrectionEvent(match, 'penalty', 1)?.id, 1)
  assert.equal(findCorrectionEvent(match, 'timeout', 1), undefined)
  assert.equal(findCorrectionEvent(match, 'penalty', 3), undefined)
})

test('判罚更正合并原事件和当前罚时状态', () => {
  assert.deepEqual(createRecordCorrectionDraft(match, 'penalty', 1), {
    mode: 'edit', team: 'home', player: '7', subject: 'player', kind: 'major', reason: '挥杆',
    server: '8', awarded: 'opponent', remaining: 59.999, correctionReason: '',
  })
})

test('暂停更正默认保留原球队', () => {
  assert.deepEqual(createRecordCorrectionDraft(match, 'timeout', 2), {
    mode: 'edit', team: 'away', correctionReason: '',
  })
})
