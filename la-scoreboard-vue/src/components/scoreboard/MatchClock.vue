<script setup>
import { computed } from 'vue'
import { useScoreboardActions } from '../../composables/useScoreboardActions.js'
import { isYouth } from '../../core/rules.js'
import {
  canFinish,
  clock,
  isOvertime,
  periodHasStarted,
  periodName,
  shotOff,
  stopTime,
} from '../../core/timing.js'

const props = defineProps({
  match: {
    type: Object,
    required: true,
  },
})
const performAction = useScoreboardActions()

const periodLabel = computed(() => {
  const state = props.match
  const total = !isOvertime(state) && state.rules.periods !== 2 ? ` / ${state.rules.periods}` : ''
  return `${periodName(state)}${total}`
})

const phaseLabel = computed(() => {
  const state = props.match
  if (state.goldenConflict) return '加时金球进球待核对'
  if (state.phase === 'finished') return '比赛结束'
  if (state.phase === 'period-end') {
    if (state.goldenGoal) return '金球 · 等待核对'
    return isOvertime(state) && !periodHasStarted(state) ? '加时未开赛 · 待核对比分' : '本节结束'
  }
  if (state.phase === 'break') return '节间休息'
  if (state.phase === 'timeout') return '球队暂停'
  if (!state.running) return '停表 · 等待鸣哨'
  return state.ball === 'live' ? '活球 · 计时中' : '死球 · 比赛钟继续'
})

const timingRule = computed(() => {
  const state = props.match
  if (stopTime(state)) return '死球停表'
  return state.rules.foulStop ? '普通死球跑表 · 犯规鸣哨停表' : '普通死球跑表 · 有罚时判罚停表'
})

const shotExpired = computed(() => !shotOff(props.match)
  && !props.match.resetOnResume
  && props.match.shot === 0)

const shotStatus = computed(() => {
  const state = props.match
  if (shotOff(state)) return '本节末段关闭'
  if (state.resetOnResume) return '复赛时重置'
  if (shotExpired.value) return '到时，待裁判确认'
  return state.running && state.ball === 'live' && state.possession ? '计时中' : '等待球权／复赛'
})

const clearStatus = computed(() => {
  const state = props.match
  if (state.frontcourt) return '已在前场'
  if (state.clear === null) return '待标记后场'
  if (state.clear === 0) return '到时，待裁判确认'
  return state.running && state.ball === 'live' ? '推进计时中' : '已暂停'
})
</script>

<template>
  <div class="clock-core">
    <div class="period-line">
      <strong>{{ periodLabel }}</strong>
      <span class="status">{{ phaseLabel }}</span>
    </div>

    <div class="clock-display">
      <div class="time" data-clock="main" aria-label="比赛剩余时间">
        {{ clock(match.main) }}
      </div>

      <div v-if="match.rules.shot || match.rules.clearMs" class="secondary-clocks">
        <div
          v-if="match.rules.shot"
          class="secondary-clock"
          :data-expired="String(shotExpired)"
        >
          <span>进攻钟</span>
          <strong data-clock="shot" aria-label="进攻剩余时间">{{ clock(match.shot) }}</strong>
          <small id="shot-status">{{ shotStatus }}</small>
        </div>

        <div
          v-if="match.rules.clearMs"
          class="secondary-clock"
          :data-expired="String(match.clear === 0)"
        >
          <span>推进辅助 · {{ match.rules.clearMs / 1000 }}秒</span>
          <strong data-clock="clear" aria-label="推进剩余时间">{{ clock(match.clear ?? 0) }}</strong>
          <small id="clear-status">{{ clearStatus }}</small>
        </div>
      </div>
    </div>

    <p id="timing-rule" class="clock-caption">{{ timingRule }}</p>

    <div v-if="['break', 'timeout'].includes(match.phase)" class="auxiliary">
      <span>{{ match.phase === 'break' ? '休息' : '暂停' }}倒计时</span>
      <strong data-clock="auxiliary">{{ clock(match.auxiliary) }}</strong>
      <button type="button" @click="performAction('auxiliary', {}, $event)">
        {{ match.auxiliaryRunning ? '暂停计时' : '继续计时' }}
      </button>
      <button v-if="match.phase === 'timeout'" type="button" @click="performAction('end-timeout', {}, $event)">
        结束暂停
      </button>
    </div>

    <button
      v-if="match.phase === 'finished'"
      type="button"
      class="next"
      @click="performAction('final-score-form', {}, $event)"
    >
      赛后更正比分
    </button>

    <button
      v-if="match.phase === 'period-end' && !match.goldenConflict"
      type="button"
      class="primary next"
      @click="performAction('next', {}, $event)"
    >
      {{ canFinish(match) ? '核对比分并结束比赛' : '核对比分，进入休息' }}
    </button>

    <div v-if="match.phase === 'break'" class="next-period">
      <p>休息结束后按裁判指令选择开球方式：</p>
      <button type="button" @click="performAction('prepare', {}, $event)">
        {{ isYouth(match) ? '准备下一半场／加时 · 中心开球' : '准备下一节 · 重新争球' }}
      </button>
      <button
        type="button"
        @click="performAction('prepare', { carry: true }, $event)"
        :disabled="!match.possession"
      >
        人数不等 · 裁判确认保留球权
      </button>
    </div>
  </div>
</template>
