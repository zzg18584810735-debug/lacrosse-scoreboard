<script setup>
import { computed, reactive } from 'vue'
import BaseDialog from '../common/BaseDialog.vue'
import { useScoreboardForm } from '../../composables/useScoreboardForm.js'
import {
  createGoldenGoalReviewDraft,
  goldenGoalCandidateLabel,
  goldenGoalCandidates,
} from './goldenGoalReviewDialog.js'

const props = defineProps({ match: { type: Object, required: true } })
const emit = defineEmits(['close'])
const { errorElement, errorMessage, submitForm } = useScoreboardForm()
const draft = reactive(createGoldenGoalReviewDraft(props.match))
const candidates = computed(() => goldenGoalCandidates(props.match))
</script>

<template>
  <BaseDialog open title="加时金球进球核对" @close="emit('close')">
    <form id="resolve-golden-form" @submit.prevent="submitForm('resolve-golden', { ...draft }, $event)">
      <p class="correction-stop">
        这不是新增进球。它只在加时金球制出现多条进球记录，或比分与有效金球不一致时使用。
      </p>
      <p>
        按本场金球规则，加时的首个有效进球应结束比赛。请按裁判判定选择有效进球；其余加时进球会标记为无效但保留原记录。当前比分 <strong>{{ match.scores.home }} : {{ match.scores.away }}</strong> 仅供核对。
      </p>
      <label>裁判确认的首个有效进球（必选）<select v-model="draft.goalId" name="goalId" required>
        <option value="">请选择裁判判定</option>
        <option v-for="goal in candidates" :key="goal.id" :value="goal.id">{{ goldenGoalCandidateLabel(match, goal) }}</option>
        <option value="none">裁判确认所有加时进球均无效，恢复平局</option>
      </select></label>
      <div class="form-grid">
        <label>核定主队总分<input v-model="draft.home" name="home" type="number" min="0" max="999" step="1" required></label>
        <label>核定客队总分<input v-model="draft.away" name="away" type="number" min="0" max="999" step="1" required></label>
      </div>
      <label>裁判核对依据（必填）<input v-model="draft.reason" name="reason" required maxlength="200" placeholder="例如：客队先入有效球，其后主队进球不计"></label>
      <p>时钟保持停止且不倒拨。核对后请检查球权、时间和判罚；有效金球仍须确认结束，所有进球无效时须手动复赛。</p>
      <p v-if="errorMessage" ref="errorElement" class="form-error error" role="alert" tabindex="-1">{{ errorMessage }}</p>
    </form>
    <template #footer>
      <button type="button" @click="emit('close')">暂不处理，保持停表</button>
      <button type="submit" class="primary" form="resolve-golden-form">保存裁判核对结果</button>
    </template>
  </BaseDialog>
</template>
