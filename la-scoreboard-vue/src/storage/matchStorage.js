export const MATCH_STORE_KEY = 'lacrosse-match-v4'

const RETIRED_STORE_KEYS = ['lacrosse-app-state', 'lacrosse-sixes-v2']

export function prepareMatchStorage(storage) {
  for (const key of RETIRED_STORE_KEYS) storage.removeItem(key)
}

export function loadStoredMatch(storage) {
  const raw = storage.getItem(MATCH_STORE_KEY)
  return raw ? JSON.parse(raw) : null
}

export function saveStoredMatch(storage, match) {
  storage.setItem(MATCH_STORE_KEY, JSON.stringify(match))
}
