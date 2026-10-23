import { post, removeAccessToken, setAccessToken } from './request.js'

export async function login(credentials) {
  const data = await post('/auth/login', credentials)
  setAccessToken(data.token)
  return data
}

export async function logout() {
  try {
    await post('/auth/logout')
  } finally {
    removeAccessToken()
  }
}
