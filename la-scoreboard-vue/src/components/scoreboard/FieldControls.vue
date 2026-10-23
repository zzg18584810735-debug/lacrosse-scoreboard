<script setup>
import { computed } from 'vue'
import { useScoreboardActions } from '../../composables/useScoreboardActions.js'
import {
  canSetPossession,
  canSetAlternate,
  clearProgressLabel,
  isFieldActive,
  otherTeam,
} from './fieldControls.js'

const props = defineProps({
  match: {
    type: Object,
    required: true,
  },
})
const performAction = useScoreboardActions()

const possessionName = computed(() => props.match.possession
  ? props.match.config[props.match.possession]
  : '')
const alternateName = computed(() => props.match.alternate
  ? props.match.config[props.match.alternate]
  : '')
const alternateCorrection = computed(() => otherTeam(props.match.alternate))
</script>

<template>
  <div class="section-heading"><h2>球权与场上事件</h2></div>

  <div class="field-groups">
    <fieldset class="field-group">
      <legend>
        球权与死球
        <span>· {{ match.possession ? `${possessionName}控球` : '等待争球' }}</span>
      </legend>
      <div class="group-actions possession-actions">
        <button
          type="button"
          class="home-ink"
          @click="performAction('possession', { team: 'home' }, $event)"
          :aria-pressed="match.possession === 'home'"
          :disabled="!canSetPossession(match)"
        >
          {{ match.config.home }}有球
        </button>
        <button
          type="button"
          class="away-ink"
          @click="performAction('possession', { team: 'away' }, $event)"
          :aria-pressed="match.possession === 'away'"
          :disabled="!canSetPossession(match)"
        >
          {{ match.config.away }}有球
        </button>
        <button
          type="button"
          class="dead-ball-button"
          @click="performAction('whistle', {}, $event)"
          :disabled="!isFieldActive(match) || match.ball !== 'live'"
        >
          出界／一般死球
        </button>
      </div>
    </fieldset>

    <fieldset class="field-group">
      <legend>
        交替发球
        <span>· {{ match.alternate ? `下次：${alternateName}` : '下次待确认' }}</span>
      </legend>
      <div class="group-actions">
        <button
          type="button"
          @click="performAction('alternate-award', {}, $event)"
          :disabled="!isFieldActive(match) || !match.alternate"
        >
          按交替发球
        </button>
        <template v-if="!match.alternate">
          <button type="button" :disabled="!canSetAlternate(match)" @click="performAction('alternate-set', { team: 'home' }, $event)">
            首次给主队
          </button>
          <button type="button" :disabled="!canSetAlternate(match)" @click="performAction('alternate-set', { team: 'away' }, $event)">
            首次给客队
          </button>
        </template>
        <button
          v-else
          type="button"
          @click="performAction('alternate-set', { team: alternateCorrection }, $event)"
          :disabled="!canSetAlternate(match)"
        >
          更正为{{ match.config[alternateCorrection] }}
        </button>
      </div>
    </fieldset>

    <fieldset v-if="match.rules.shot" class="field-group">
      <legend>进攻钟 <span>· {{ match.rules.shot / 1000 }} 秒</span></legend>
      <div class="group-actions">
        <button type="button" :disabled="!isFieldActive(match)" @click="performAction('reset-shot', {}, $event)">
          重置 {{ match.rules.shot / 1000 }} 秒
        </button>
        <button type="button" :disabled="!isFieldActive(match)" @click="performAction('wide', {}, $event)">
          射偏 · 不重置
        </button>
      </div>
    </fieldset>

    <fieldset v-if="match.rules.clearMs" class="field-group">
      <legend>推进 <span>· {{ clearProgressLabel(match) }}</span></legend>
      <div class="group-actions">
        <button type="button" :disabled="!isFieldActive(match)" @click="performAction('backcourt', {}, $event)">
          后场开始 {{ match.rules.clearMs / 1000 }} 秒
        </button>
        <button type="button" :disabled="!isFieldActive(match)" @click="performAction('frontcourt', {}, $event)">
          已推进前场
        </button>
      </div>
    </fieldset>
  </div>
</template>
