<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { clock, periodName } from '../../core/timing.js'
import BaseDialog from '../common/BaseDialog.vue'
import { useScoreboardForm } from '../../composables/useScoreboardForm.js'
import { createGoalDialogDraft, goalDialogDraftKey, otherGoalTeam } from './goalDialog.js'

const props = defineProps({
  match: { type: Object, required: true },
  mode: { type: String, required: true },
  initialTeam: { type: String, default: 'home' },
})
const emit = defineEmits(['close', 'team-change'])
const { errorElement, errorMessage, savePendingGoal, submitForm } = useScoreboardForm()
const draft = reactive(createGoalDialogDraft(props.match, props.mode, props.initialTeam))
const playerInput = ref(null)
const backfill = computed(() => props.mode === 'backfill')
const blocked = computed(() => !backfill.value && props.match.goldenConflict)
const formId = computed(() => backfill.value ? 'goal-backfill-form' : 'goal-entry-form')
const teamLabel = computed(() => draft.team === 'home' ? '主队' : '客队')
const title = computed(() => `${teamLabel.value} · ${props.match.config[draft.team]}${backfill.value ? '补录进球' : '进球记录'}`)

watch(
  () => goalDialogDraftKey(props.match, props.mode),
  async () => {
    Object.assign(draft, createGoalDialogDraft(props.match, props.mode, props.initialTeam))
    await nextTick()
    playerInput.value?.focus()
  },
  { immediate: true },
)

function switchTeam() {
  draft.team = otherGoalTeam(draft.team)
  emit('team-change', draft.team)
}

function saveDraft() {
  if (!backfill.value) savePendingGoal({ player: draft.player, assist: draft.assist, team: draft.team })
}
</script>

<template>
  <BaseDialog open :title="title" :form-id="formId" @close="emit('close')">
    <form
      :id="formId"
      @input="saveDraft"
      @submit.prevent="submitForm(backfill ? 'goal-backfill' : 'goal-entry', { ...draft }, $event)"
    >
      <input v-model="draft.team" type="hidden" name="team">
      <input v-if="!backfill" :value="draft.goalId" type="hidden" name="goalId">
      <p v-if="backfill">
        已暂停计时。只可选择已开赛节次；下一节未开赛也能补录上一节。补录不会倒拨时钟或改变球权；金球记录冲突时须先核对。取消后也保持停表。
      </p>
      <p v-else>
        发生时间：{{ periodName({ ...match, period: match.pendingGoal?.period }) }} · {{ clock(draft.main) }}。
        <template v-if="blocked">本草稿尚未计分，保持停表。</template>
        <template v-else>确认后加 {{ match.rules.points }} 分。比赛钟按死球规则{{ match.running ? '继续' : '暂停' }}；取消不加分，并恢复打开弹窗前的场上状态。</template>
      </p>
      <p v-if="blocked" class="correction-stop">
        本草稿尚未计分。已有金球记录冲突，请先取消本草稿，再核对金球；已填写内容仍保留在表单中。
      </p>
      <label>进球球员（必填）<input ref="playerInput" v-model="draft.player" name="player" required maxlength="20" placeholder="输入球员号码或姓名" autocomplete="off"></label>
      <label>助攻球员（选填）<input v-model="draft.assist" name="assist" maxlength="20" placeholder="球员号码或姓名" autocomplete="off"></label>
      <template v-if="backfill">
        <label>进球节次<select v-model="draft.period" name="period">
          <option v-for="period in draft.latestPeriod" :key="period" :value="period">{{ periodName({ ...match, period }) }}</option>
        </select></label>
        <div class="form-grid">
          <label>进球时比赛剩余 · 分<input v-model="draft.minutes" name="minutes" type="number" min="0" max="60" step="1" required></label>
          <label>秒<input v-model="draft.seconds" name="seconds" type="number" min="0" max="59" step="1" required></label>
        </div>
        <label>比分处理<select v-model="draft.scoreMode" name="scoreMode">
          <option value="add">比分也漏了：补加 {{ match.rules.points }} 分</option>
          <option value="included">比分已计入：只补进球明细</option>
        </select></label>
        <label>补录依据（必填）<input v-model="draft.reason" name="reason" required maxlength="200" placeholder="例如：裁判确认上半场 06:20 的进球漏记"></label>
      </template>
      <p v-if="errorMessage" ref="errorElement" class="form-error error" role="alert" tabindex="-1">{{ errorMessage }}</p>
    </form>
    <template #footer>
      <button type="button" @click="emit('close')">{{ blocked ? '取消本草稿，前往金球核对' : '取消' }}</button>
      <button v-if="!blocked" type="button" @click="switchTeam">对方进球</button>
      <button type="submit" class="primary" :form="formId" :disabled="blocked">确认记录</button>
    </template>
  </BaseDialog>
</template>
