import assert from 'node:assert/strict'
import test from 'node:test'
import {
  loadStoredMatch,
  MATCH_STORE_KEY,
  prepareMatchStorage,
  saveStoredMatch,
} from './matchStorage.js'

function createMemoryStorage(initial = {}) {
  const values = new Map(Object.entries(initial))
  return {
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, String(value)),
    removeItem: key => values.delete(key),
  }
}

test('比赛缓存可以保存并恢复结构化状态', () => {
  const storage = createMemoryStorage()
  const match = { id: 12, scores: { home: 2, away: 1 } }

  saveStoredMatch(storage, match)

  assert.deepEqual(loadStoredMatch(storage), match)
  assert.equal(typeof storage.getItem(MATCH_STORE_KEY), 'string')
})

test('只清理已明确废弃的旧比赛缓存', () => {
  const storage = createMemoryStorage({
    'lacrosse-app-state': '{}',
    'lacrosse-sixes-v2': '{}',
    theme: 'dark',
  })

  prepareMatchStorage(storage)

  assert.equal(storage.getItem('lacrosse-app-state'), null)
  assert.equal(storage.getItem('lacrosse-sixes-v2'), null)
  assert.equal(storage.getItem('theme'), 'dark')
})
