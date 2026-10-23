<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import BaseDialog from '../common/BaseDialog.vue'
import { useScoreboardForm } from '../../composables/useScoreboardForm.js'
import { createPenaltyDraft, penaltyKindOptions, requiresPenaltyServer } from './penaltyDialog.js'

const props = defineProps({
  match: { type: Object, required: true },
  defaults: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['close'])
const { errorElement, errorMessage, submitForm } = useScoreboardForm()
const draft = reactive(createPenaltyDraft(props.defaults))
const playerInput = ref(null)
const kindOptions = computed(() => penaltyKindOptions(props.match))
const serverRequired = computed(() => requiresPenaltyServer(props.match, draft))

onMounted(async () => {
  await nextTick()
  playerInput.value?.focus()
})
</script>

<template>
  <BaseDialog
    open
    title="记录已鸣哨的裁判判罚"
    form-id="penalty-form"
    confirm-label="确认判罚"
    @close="emit('close')"
  >
    <form id="penalty-form" @submit.prevent="submitForm('penalty', { ...draft }, $event)">
      <p>
        先以裁判报告为准。{{ match.rules.foulStop
          ? '本场所有犯规鸣哨均应停表；已暂停比赛钟与罚时，等待录入判罚。'
          : '打开表单不会自行停表；提交有罚时判罚后，所有比赛时钟停止。' }}
      </p>
      <div class="form-grid">
        <label>犯规球队<select v-model="draft.team" name="team">
          <option value="home">{{ match.config.home }}</option>
          <option value="away">{{ match.config.away }}</option>
        </select></label>
        <label>被记录者号码／姓名<input ref="playerInput" v-model="draft.player" name="player" required maxlength="12" placeholder="例如 7"></label>
      </div>
      <label>犯规主体<select v-model="draft.subject" name="subject">
        <option value="player">球员</option>
        <option value="goalie">守门员</option>
        <option value="official">球队官员（代罚不计球员重大犯规）</option>
      </select></label>
      <label>裁判确认的判罚<select v-model="draft.kind" name="kind">
        <option v-for="option in kindOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
      </select></label>
      <label>犯规定义／备注（选填）<input v-model="draft.reason" name="reason" maxlength="120" placeholder="例如：球杆挥击、拖延比赛"></label>
      <label>
        代罚球员号码{{ serverRequired ? '（必填）' : '（守门员 / 红牌等情形填写）' }}
        <input
          v-model="draft.server"
          name="server"
          maxlength="12"
          :required="serverRequired"
          :disabled="draft.kind === 'minor-turnover'"
          placeholder="普通罚时留空表示本人；红牌必须填写"
        >
      </label>
      <label>有罚时判罚后的球权<select v-model="draft.awarded" name="awarded" :disabled="draft.kind === 'minor-turnover'">
        <option value="opponent">判给未犯规一方</option>
        <option value="home">{{ match.config.home }}</option>
        <option value="away">{{ match.config.away }}</option>
        <option value="none">裁判尚未指定</option>
      </select></label>
      <p v-if="draft.kind === 'minor-turnover'" class="source-note">轻微犯规只记录换球权，不创建罚时；球权自动判给未犯规一方。</p>
      <p v-if="errorMessage" ref="errorElement" class="form-error error" role="alert" tabindex="-1">{{ errorMessage }}</p>
    </form>
  </BaseDialog>
</template>
