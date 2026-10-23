import { inject, nextTick, ref } from 'vue'

export const scoreboardFormServiceKey = Symbol('scoreboardFormService')

export function revealInvalidSection(event) {
  const section = event.target.closest('details')
  if (section) section.open = true
}

export function useScoreboardForm() {
  const service = inject(scoreboardFormServiceKey)
  if (!service) throw new Error('计分板表单服务尚未注册。')

  const errorMessage = ref('')
  const errorElement = ref(null)

  async function submitForm(type, payload, event) {
    errorMessage.value = ''
    try {
      const values = typeof payload === 'function' ? payload() : payload
      await service.submit(type, values, { submitterValue: event?.submitter?.value })
    } catch (error) {
      errorMessage.value = error.message
      nextTick(() => errorElement.value?.focus())
    }
  }

  return {
    errorElement,
    errorMessage,
    savePendingGoal: service.savePendingGoal,
    submitForm,
  }
}
