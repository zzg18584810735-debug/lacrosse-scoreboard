import assert from 'node:assert/strict'
import test from 'node:test'
import { normalizeTheme } from './useTheme.js'

test('主题只接受系统、日间和夜间三种模式', () => {
  assert.equal(normalizeTheme('light'), 'light')
  assert.equal(normalizeTheme('dark'), 'dark')
  assert.equal(normalizeTheme('system'), 'system')
  assert.equal(normalizeTheme('unknown'), 'system')
  assert.equal(normalizeTheme(null), 'system')
})
