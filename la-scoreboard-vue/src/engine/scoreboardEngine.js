import { createMatch } from '../core/matchModel.js'
import { restoreMatch } from '../core/matchRecovery.js'
import { act } from '../core/scoreboardActions.js'
import { advance, freeze } from '../core/timing.js'
import { toMatchStateSaveRequest } from '../adapters/matchDetailAdapter.js'

/**
 * 连接纯计分规则、Vue 状态仓库和浏览器缓存的运行引擎。
 */
export class ScoreboardEngine {
  constructor(stateStore, persistence, uiHooks = {}) {
    this.stateStore = stateStore
    this.persistence = persistence
    this.uiHooks = uiHooks
    this.real = createMatch({ preset: 'sll-u8' })
    const restoredMatch = this.persistence.load(restoreMatch)
    if (restoredMatch) this.real = restoredMatch
    this.last = 0
    this.savedAt = 0
    this.syncQueue = Promise.resolve()
  }

  get real() {
    return this.stateStore?.get() ?? this._real
  }

  set real(value) {
    this._real = value
    this.stateStore?.replace(value)
  }

  get state() {
    return this.real
  }

  init() {
    this.render()
    this.last = performance.now()
  }

  handleExternalStorageChange() {
    if (this.real) freeze(this.real)
    this.persistence.blockExternal()
    this.render()
  }

  settle() {
    const now = performance.now()
    const before = this.state.sequence
    let changed = advance(this.state, now - this.last)
    if (this.state.phase === 'timeout'
      && this.state.auxiliary === 0
      && this.state.timeoutContext) {
      act(this.state, 'end-timeout')
      changed = true
    }
    this.last = now
    if (changed) this.render()
    if (this.state.sequence !== before) {
      this.uiHooks.onBeep?.()
      this.syncRemote()
    }
    if (now - this.savedAt >= 1000) {
      this.save()
      this.savedAt = now
    }
  }

  save() {
    this.stateStore?.notify(this.real)
    this.persistence.save(this.real)
  }

  dispatch(type, params = {}) {
    act(this.state, type, params)
    this.last = performance.now()
    this.save()
    this.render()
    this.syncRemote()
  }

  commit() {
    this.last = performance.now()
    this.save()
    this.render()
    return this.syncRemote()
  }

  render() {
    this.stateStore?.notify(this.real)
  }

  syncRemote() {
    if (!this.stateStore?.remoteMatchId || typeof this.stateStore.syncMatchState !== 'function') {
      return Promise.resolve(null)
    }
    this.syncQueue = this.syncQueue
      .catch(() => undefined)
      .then(() => this.stateStore.syncMatchState(
        toMatchStateSaveRequest(this.state, this.stateStore.remoteDetail),
      ))
    // 同步错误已经由 Pinia 转换为离线、冲突或错误状态，避免未处理的 Promise 拒绝。
    this.syncQueue.catch(() => undefined)
    return this.syncQueue
  }

  waitForSync() {
    return this.syncQueue.catch(() => undefined)
  }
}
