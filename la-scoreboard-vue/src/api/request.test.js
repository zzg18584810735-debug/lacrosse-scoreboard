import assert from 'node:assert/strict'
import test from 'node:test'
import { ApiError, configureRequest, get, post } from './request.js'
import { scoreboardApi } from './scoreboard.js'

test('请求层追加查询参数、Bearer token并解包Result数据', async () => {
  let received
  configureRequest({
    baseURL: 'http://localhost:8080/api/',
    getToken: () => 'test-token',
    fetchImpl: async (url, options) => {
      received = { url, options }
      return response({ code: 200, message: '操作成功', data: [{ id: 1 }] })
    },
  })

  const data = await scoreboardApi.listMatches({ matchDate: '2026-10-21', status: 'READY', empty: '' })
  assert.deepEqual(data, [{ id: 1 }])
  assert.equal(received.url, 'http://localhost:8080/api/scoreboard/matches?matchDate=2026-10-21&status=READY')
  assert.equal(received.options.headers.get('Authorization'), 'Bearer test-token')
})

test('JSON请求设置请求体并返回业务数据', async () => {
  let received
  configureRequest({
    fetchImpl: async (url, options) => {
      received = { url, options }
      return response({ code: 200, data: 99 })
    },
  })
  assert.equal(await post('/scoreboard/matches', { title: '测试比赛' }), 99)
  assert.equal(received.options.method, 'POST')
  assert.equal(received.options.headers.get('Content-Type'), 'application/json;charset=UTF-8')
  assert.equal(received.options.body, JSON.stringify({ title: '测试比赛' }))
})

test('计分接口按比赛ID读取详情', async () => {
  let receivedUrl
  configureRequest({
    baseURL: '/api',
    getToken: () => '',
    fetchImpl: async url => {
      receivedUrl = url
      return response({ code: 200, data: { match: { id: 12 } } })
    },
  })

  const detail = await scoreboardApi.getMatchDetail(12)

  assert.equal(receivedUrl, '/api/scoreboard/matches/12')
  assert.equal(detail.match.id, 12)
})

test('清空比赛记录接口提交当前状态版本', async () => {
  let received
  configureRequest({
    baseURL: '/api',
    getToken: () => '',
    fetchImpl: async (url, options) => {
      received = { url, options }
      return response({ code: 200, data: null })
    },
  })

  await scoreboardApi.clearMatchRecords(12, { expectedVersion: 5 })

  assert.equal(received.url, '/api/scoreboard/matches/12/records/clear')
  assert.equal(received.options.method, 'POST')
  assert.equal(received.options.body, JSON.stringify({ expectedVersion: 5 }))
})

test('业务失败抛出包含状态码和业务码的ApiError', async () => {
  configureRequest({
    fetchImpl: async () => response({ code: 1006, message: '状态版本已经变化' }, 400),
  })
  await assert.rejects(() => get('/scoreboard/matches/1'), error => {
    assert.equal(error instanceof ApiError, true)
    assert.equal(error.status, 400)
    assert.equal(error.code, 1006)
    assert.equal(error.message, '状态版本已经变化')
    return true
  })
})

function response(payload, status = 200) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}
