const SUCCESS_CODE = 200
const DEFAULT_TIMEOUT = 15000
export const TOKEN_STORAGE_KEY = 'lacrosse-access-token'

const runtime = {
  baseURL: normalizeBaseURL(import.meta.env?.VITE_API_BASE_URL || '/api'),
  fetchImpl: (...args) => globalThis.fetch(...args),
  getToken: readToken,
  onUnauthorized: notifyUnauthorized,
}

export class ApiError extends Error {
  constructor(message, options = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = options.status ?? 0
    this.code = options.code
    this.data = options.data
    this.cause = options.cause
  }
}

export function configureRequest(options = {}) {
  if (options.baseURL !== undefined) runtime.baseURL = normalizeBaseURL(options.baseURL)
  if (options.fetchImpl) runtime.fetchImpl = options.fetchImpl
  if (options.getToken) runtime.getToken = options.getToken
  if (options.onUnauthorized) runtime.onUnauthorized = options.onUnauthorized
}

export function setAccessToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_STORAGE_KEY, token)
    else localStorage.removeItem(TOKEN_STORAGE_KEY)
  } catch {}
}

export function removeAccessToken() {
  setAccessToken(null)
}

export async function request(path, options = {}) {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), options.timeout ?? DEFAULT_TIMEOUT)
  const url = buildURL(path, options.params)
  const headers = new Headers(options.headers || {})
  const token = runtime.getToken?.()
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', token.startsWith('Bearer ') ? token : `Bearer ${token}`)
  }

  let body = options.body
  if (body !== undefined && body !== null && !(body instanceof FormData)) {
    headers.set('Content-Type', 'application/json;charset=UTF-8')
    body = JSON.stringify(body)
  }

  try {
    const response = await runtime.fetchImpl(url, {
      method: options.method || 'GET',
      headers,
      body,
      credentials: options.credentials || 'same-origin',
      signal: controller.signal,
    })
    const payload = await readPayload(response)
    if (!response.ok || (payload && payload.code !== undefined && payload.code !== SUCCESS_CODE)) {
      const error = new ApiError(payload?.message || `请求失败（${response.status}）`, {
        status: response.status,
        code: payload?.code,
        data: payload?.data,
      })
      if (response.status === 401 || payload?.code === 401) runtime.onUnauthorized?.(error)
      throw error
    }
    return payload?.code === SUCCESS_CODE ? payload.data : payload
  } catch (error) {
    if (error instanceof ApiError) throw error
    if (error?.name === 'AbortError') {
      throw new ApiError('请求超时，请检查网络后重试', { cause: error })
    }
    throw new ApiError('网络连接失败，请稍后重试', { cause: error })
  } finally {
    clearTimeout(timeout)
  }
}

export const get = (path, params, options = {}) => request(path, { ...options, params })
export const post = (path, body, options = {}) => request(path, { ...options, method: 'POST', body })
export const put = (path, body, options = {}) => request(path, { ...options, method: 'PUT', body })
export const del = (path, body, options = {}) => request(path, { ...options, method: 'DELETE', body })

function buildURL(path, params) {
  const target = /^https?:\/\//i.test(path) ? path : `${runtime.baseURL}${path.startsWith('/') ? path : `/${path}`}`
  if (!params) return target
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    if (Array.isArray(value)) value.forEach(item => search.append(key, item))
    else search.set(key, value)
  })
  const query = search.toString()
  return query ? `${target}${target.includes('?') ? '&' : '?'}${query}` : target
}

async function readPayload(response) {
  if (response.status === 204) return null
  const text = await response.text()
  if (!text) return null
  try { return JSON.parse(text) } catch {
    throw new ApiError('服务器返回了无法识别的数据', { status: response.status, data: text })
  }
}

function normalizeBaseURL(value) {
  return String(value || '').replace(/\/$/, '')
}

function readToken() {
  try { return localStorage.getItem(TOKEN_STORAGE_KEY) || '' } catch { return '' }
}

function notifyUnauthorized(error) {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('lacrosse:unauthorized', { detail: error }))
  }
}
