<script setup>
import { reactive } from 'vue'
import BaseDialog from '../common/BaseDialog.vue'
import { useScoreboardForm } from '../../composables/useScoreboardForm.js'
import { createFinalScoreDraft } from './finalScoreDialog.js'

const props = defineProps({ match: { type: Object, required: true } })
const emit = defineEmits(['close'])
const { errorElement, errorMessage, submitForm } = useScoreboardForm()
const draft = reactive(createFinalScoreDraft(props.match))
</script>

<template>
  <BaseDialog
    open
    title="赛后更正比分"
    form-id="final-score-form"
    confirm-label="保存比分，重新核对"
    @close="emit('close')"
  >
    <form id="final-score-form" @submit.prevent="submitForm('final-score', { ...draft }, $event)">
      <p>
        原比分 <strong>{{ match.scores.home }} : {{ match.scores.away }}</strong>。填写裁判核定的总比分和依据，保存后重新核对比赛结束状态。时钟保持停止，原进球明细保留，不会自动增删或改队；涉及漏记请同时核对进球明细。
      </p>
      <div class="form-grid">
        <label>主队 · {{ match.config.home }}比分<input v-model="draft.home" name="home" type="number" min="0" max="999" step="1" required></label>
        <label>客队 · {{ match.config.away }}比分<input v-model="draft.away" name="away" type="number" min="0" max="999" step="1" required></label>
      </div>
      <label>更正依据（必填）<input v-model="draft.reason" name="reason" required maxlength="200" placeholder="例如：裁判核实客队被多记一分"></label>
      <p>若更正后的比分与有效金球不一致，必须先核对金球，不能直接复赛；其他平局按本场规则处理。保存不会自动开始计时。</p>
      <p v-if="errorMessage" ref="errorElement" class="form-error error" role="alert" tabindex="-1">{{ errorMessage }}</p>
    </form>
  </BaseDialog>
</template>
