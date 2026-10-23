import { inject } from 'vue'

export const scoreboardActionsKey = Symbol('scoreboardActions')

export function useScoreboardActions() {
  const actions = inject(scoreboardActionsKey)
  if (!actions) throw new Error('计分板动作服务尚未注册。')
  return actions
}
