import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from '../../core/matchModel.js'
import { createMatchInfoDraft } from './matchInfoDialog.js'

test('比赛信息表单只复制允许修改的基础字段', () => {
  const match = createMatch({ title: '测试比赛', date: '2026-10-21', home: '甲队', away: '乙队' })
  assert.deepEqual(createMatchInfoDraft(match), {
    title: '测试比赛',
    date: '2026-10-21',
    home: '甲队',
    away: '乙队',
  })
})
