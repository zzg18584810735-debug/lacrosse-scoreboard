import assert from 'node:assert/strict'
import test from 'node:test'
import { MATCH_STORE_KEY } from './matchStorage.js'
import { createScoreboardPersistence } from './scoreboardPersistence.js'

function createMemoryStorage(initial = {}) {
  const values = new Map(Object.entries(initial))
  return {
    getItem: key => values.get(key) ?? null,
    removeItem: key => values.delete(key),
    setItem: (key, value) => values.set(key, value),
  }
}

test('计分板持久化服务统一恢复和保存比赛缓存', () => {
  const storage = createMemoryStorage()
  const persistence = createScoreboardPersistence(storage)
  const match = { id: 1, title: '测试比赛' }

  assert.equal(persistence.load(), null)
  assert.equal(persistence.save(match), true)
  assert.deepEqual(JSON.parse(storage.getItem(MATCH_STORE_KEY)), match)
})

test('读取失败和跨标签页修改会锁定本页写入', () => {
  const messages = []
  const brokenStorage = {
    getItem() { throw new Error('读取失败') },
    removeItem() {},
    setItem() { throw new Error('不应写入') },
  }
  const persistence = createScoreboardPersistence(brokenStorage, {
    onMessage: message => messages.push(message),
  })

  assert.equal(persistence.load(), null)
  assert.equal(persistence.blocked, true)
  assert.equal(persistence.save({}), false)
  persistence.reset()
  persistence.blockExternal()

  assert.equal(persistence.blocked, true)
  assert.match(messages[0], /缓存无法读取或保存/)
  assert.match(messages.at(-1), /另一个页面已修改/)
})
