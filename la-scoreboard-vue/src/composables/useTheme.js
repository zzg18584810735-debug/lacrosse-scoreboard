import { onBeforeUnmount, onMounted, ref } from 'vue'

const THEME_KEY = 'lacrosse-theme'

export function useTheme() {
  const systemTheme = window.matchMedia('(prefers-color-scheme: dark)')
  const themeMode = ref(readTheme())

  const applyTheme = () => {
    document.documentElement.dataset.theme = themeMode.value === 'system'
      ? (systemTheme.matches ? 'dark' : 'light')
      : themeMode.value
    document.documentElement.dataset.themeMode = themeMode.value
  }

  const onSystemChange = () => {
    if (themeMode.value === 'system') applyTheme()
  }
  const onStorage = event => {
    if (event.key !== THEME_KEY) return
    themeMode.value = normalizeTheme(event.newValue)
    applyTheme()
  }

  const setTheme = value => {
    themeMode.value = normalizeTheme(value)
    applyTheme()
    try { localStorage.setItem(THEME_KEY, themeMode.value) } catch {}
  }

  onMounted(() => {
    applyTheme()
    systemTheme.addEventListener('change', onSystemChange)
    window.addEventListener('storage', onStorage)
  })

  onBeforeUnmount(() => {
    systemTheme.removeEventListener('change', onSystemChange)
    window.removeEventListener('storage', onStorage)
  })

  return { themeMode, setTheme }
}

function readTheme() {
  try { return normalizeTheme(localStorage.getItem(THEME_KEY)) } catch { return 'system' }
}

export function normalizeTheme(value) {
  return value === 'light' || value === 'dark' ? value : 'system'
}
