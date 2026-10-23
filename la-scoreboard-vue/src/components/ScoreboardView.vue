<script setup>
import { nextTick, onBeforeUnmount, onMounted, provide, ref } from 'vue'
import { ScoreboardEngine } from '../engine/scoreboardEngine.js'
import { createEmptyMatch } from '../core/matchModel.js'
import { actionAllowedWithoutMatch } from '../core/actionPolicy.js'
import { useScoreboardStore } from '../stores/scoreboardStore.js'
import { scoreboardActionsKey } from '../composables/useScoreboardActions.js'
import { scoreboardFormServiceKey } from '../composables/useScoreboardForm.js'
import { startScoreboardRuntime } from '../composables/useScoreboardRuntime.js'
import { createScoreboardController } from '../composables/useScoreboardController.js'
import { createScoreboardPersistence } from '../storage/scoreboardPersistence.js'
import { downloadFile, matchIdFromSearch, replaceMatchIdInSearch, toggleFullscreen } from '../utils/browser.js'
import MatchClock from './scoreboard/MatchClock.vue'
import MatchClockControls from './scoreboard/MatchClockControls.vue'
import TeamPanel from './scoreboard/TeamPanel.vue'
import FieldControls from './scoreboard/FieldControls.vue'
import PenaltyList from './scoreboard/PenaltyList.vue'
import MatchRecords from './scoreboard/MatchRecords.vue'
import MatchHeader from './scoreboard/MatchHeader.vue'
import MatchNavigation from './scoreboard/MatchNavigation.vue'
import MatchRules from './scoreboard/MatchRules.vue'
import MatchSetupDialog from './scoreboard/MatchSetupDialog.vue'
import MatchInfoDialog from './scoreboard/MatchInfoDialog.vue'
import MatchExportDialog from './scoreboard/MatchExportDialog.vue'
import GoalEntryDialog from './scoreboard/GoalEntryDialog.vue'
import GoalDetailsDialog from './scoreboard/GoalDetailsDialog.vue'
import TeamTimeoutDialog from './scoreboard/TeamTimeoutDialog.vue'
import PenaltyDialog from './scoreboard/PenaltyDialog.vue'
import RecordCorrectionDialog from './scoreboard/RecordCorrectionDialog.vue'
import MatchCorrectionDialog from './scoreboard/MatchCorrectionDialog.vue'
import FinalScoreDialog from './scoreboard/FinalScoreDialog.vue'
import GoldenGoalReviewDialog from './scoreboard/GoldenGoalReviewDialog.vue'
import ClearMatchRecordsDialog from './scoreboard/ClearMatchRecordsDialog.vue'

const host = ref(null)
const currentView = ref('desk')
const recordFilter = ref('all')
const dialogName = ref('')
const editingRules = ref(false)
const goalTeam = ref('home')
const goalRecordId = ref(0)
const timeoutTeam = ref('home')
const penaltyDefaults = ref({})
const correctionId = ref(0)
const pageStatus = ref('loading')
const pageMessage = ref('')
const actionError = ref('')
const actionErrorElement = ref(null)
const scoreboardStore = useScoreboardStore()
const initialMatchId = matchIdFromSearch()
let scoreboardEngine
let scoreboardController
let scoreboardRuntime
let dialogTrigger

provide(scoreboardActionsKey, (action, params = {}, event) => {
  handleAction(action, params, event?.currentTarget)
})
provide(scoreboardFormServiceKey, {
  submit: async (type, payload, context) => {
    const effect = await scoreboardController?.submitForm(type, payload, {
      ...context,
      dialog: dialogName.value,
      editingRules: editingRules.value,
      correctionId: correctionId.value,
      timeoutTeam: timeoutTeam.value,
      goalRecordId: goalRecordId.value,
    })
    if (effect?.matchId) {
      replaceMatchIdInSearch(effect.matchId)
      pageStatus.value = 'ready'
      pageMessage.value = ''
    }
    dialogName.value = ''
    editingRules.value = false
    restoreDialogFocus()
  },
  savePendingGoal: payload => scoreboardController?.savePendingGoal(payload),
})

onMounted(async () => {
  const persistence = createScoreboardPersistence(undefined, {
    // 缓存读写失败不打扰计分员；跨页面修改仍需提示，避免覆盖另一页面的数据。
    onMessage: message => {
      if (message.includes('另一个页面')) pageMessage.value = message
    },
  })
  scoreboardEngine = new ScoreboardEngine(scoreboardStore, persistence, {
    onBeep: () => scoreboardRuntime?.beep(),
  })
  scoreboardController = createScoreboardController(scoreboardEngine, persistence)
  // 计时轮询必须先启动，不能被字典、模板或比赛详情请求阻塞。
  scoreboardRuntime = startScoreboardRuntime(scoreboardEngine)
  const configurationPromise = Promise.allSettled([
    scoreboardStore.loadAgeGroups(),
    scoreboardStore.loadGroupRules(),
  ])
  await loadInitialMatch(persistence)
  const [ageGroupResult, ruleResult] = await configurationPromise
  const configurationErrors = [ageGroupResult, ruleResult]
    .filter(result => result.status === 'rejected')
    .map(result => result.reason?.message || '加载失败')
  if (configurationErrors.length) {
    pageMessage.value = `建赛配置加载失败：${configurationErrors.join('；')}`
  }
  if (pageStatus.value === 'ready' && scoreboardStore.match?.pendingGoal) {
    goalTeam.value = scoreboardStore.match.pendingGoal.team || 'home'
    dialogName.value = 'goal-entry'
  }
})

onBeforeUnmount(() => {
  scoreboardRuntime?.stop()
  scoreboardRuntime = undefined
  scoreboardController = undefined
  scoreboardEngine = undefined
})

defineExpose({ scoreboardStore })

function selectView(view) {
  if (['desk', 'rules', 'log'].includes(view)) currentView.value = view
}

function closeDialog() {
  scoreboardController?.cancelDialog(dialogName.value)
  dialogName.value = ''
  editingRules.value = false
  restoreDialogFocus()
}

function changeGoalTeam(team) {
  goalTeam.value = team
  scoreboardController?.setGoalDialogTeam(dialogName.value, team)
}

function handleAction(action, params, trigger) {
  if (trigger?.disabled) return
  if (pageStatus.value === 'empty' && !actionAllowedWithoutMatch(action)) return
  actionError.value = ''
  try {
    const effect = scoreboardController?.performAction(action, params)
    if (!effect) return
    if (effect.download) downloadFile(effect.download.content, effect.download.filename, effect.download.type)
    if (effect.fullscreen) toggleFullscreen().catch(error => showActionError(error.message))
    if (effect.enableAudio) scoreboardRuntime?.enableAudio()
    if (effect.view) selectView(effect.view)
    if (effect.recordFilter) recordFilter.value = effect.recordFilter
    if (!effect.dialog) return
    dialogTrigger = trigger
    dialogName.value = effect.dialog
    editingRules.value = Boolean(effect.editingRules)
    if (effect.goalTeam) goalTeam.value = effect.goalTeam
    if (effect.goalRecordId) goalRecordId.value = effect.goalRecordId
    if (effect.timeoutTeam) timeoutTeam.value = effect.timeoutTeam
    penaltyDefaults.value = { ...(effect.penaltyDefaults || {}) }
    if (effect.correctionId) correctionId.value = effect.correctionId
  } catch (error) {
    showActionError(error.message)
  }
}

function showActionError(message) {
  actionError.value = message
  nextTick(() => actionErrorElement.value?.focus())
}

function restoreDialogFocus() {
  const trigger = dialogTrigger
  dialogTrigger = undefined
  nextTick(() => {
    const target = trigger?.isConnected && !trigger.disabled
      ? trigger
      : host.value?.querySelector('#timing-desk')
    target?.focus({ preventScroll: true })
  })
}

function performAction(action, event) {
  handleAction(action, {}, event?.currentTarget)
}

async function loadInitialMatch(persistence) {
  pageStatus.value = 'loading'
  pageMessage.value = ''
  try {
    if (initialMatchId) {
      await scoreboardStore.loadMatchDetail(initialMatchId)
    } else {
      const latest = await scoreboardStore.loadLatestMatch()
      if (!latest) {
        scoreboardStore.useLocalMode()
        scoreboardStore.replace(createEmptyMatch())
        pageStatus.value = 'empty'
        return
      }
      replaceMatchIdInSearch(latest.id)
    }
    persistence.reset()
    pageStatus.value = 'ready'
  } catch (error) {
    pageStatus.value = 'error'
    pageMessage.value = `比赛加载失败：${error.message}`
  }
}

</script>

<template>
  <div
    ref="host"
    class="app-shell scoreboard-host"
  >
    <section v-if="pageStatus === 'loading'" class="match-empty" aria-live="polite">
      <p>正在加载最近的比赛…</p>
    </section>

    <section v-else-if="pageStatus === 'error'" class="match-empty" role="alert">
      <h1>比赛加载失败</h1>
      <p>{{ pageMessage }}</p>
      <button type="button" @click="loadInitialMatch(scoreboardEngine.persistence)">重新加载</button>
    </section>

    <template v-else-if="['ready', 'empty'].includes(pageStatus) && scoreboardStore.match">
      <header class="topbar">
        <MatchHeader
          :match="scoreboardStore.match"
          :remote-match-id="scoreboardStore.remoteMatchId"
          :locked="pageStatus === 'empty'"
        />
      </header>
      <nav class="navigation" aria-label="应用导航">
        <MatchNavigation
          :match="scoreboardStore.match"
          :current-view="currentView"
          :sync-status="scoreboardStore.syncStatus"
          :sync-error="scoreboardStore.syncError"
          :remote-match-id="scoreboardStore.remoteMatchId"
          @select="selectView"
        />
      </nav>
      <fieldset
        class="scoreboard-lock"
        :disabled="pageStatus === 'empty'"
        :inert="pageStatus === 'empty' ? '' : null"
        :aria-disabled="pageStatus === 'empty'"
      >
      <p id="page-status" class="error" role="status" :hidden="!pageMessage">{{ pageMessage }}</p>
      <p v-if="actionError" id="error" ref="actionErrorElement" class="error" tabindex="-1" role="alert">{{ actionError }}</p>

      <template v-if="scoreboardStore.match.goldenConflict">
        <p class="correction-stop">加时只能有首个有效进球。当前记录和比分待核对，不能复赛或确认结束。</p>
        <button type="button" class="primary next" @click="performAction('resolve-golden-form', $event)">核对加时金球进球</button>
      </template>

      <div v-if="currentView === 'desk'" class="workspace match-workspace">
        <main>
          <div class="scoreboard">
            <section class="team home" :aria-label="scoreboardStore.match.config.home">
              <TeamPanel :match="scoreboardStore.match" team="home" />
            </section>
            <section id="timing-desk" class="main-clock" tabindex="-1" aria-label="比赛计时">
              <MatchClock :match="scoreboardStore.match" />
              <MatchClockControls :match="scoreboardStore.match" />
              <section class="records-panel records-compact" aria-label="比赛事件记录">
                <MatchRecords :match="scoreboardStore.match" :filter="recordFilter" compact />
              </section>
            </section>
            <section class="team away" :aria-label="scoreboardStore.match.config.away">
              <TeamPanel :match="scoreboardStore.match" team="away" />
            </section>
          </div>

          <div class="desk-controls">
            <section class="control-section"><FieldControls :match="scoreboardStore.match" /></section>
            <section class="penalties"><PenaltyList :match="scoreboardStore.match" /></section>
          </div>

          <p v-if="scoreboardStore.match.alert" class="alert" role="alert">{{ scoreboardStore.match.alert }}</p>
          <p class="sr-only" role="status">{{ scoreboardStore.match.notice }}</p>
        </main>
      </div>

      <main v-else-if="currentView === 'rules'" class="rules-view">
        <MatchRules :match="scoreboardStore.match" />
      </main>

      <main v-else class="log-view">
        <div class="section-heading">
          <h2>比赛记录</h2>
          <div class="record-actions">
            <button v-if="scoreboardStore.match.phase === 'finished'" type="button" @click="performAction('final-score-form', $event)">赛后更正比分</button>
          </div>
        </div>
        <p>进球须填写进球球员，助攻选填；漏记请用“补录进球”，已计入比分的球可只补明细。判罚和暂停可在对应记录旁更正或作废，须填写依据；原记录与修改痕迹保留。</p>
        <section class="records-panel" aria-label="比赛事件记录">
          <MatchRecords :match="scoreboardStore.match" :filter="recordFilter" />
        </section>
      </main>

      <footer v-if="currentView !== 'desk'">比赛操作同步至服务器 · 进球、球权与判罚按裁判指令记录</footer>
      </fieldset>

      <MatchInfoDialog v-if="dialogName === 'match-info'" :match="scoreboardStore.match" @close="closeDialog" />
      <MatchExportDialog v-if="dialogName === 'export'" @close="closeDialog" />
      <GoalEntryDialog
        v-if="['goal-entry', 'goal-backfill'].includes(dialogName)"
        :match="scoreboardStore.match"
        :mode="dialogName === 'goal-backfill' ? 'backfill' : 'entry'"
        :initial-team="goalTeam"
        @team-change="changeGoalTeam"
        @close="closeDialog"
      />
      <GoalDetailsDialog v-if="dialogName === 'goal-details'" :match="scoreboardStore.match" :goal-record-id="goalRecordId" @close="closeDialog" />
      <TeamTimeoutDialog v-if="dialogName === 'timeout'" :match="scoreboardStore.match" :team="timeoutTeam" @close="closeDialog" />
      <PenaltyDialog v-if="dialogName === 'penalty'" :match="scoreboardStore.match" :defaults="penaltyDefaults" @close="closeDialog" />
      <RecordCorrectionDialog
        v-if="['correct-penalty', 'correct-timeout'].includes(dialogName)"
        :match="scoreboardStore.match"
        :type="dialogName === 'correct-timeout' ? 'timeout' : 'penalty'"
        :correction-id="correctionId"
        @close="closeDialog"
      />
      <MatchCorrectionDialog v-if="dialogName === 'correct'" :match="scoreboardStore.match" @close="closeDialog" />
      <FinalScoreDialog v-if="dialogName === 'final-score'" :match="scoreboardStore.match" @close="closeDialog" />
      <GoldenGoalReviewDialog v-if="dialogName === 'resolve-golden'" :match="scoreboardStore.match" @close="closeDialog" />
      <ClearMatchRecordsDialog v-if="dialogName === 'clear-match-records'" @close="closeDialog" />
    </template>

    <MatchSetupDialog
      v-if="dialogName === 'setup' && scoreboardStore.match"
      :match="scoreboardStore.match"
      :editing-rules="editingRules"
      :age-groups="scoreboardStore.ageGroups"
      :group-rules="scoreboardStore.groupRules"
      @close="closeDialog"
    />
  </div>
</template>
