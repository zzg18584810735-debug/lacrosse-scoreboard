<script setup>
import { computed, reactive } from 'vue'
import BaseDialog from '../common/BaseDialog.vue'
import { useScoreboardForm } from '../../composables/useScoreboardForm.js'
import { penaltyKindOptions, requiresPenaltyServer } from './penaltyDialog.js'
import { createRecordCorrectionDraft, findCorrectionEvent } from './recordCorrectionDialog.js'

const props = defineProps({
  match: { type: Object, required: true },
  type: { type: String, required: true },
  correctionId: { type: Number, required: true },
})
const emit = defineEmits(['close'])
const { errorElement, errorMessage, submitForm } = useScoreboardForm()
const penalty = computed(() => props.type === 'penalty')
const event = computed(() => findCorrectionEvent(props.match, props.type, props.correctionId))
const initialDraft = createRecordCorrectionDraft(props.match, props.type, props.correctionId)
const draft = reactive(initialDraft || { mode: 'edit', team: 'home', correctionReason: '' })
const editing = computed(() => draft.mode === 'edit')
const kindOptions = computed(() => penaltyKindOptions(props.match))
const serverRequired = computed(() => editing.value && penalty.value && requiresPenaltyServer(props.match, draft))
const eventTeamName = computed(() => event.value ? props.match.config[event.value.team] : '')
</script>

<template>
  <BaseDialog
    open
    :title="penalty ? '更正／作废判罚' : '更正／作废暂停'"
    form-id="record-correction-form"
    confirm-label="确认更正，保持停表"
    @close="emit('close')"
  >
    <form v-if="event" id="record-correction-form" @submit.prevent="submitForm('record-correction', { ...draft }, $event)">
      <p><strong>{{ event.period }} {{ event.time }} · {{ eventTeamName }}</strong><br>{{ event.text }}</p>
      <p>
        已暂停所有时钟，取消后也保持停表。更正保留原记录和依据，不回溯现场比分、比赛时钟或球权，请按裁判指令核对后复赛。{{ penalty
          ? '更换罚时球队／代罚者后须重新确认就座。'
          : '次数归还或转记在原半场／加时，历史更正不增加当前半场次数。' }}
      </p>
      <label>处理方式<select v-model="draft.mode" name="mode">
        <option value="edit">更正记录</option>
        <option value="cancel">作废误记{{ penalty ? '判罚（撤除该条罚时及累计）' : '暂停（归还所属半场次数）' }}</option>
      </select></label>

      <div class="correction-fields">
        <template v-if="penalty">
          <div class="form-grid">
            <label>犯规球队<select v-model="draft.team" name="team" :disabled="!editing">
              <option value="home">{{ match.config.home }}</option>
              <option value="away">{{ match.config.away }}</option>
            </select></label>
            <label>被记录者号码／姓名<input v-model="draft.player" name="player" required maxlength="12" :disabled="!editing" placeholder="例如 7"></label>
          </div>
          <label>犯规主体<select v-model="draft.subject" name="subject" :disabled="!editing">
            <option value="player">球员</option>
            <option value="goalie">守门员</option>
            <option value="official">球队官员（代罚不计球员重大犯规）</option>
          </select></label>
          <label>裁判确认的判罚<select v-model="draft.kind" name="kind" :disabled="!editing">
            <option v-for="option in kindOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select></label>
          <label>犯规定义／备注（选填）<input v-model="draft.reason" name="reason" maxlength="120" :disabled="!editing" placeholder="例如：球杆挥击、拖延比赛"></label>
          <label>
            代罚球员号码{{ serverRequired ? '（必填）' : '（守门员 / 红牌等情形填写）' }}
            <input v-model="draft.server" name="server" maxlength="12" :required="serverRequired" :disabled="!editing || draft.kind === 'minor-turnover'" placeholder="普通罚时留空表示本人；红牌必须填写">
          </label>
          <label v-if="draft.kind !== 'minor-turnover'">裁判确认的剩余罚时（秒）<input v-model="draft.remaining" name="remaining" type="number" min="0" max="600" step="0.001" required :disabled="!editing"></label>
          <p v-if="editing" class="correction-help">已执行的时间不会重算。改牌型时请同时核定剩余罚时；无牌不保留罚时。</p>
        </template>
        <label v-else>正确的暂停球队<select v-model="draft.team" name="team" :disabled="!editing">
          <option value="home">{{ match.config.home }}</option>
          <option value="away">{{ match.config.away }}</option>
        </select></label>
      </div>

      <label>更正依据（必填）<input v-model="draft.correctionReason" name="correctionReason" required maxlength="200" placeholder="例如：裁判确认记错球队／球员"></label>
      <p v-if="errorMessage" ref="errorElement" class="form-error error" role="alert" tabindex="-1">{{ errorMessage }}</p>
    </form>
    <p v-else class="error">该记录不存在、已经作废或不属于当前更正类型。</p>
    <template #footer>
      <button type="button" @click="emit('close')">{{ event ? '取消' : '关闭' }}</button>
      <button v-if="event" type="submit" class="primary" form="record-correction-form">确认更正，保持停表</button>
    </template>
  </BaseDialog>
</template>
