<script setup>
import { computed } from 'vue'
import { clock } from '../../core/timing.js'
import { useScoreboardActions } from '../../composables/useScoreboardActions.js'
import {
  activePenalties,
  penaltyDetails,
  penaltyStatus,
} from './penaltyList.js'

const props = defineProps({
  match: {
    type: Object,
    required: true,
  },
})
const performAction = useScoreboardActions()

const penalties = computed(() => activePenalties(props.match))
const teamName = team => props.match.config[team]
const subjectLabel = penalty => penalty.subject === 'official'
  ? `官员 ${penalty.player}`
  : `${penalty.player} 号`
</script>

<template>
  <div class="section-heading">
    <h2>罚时区 <span>{{ penalties.length }}</span></h2>
    <span>对手进球不会取消罚时</span>
  </div>

  <div
    v-if="penalties.length"
    class="penalty-list"
    tabindex="0"
    aria-label="当前罚时列表，更多罚时可在区内滚动"
  >
    <div v-for="penalty in penalties" :key="penalty.id" class="penalty-row">
      <div>
        <strong>{{ teamName(penalty.team) }} · {{ subjectLabel(penalty) }}</strong>
        <small>{{ penaltyDetails(match, penalty) }}</small>
      </div>
      <strong class="penalty-time" :data-penalty-clock="penalty.id">
        {{ clock(penalty.remaining) }}
      </strong>
      <span>{{ penaltyStatus(match, penalty) }}</span>
      <button
        v-if="!penalty.seated && penalty.remaining > 0"
        type="button"
        @click="performAction('seat', { id: penalty.id }, $event)"
      >
        确认已就座
      </button>
      <button
        v-else-if="penalty.remaining === 0"
        type="button"
        @click="performAction('release', { id: penalty.id }, $event)"
      >
        确认释放
      </button>
    </div>
  </div>

  <p v-else class="empty">暂无罚时 · 点击球队色牌记录判罚</p>
</template>
