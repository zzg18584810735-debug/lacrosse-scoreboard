<script setup>
import { computed, reactive, watch } from 'vue'
import { clock, isOvertime, periodName } from '../../core/timing.js'
import BaseDialog from '../common/BaseDialog.vue'
import { useScoreboardForm } from '../../composables/useScoreboardForm.js'
import {
  activeCorrectionPenalties,
  correctionLabels,
  correctionPenaltyLabel,
  correctionPenaltySeconds,
  correctionReasonPlaceholders,
  correctionScopes,
  createMatchCorrectionDraft,
} from './matchCorrectionDialog.js'

const props = defineProps({ match: { type: Object, required: true } })
const emit = defineEmits(['close'])
const { errorElement, errorMessage, submitForm } = useScoreboardForm()
const draft = reactive(createMatchCorrectionDraft(props.match))
const scopes = computed(() => correctionScopes(props.match))
const penalties = computed(() => activeCorrectionPenalties(props.match))
const noPenalty = computed(() => draft.scope === 'penalty' && penalties.value.length === 0)
const reasonPlaceholder = computed(() => correctionReasonPlaceholders[draft.scope])
const confirmLabel = computed(() => `保存${correctionLabels[draft.scope]}，保持停表`)
const penaltyHelp = computed(() => {
  if (draft.penaltyId) return `当前剩余 ${draft.penaltySeconds} 秒；只改剩余时间，不改牌型、球员和就座状态。`
  return penalties.value.length
    ? '请先选择一条罚时，将自动带入该球员当前剩余秒数。'
    : '当前没有尚未释放的罚时。新增判罚请点击球队色牌；修改或作废请到“比赛记录”。'
})

watch(() => draft.penaltyId, id => {
  draft.penaltySeconds = correctionPenaltySeconds(props.match, id)
})
</script>

<template>
  <BaseDialog open title="比赛更正" @close="emit('close')">
    <form id="correct-form" @submit.prevent="submitForm('correct', { ...draft }, $event)">
      <p class="correction-stop">已暂停所有时钟，取消后也保持停表。只修改所选项目，保存后请核对并按裁判指令继续。</p>
      <p v-if="match.phase === 'break'">当前在节间休息；下一节的比赛时间和进攻时间，请准备下一节后更正。</p>
      <label>更正项目<select v-model="draft.scope" name="scope">
        <option v-for="scope in scopes" :key="scope.value" :value="scope.value">{{ scope.label }}</option>
      </select></label>

      <fieldset v-if="draft.scope === 'main'" class="general-correction-fields">
        <legend>比赛剩余时间 · {{ periodName(match) }}</legend>
        <p>当前 {{ clock(match.main) }}；本节最多 {{ clock(isOvertime(match) ? match.rules.overtime : match.rules.period) }}。{{ match.phase === 'timeout' ? '若需改为 0 分 0 秒，请先结束球队暂停，再更正并核对本节结束。' : '填 0 分 0 秒将进入本节结束核对。' }}</p>
        <div class="form-grid">
          <label>剩余分钟<input v-model="draft.minutes" type="number" name="minutes" min="0" step="1" :max="draft.maxMinutes" required></label>
          <label>剩余秒数<input v-model="draft.seconds" type="number" name="seconds" min="0" max="59.999" step="0.001" required></label>
        </div>
      </fieldset>

      <fieldset v-else-if="draft.scope === 'shot'" class="general-correction-fields">
        <legend>复赛进攻时间</legend>
        <p>{{ match.resetOnResume ? '原计划复赛时重置，已填入重置值。' : '当前复赛进攻时间如下。' }}保存后使用所填秒数。设为 0 时须先按裁判指令处理，才能复赛。</p>
        <label>剩余进攻秒数<input v-model="draft.shot" type="number" name="shot" min="0" :max="match.rules.shot / 1000" step="0.001" required></label>
      </fieldset>

      <fieldset v-else-if="draft.scope === 'score'" class="general-correction-fields">
        <legend>双方总比分</legend>
        <p>当前 {{ match.scores.home }} : {{ match.scores.away }}。只改总分，原进球明细保留；漏记请用“补录进球”。若比分与有效金球不一致，须先核对金球。</p>
        <div class="form-grid">
          <label>主队 · {{ match.config.home }}比分<input v-model="draft.home" type="number" name="home" min="0" max="999" step="1" required></label>
          <label>客队 · {{ match.config.away }}比分<input v-model="draft.away" type="number" name="away" min="0" max="999" step="1" required></label>
        </div>
      </fieldset>

      <fieldset v-else class="general-correction-fields">
        <legend>单条剩余罚时</legend>
        <p id="penalty-current-help">{{ penaltyHelp }}</p>
        <label>选择球队与服罚球员<select v-model="draft.penaltyId" name="penaltyId" required>
          <option value="">请选择一条罚时</option>
          <option v-for="penalty in penalties" :key="penalty.id" :value="penalty.id">{{ correctionPenaltyLabel(match, penalty) }}</option>
        </select></label>
        <label>新的剩余罚时（秒）<input v-model="draft.penaltySeconds" type="number" name="penaltySeconds" min="0" max="600" step="0.001" required></label>
        <p>填 0 表示罚时已到，仍需点击“确认释放”。记错球队、球员或牌型，请到“比赛记录”更正／作废判罚。</p>
      </fieldset>

      <label>更正依据（必填）<input v-model="draft.reason" name="reason" required maxlength="200" :placeholder="reasonPlaceholder"></label>
      <p v-if="errorMessage" ref="errorElement" class="form-error error" role="alert" tabindex="-1">{{ errorMessage }}</p>
    </form>
    <template #footer>
      <button type="button" @click="emit('close')">取消</button>
      <button type="submit" class="primary" form="correct-form" :disabled="noPenalty" :aria-describedby="noPenalty ? 'penalty-current-help' : undefined">{{ confirmLabel }}</button>
    </template>
  </BaseDialog>
</template>
