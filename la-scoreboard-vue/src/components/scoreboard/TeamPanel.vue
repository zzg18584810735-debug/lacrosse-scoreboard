<script setup>
import { computed } from 'vue'
import { isOvertime } from '../../core/timing.js'
import { foulCounts } from '../../core/matchRecords.js'
import { useScoreboardActions } from '../../composables/useScoreboardActions.js'
import {
  canRecordGoal,
  canRecordPenalty,
  canRequestTeamTimeout,
  expelledNumbers,
} from './teamPanel.js'

const props = defineProps({
  match: {
    type: Object,
    required: true,
  },
  team: {
    type: String,
    required: true,
    validator: value => ['home', 'away'].includes(value),
  },
})
const performAction = useScoreboardActions()

const cards = [
  ['minor-turnover', '无牌'],
  ['minor', '绿牌'],
  ['major', '黄牌'],
  ['expulsion', '红牌'],
]

const teamName = computed(() => props.match.config[props.team])
const counts = computed(() => foulCounts(props.match, props.team))
const expelled = computed(() => expelledNumbers(props.match, props.team))
</script>

<template>
  <div class="team-heading">
    <span class="team-side">{{ team === 'home' ? '主队' : '客队' }}</span>
    <h2>{{ teamName }}</h2>
  </div>

  <div class="score" aria-label="得分">{{ match.scores[team] }}</div>

  <div class="team-actions">
    <button
      type="button"
      class="goal"
      @click="performAction('goal', { team }, $event)"
      :disabled="!canRecordGoal(match)"
    >
      进球 <strong>+{{ match.rules.points }}</strong>
    </button>
    <button
      type="button"
      class="timeout-button"
      @click="performAction('timeout', { team }, $event)"
      :disabled="!canRequestTeamTimeout(match, team)"
    >
      暂停 <strong>{{ match.timeouts[team] }}</strong>
    </button>
  </div>

  <p class="timeout-label">
    {{ isOvertime(match) ? '本节加时' : '本半场' }}剩余 {{ match.timeouts[team] }} 次 ·
    每次 {{ match.rules.timeout / 1000 }} 秒
  </p>

  <div class="team-fouls">
    <h3>记录判罚 · 累计次数</h3>
    <div class="card-counts">
      <div v-for="([kind, label]) in cards" :key="kind">
        <button
          type="button"
          :class="['card-count', kind]"
          @click="performAction('penalty-form', { team, kind }, $event)"
          :aria-label="`${teamName} ${label}犯规`"
          :disabled="!canRecordPenalty(match)"
          :title="counts[kind] === null ? '旧版无牌记录未分类，请查看全部记录' : `记录${label}判罚`"
        >
          {{ counts[kind] === null ? '—' : counts[kind] }}
        </button>
        <span>{{ label }}</span>
      </div>
    </div>
  </div>

  <div class="team-expelled">
    <span>本场罚退：</span>
    <span v-if="expelled.length">
      <template v-for="(number, index) in expelled" :key="`${number}-${index}`">
        <span v-if="index">、</span><strong>{{ number }} 号</strong>
      </template>
    </span>
    <span v-else>无</span>
  </div>
</template>
