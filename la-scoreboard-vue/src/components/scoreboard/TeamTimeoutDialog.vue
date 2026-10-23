<script setup>
import { computed } from 'vue'
import { isYouth } from '../../core/rules.js'
import { clock } from '../../core/timing.js'
import BaseDialog from '../common/BaseDialog.vue'
import { useScoreboardForm } from '../../composables/useScoreboardForm.js'
import { timeoutDialogContext } from './timeoutDialog.js'

const props = defineProps({
  match: { type: Object, required: true },
  team: { type: String, required: true },
})
const emit = defineEmits(['close'])
const { errorElement, errorMessage, submitForm } = useScoreboardForm()
const context = computed(() => timeoutDialogContext(props.match, props.team))
const possessionText = computed(() => props.match.possession
  ? `${props.match.config[props.match.possession]}有球`
  : '球权未记录')
</script>

<template>
  <BaseDialog open title="确认球队暂停" @close="emit('close')">
    <form id="timeout-form" @submit.prevent="submitForm('timeout', {}, $event)">
      <p>
        <strong :class="`${team}-ink`">{{ context.teamName }}</strong>
        · 剩余 {{ match.timeouts[team] }} 次 · 每次 {{ match.rules.timeout / 1000 }} 秒
      </p>
      <p class="timeout-clock">
        比赛钟 <strong>{{ clock(match.main) }}</strong> · {{ match.running ? '仍在计时' : '已停表' }}<br>
        确认后立即停表、扣除 1 次并开始暂停倒计时；取消不改变比赛。
      </p>
      <p>
        当前记录：{{ match.ball === 'live' ? '活球' : '死球' }} · {{ possessionText }}{{ match.frontcourt ? ' · 已在前场' : '' }}。<br>
        请按裁判判定确认，可在这里同步球权与前场状态。
      </p>
      <p><strong>{{ context.conditionPrompt }}</strong></p>
      <p class="source-note">
        依据：{{ isYouth(match) ? '中国 U12 2026 · 6.8.4–6.8.5' : '六人制 2026–2028 · 6.10.4–6.10.5' }}。{{ match.rules.shot ? '若同步了换队球权，进攻钟按换权规则重置；同队暂停保留原进攻时间。' : '' }}
      </p>
      <p v-if="errorMessage" ref="errorElement" class="form-error error" role="alert" tabindex="-1">{{ errorMessage }}</p>
    </form>
    <template #footer>
      <button type="button" class="timeout-cancel" @click="emit('close')">取消，返回比赛</button>
      <button
        v-for="condition in context.conditions"
        :key="condition.value"
        type="submit"
        name="condition"
        :value="condition.value"
        form="timeout-form"
        class="timeout-confirm"
      >{{ condition.label }}</button>
    </template>
  </BaseDialog>
</template>
