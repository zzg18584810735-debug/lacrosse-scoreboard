import assert from 'node:assert/strict'
import test from 'node:test'
import { downloadFile, matchIdFromSearch, replaceMatchIdInSearch, toggleFullscreen } from './browser.js'

test('下载工具创建临时链接并释放对象地址', () => {
  let clicked = false
  let revoked = ''
  const link = { click: () => { clicked = true } }
  const browser = {
    document: { createElement: tag => tag === 'a' ? link : null },
    URL: {
      createObjectURL: () => 'blob:test',
      revokeObjectURL: url => { revoked = url },
    },
    Blob: class { constructor(parts, options) { this.parts = parts; this.options = options } },
    setTimeout: callback => callback(),
  }

  downloadFile('内容', '记录.csv', 'text/csv', browser)

  assert.equal(link.href, 'blob:test')
  assert.equal(link.download, '记录.csv')
  assert.equal(clicked, true)
  assert.equal(revoked, 'blob:test')
})

test('全屏工具可以进入和退出全屏', async () => {
  let entered = false
  let exited = false
  const documentApi = {
    fullscreenElement: null,
    documentElement: { requestFullscreen: async () => { entered = true } },
    exitFullscreen: async () => { exited = true },
  }

  await toggleFullscreen(documentApi)
  documentApi.fullscreenElement = {}
  await toggleFullscreen(documentApi)

  assert.equal(entered, true)
  assert.equal(exited, true)
})

test('浏览器地址可以指定需要加载的服务器比赛', () => {
  assert.equal(matchIdFromSearch('?matchId=18&view=desk'), '18')
  assert.equal(matchIdFromSearch('?view=desk'), null)
})

test('创建服务器比赛后将比赛 ID 写回当前地址', () => {
  let replaced = ''
  replaceMatchIdInSearch(19, {
    location: { href: 'http://localhost:5173/?view=desk' },
    history: {
      state: { current: true },
      replaceState: (_state, _title, url) => { replaced = String(url) },
    },
  })

  assert.equal(replaced, 'http://localhost:5173/?view=desk&matchId=19')
})
