import assert from 'node:assert/strict'
import test from 'node:test'
import { canClearMatchRecords } from './matchHeader.js'

test('服务器比赛在页面顶部显示清空记录入口', () => {
  assert.equal(canClearMatchRecords('18'), true)
  assert.equal(canClearMatchRecords(null), false)
})
