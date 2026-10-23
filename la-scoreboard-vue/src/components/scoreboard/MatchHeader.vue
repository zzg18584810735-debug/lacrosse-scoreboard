<script setup>
import { periodName } from '../../core/timing.js'
import { useTheme } from '../../composables/useTheme.js'
import { useScoreboardActions } from '../../composables/useScoreboardActions.js'
import { shortMatchDate } from './matchNavigation.js'
import { canClearMatchRecords } from './matchHeader.js'

const props = defineProps({
  match: {
    type: Object,
    required: true,
  },
  remoteMatchId: {
    type: [String, Number],
    default: null,
  },
  locked: Boolean,
})

const { themeMode, setTheme } = useTheme()
const performAction = useScoreboardActions()
</script>

<template>
  <div class="header-tools">
    <button type="button" :disabled="locked" @click="performAction('match-info-form', {}, $event)">比赛信息</button>
    <button type="button" class="subtle" @click="performAction('setup', {}, $event)">新建比赛</button>
    <button
      v-if="canClearMatchRecords(props.remoteMatchId)"
      type="button"
      class="danger-outline"
      :disabled="locked"
      @click="performAction('clear-match-records-form', {}, $event)"
    >清空比赛记录</button>
  </div>
  <div class="match-title">
    <h1 tabindex="-1">{{ match.config.title }}</h1>
    <p>
      {{ shortMatchDate(match.config.date) }} · {{ match.config.home }}
      <span>vs</span> {{ match.config.away }} · {{ periodName(match) }}
    </p>
  </div>
  <div class="top-actions">
    <label class="theme-picker">
      <span class="sr-only">外观模式</span>
      <select
        id="theme-mode"
        :value="themeMode"
        aria-label="外观模式"
        @change="setTheme($event.target.value)"
      >
        <option value="system">跟随系统</option>
        <option value="light">日间模式</option>
        <option value="dark">夜间模式</option>
      </select>
    </label>
    <button type="button" :disabled="locked" @click="performAction('export', {}, $event)">导出记录</button>
    <button type="button" @click="performAction('fullscreen', {}, $event)">全屏</button>
  </div>
</template>
