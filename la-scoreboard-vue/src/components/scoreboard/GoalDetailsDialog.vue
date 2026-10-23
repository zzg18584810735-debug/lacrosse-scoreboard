<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import BaseDialog from '../common/BaseDialog.vue'
import { useScoreboardForm } from '../../composables/useScoreboardForm.js'
import { findGoalRecord } from './goalDialog.js'

const props = defineProps({
  match: { type: Object, required: true },
  goalRecordId: { type: Number, required: true },
})
const emit = defineEmits(['close'])
const { errorElement, errorMessage, submitForm } = useScoreboardForm()
const draft = reactive({ player: '', assist: '' })
const playerInput = ref(null)
const goal = computed(() => findGoalRecord(props.match, props.goalRecordId))
const heading = computed(() => goal.value
  ? `${goal.value.team === 'home' ? '主队' : '客队'} · ${props.match.config[goal.value.team]}进球记录`
  : '进球记录')

watch(
  () => props.goalRecordId,
  async () => {
    draft.player = goal.value?.player || ''
    draft.assist = goal.value?.assist || ''
    await nextTick()
    playerInput.value?.focus()
  },
  { immediate: true },
)
</script>

<template>
  <BaseDialog open title="进球球员与助攻" form-id="goal-details-form" confirm-label="保存球员信息" @close="emit('close')">
    <form id="goal-details-form" @submit.prevent="submitForm('goal-details', { ...draft }, $event)">
      <p>{{ heading }} · {{ goal?.period }} {{ goal?.time }}<br>该进球已计分。修改球员信息不改变比分或计时。</p>
      <div class="form-grid">
        <label>进球球员（必填）<input ref="playerInput" v-model="draft.player" name="player" required maxlength="20" placeholder="球员号码或姓名"></label>
        <label>助攻球员（选填）<input v-model="draft.assist" name="assist" maxlength="20" placeholder="球员号码或姓名"></label>
      </div>
      <p v-if="errorMessage" ref="errorElement" class="form-error error" role="alert" tabindex="-1">{{ errorMessage }}</p>
    </form>
  </BaseDialog>
</template>
