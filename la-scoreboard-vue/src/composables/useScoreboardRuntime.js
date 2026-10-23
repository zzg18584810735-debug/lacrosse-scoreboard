import { MATCH_STORE_KEY } from '../storage/matchStorage.js'

export function createScoreboardRuntime(app, options = {}) {
  const windowApi = options.windowApi || window
  const documentApi = options.documentApi || document
  const startInterval = options.setIntervalApi || setInterval
  const stopInterval = options.clearIntervalApi || clearInterval
  let interval
  let audio

  const handleStorage = event => {
    if (event.key === MATCH_STORE_KEY) app.handleExternalStorageChange()
  }
  const settleAndSave = () => {
    app.settle()
    app.save()
  }

  function start() {
    if (interval !== undefined) return
    interval = startInterval(() => app.settle(), 100)
    windowApi.addEventListener('storage', handleStorage)
    documentApi.addEventListener('visibilitychange', settleAndSave)
    windowApi.addEventListener('pagehide', settleAndSave)
  }

  function stop() {
    if (interval === undefined) return
    stopInterval(interval)
    interval = undefined
    windowApi.removeEventListener('storage', handleStorage)
    documentApi.removeEventListener('visibilitychange', settleAndSave)
    windowApi.removeEventListener('pagehide', settleAndSave)
  }

  async function enableAudio() {
    try {
      const AudioContextApi = windowApi.AudioContext || windowApi.webkitAudioContext
      if (!AudioContextApi) return
      audio ||= new AudioContextApi()
      await audio.resume()
    } catch {
      // 页面文字提示始终可用，音频初始化失败不影响比赛操作。
    }
  }

  function beep() {
    if (!audio) return
    const oscillator = audio.createOscillator()
    const gain = audio.createGain()
    oscillator.connect(gain)
    gain.connect(audio.destination)
    gain.gain.value = 0.08
    oscillator.frequency.value = 660
    oscillator.start()
    oscillator.stop(audio.currentTime + 0.3)
  }

  return { beep, enableAudio, start, stop }
}

/**
 * 立即初始化计分引擎并启动轮询，不能等待字典或远程比赛请求完成。
 */
export function startScoreboardRuntime(app, options = {}) {
  app.init()
  const runtime = createScoreboardRuntime(app, options)
  runtime.start()
  return runtime
}
