import assert from 'node:assert/strict'
import test from 'node:test'
import { createMatch } from '../../core/matchModel.js'
import { compactSyncSummary, navigationSummary, shortMatchDate } from './matchNavigation.js'

test('顶部日期显示为月日格式', () => {
  assert.equal(shortMatchDate('2026-10-21'), '10/21')
})

test('导航摘要展示组别、赛制、节次和进攻钟', () => {
  const match = createMatch({ preset: 'sll-sixes', stage: 'knockout' })
  assert.match(navigationSummary(match), /六人制/)
  assert.match(navigationSummary(match), /淘汰／名次赛/)
  assert.match(navigationSummary(match), /4 × 8 分钟/)
  assert.match(navigationSummary(match), /30 秒进攻钟/)
})

test('全部远程同步状态固定显示在导航摘要中', () => {
  assert.equal(compactSyncSummary('synced', 3), '已同步 #3')
  assert.equal(compactSyncSummary('syncing', 3), '同步中 #3')
  assert.equal(compactSyncSummary('offline', 3), '网络离线 #3')
  assert.equal(compactSyncSummary('conflict', 3), '同步冲突 #3')
  assert.equal(compactSyncSummary('error', 3), '同步失败 #3')
  assert.equal(compactSyncSummary('local', 3), '')
  assert.equal(compactSyncSummary('synced', null), '')
})
