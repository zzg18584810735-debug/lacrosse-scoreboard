import assert from 'node:assert/strict'
import test from 'node:test'
import { MATCH_STORE_KEY } from '../storage/matchStorage.js'
import { createScoreboardRuntime, startScoreboardRuntime } from './useScoreboardRuntime.js'

function createEventTarget() {
  const listeners = new Map()
  return {
    addEventListener(type, listener) { listeners.set(type, listener) },
    removeEventListener(type, listener) {
      if (listeners.get(type) === listener) listeners.delete(type)
    },
    fire(type, event = {}) { listeners.get(type)?.(event) },
    has(type) { return listeners.has(type) },
  }
}

test('计分板运行时统一管理轮询和页面生命周期', () => {
  const windowApi = createEventTarget()
  const documentApi = createEventTarget()
  let intervalCallback
  let cleared
  const calls = { settle: 0, save: 0, external: 0 }
  const app = {
    settle: () => { calls.settle += 1 },
    save: () => { calls.save += 1 },
    handleExternalStorageChange: () => { calls.external += 1 },
  }
  const runtime = createScoreboardRuntime(app, {
    windowApi,
    documentApi,
    setIntervalApi: callback => { intervalCallback = callback; return 7 },
    clearIntervalApi: id => { cleared = id },
  })

  runtime.start()
  intervalCallback()
  windowApi.fire('storage', { key: 'other' })
  windowApi.fire('storage', { key: MATCH_STORE_KEY })
  documentApi.fire('visibilitychange')
  windowApi.fire('pagehide')
  runtime.stop()

  assert.deepEqual(calls, { settle: 3, save: 2, external: 1 })
  assert.equal(cleared, 7)
  assert.equal(windowApi.has('storage'), false)
  assert.equal(documentApi.has('visibilitychange'), false)
})

test('页面启动时立即初始化引擎并注册计时轮询', () => {
  const windowApi = createEventTarget()
  const documentApi = createEventTarget()
  let initialized = false
  let intervalCallback
  const app = {
    init: () => { initialized = true },
    settle() {},
    save() {},
    handleExternalStorageChange() {},
  }

  const runtime = startScoreboardRuntime(app, {
    windowApi,
    documentApi,
    setIntervalApi: callback => { intervalCallback = callback; return 8 },
    clearIntervalApi() {},
  })

  assert.equal(initialized, true)
  assert.equal(typeof intervalCallback, 'function')
  runtime.stop()
})

test('计分板运行时在用户授权后播放蜂鸣音', async () => {
  let started = false
  let stoppedAt = 0
  class AudioContextMock {
    constructor() { this.currentTime = 2; this.destination = {} }
    async resume() {}
    createOscillator() {
      return {
        connect() {},
        frequency: { value: 0 },
        start() { started = true },
        stop(value) { stoppedAt = value },
      }
    }
    createGain() { return { connect() {}, gain: { value: 0 } } }
  }
  const runtime = createScoreboardRuntime({}, {
    windowApi: { AudioContext: AudioContextMock },
    documentApi: {},
  })

  await runtime.enableAudio()
  runtime.beep()

  assert.equal(started, true)
  assert.equal(stoppedAt, 2.3)
})
