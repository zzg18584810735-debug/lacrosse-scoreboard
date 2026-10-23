export function downloadFile(content, filename, type = 'application/json', browser = {}) {
  const documentApi = browser.document || document
  const urlApi = browser.URL || URL
  const BlobApi = browser.Blob || Blob
  const schedule = browser.setTimeout || setTimeout
  const url = urlApi.createObjectURL(new BlobApi([content], { type }))
  const link = documentApi.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  schedule(() => urlApi.revokeObjectURL(url), 1000)
}

export async function toggleFullscreen(documentApi = document) {
  try {
    if (documentApi.fullscreenElement) {
      await documentApi.exitFullscreen()
      return
    }
    if (!documentApi.documentElement?.requestFullscreen) throw new Error()
    await documentApi.documentElement.requestFullscreen()
  } catch {
    throw new Error('当前浏览器不支持全屏，请使用浏览器窗口最大化。')
  }
}

export function matchIdFromSearch(search = globalThis.location?.search || '') {
  return new URLSearchParams(search).get('matchId')
}

export function replaceMatchIdInSearch(matchId, browser = globalThis) {
  const url = new URL(browser.location.href)
  url.searchParams.set('matchId', String(matchId))
  browser.history.replaceState(browser.history.state, '', url)
}
