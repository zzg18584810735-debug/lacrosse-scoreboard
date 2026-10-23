<script setup>
import { computed } from 'vue'
import { useScoreboardActions } from '../../composables/useScoreboardActions.js'
import {
  canPauseClock,
  canResumeClock,
  pendingPenaltyTasks,
} from './matchClockControls.js'

const props = defineProps({
  match: {
    type: Object,
    required: true,
  },
})
const performAction = useScoreboardActions()

const penaltyTasks = computed(() => pendingPenaltyTasks(props.match))
const teamName = team => props.match.config[team]
</script>

<template>
  <section v-if="penaltyTasks.length" class="penalty-tasks" aria-label="罚时待办">
    <h3>罚时待办 · {{ penaltyTasks.length }}</h3>
    <p>未就座的罚时不会开始；正常倒计时到时自动释放，人工置零或旧记录可手动确认释放。</p>
    <div v-for="penalty in penaltyTasks" :key="penalty.id">
      <span>
        <strong :class="`${penalty.team}-ink`">
          {{ teamName(penalty.team) }} · {{ penalty.server }} 号
        </strong>
        <small>{{ penalty.remaining === 0 ? '罚时到，等待释放' : '等待就座' }}</small>
      </span>
      <button
        type="button"
        @click="performAction(penalty.remaining === 0 ? 'release' : 'seat', { id: penalty.id }, $event)"
        :aria-label="`${teamName(penalty.team)} ${penalty.server} 号${penalty.remaining === 0 ? '确认释放' : '确认已就座'}`"
      >
        {{ penalty.remaining === 0 ? '确认释放' : '确认已就座' }}
      </button>
    </div>
  </section>

  <div class="clock-actions">
    <button
      type="button"
      class="primary"
      @click="performAction('resume', {}, $event)"
      :disabled="!canResumeClock(match)"
    >
      {{ match.phase === 'ready' ? '鸣哨开赛' : '鸣哨复赛' }}
    </button>
    <button
      type="button"
      @click="performAction('pause', {}, $event)"
      :disabled="!canPauseClock(match)"
    >
      裁判暂停
    </button>
  </div>

  <div class="corrections">
    <button type="button" :disabled="!match.undo" @click="performAction('undo', {}, $event)">
      撤销上一步
    </button>
    <button v-if="match.phase !== 'finished'" type="button" @click="performAction('correct-form', {}, $event)">
      比赛更正：时间／比分／罚时
    </button>
    <span>打开更正即停表，取消后也保持停表</span>
  </div>
</template>
