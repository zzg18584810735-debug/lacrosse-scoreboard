<script setup>
import { computed } from 'vue'
import { compactSyncSummary, navigationSummary, SCOREBOARD_VIEWS } from './matchNavigation.js'

const props = defineProps({
  match: {
    type: Object,
    required: true,
  },
  currentView: {
    type: String,
    required: true,
  },
  syncStatus: {
    type: String,
    default: 'local',
  },
  remoteMatchId: {
    type: [String, Number],
    default: null,
  },
  syncError: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['select'])
const syncSummary = computed(() => compactSyncSummary(props.syncStatus, props.remoteMatchId))
</script>

<template>
  <span class="navigation-summary">
    {{ navigationSummary(match) }}
    <span
      v-if="syncSummary"
      class="sync-summary"
      :data-status="syncStatus"
      :title="syncError || syncSummary"
      role="status"
    > · {{ syncSummary }}</span>
  </span>
  <div class="tabs">
    <button
      v-for="([value, label]) in SCOREBOARD_VIEWS"
      :key="value"
      type="button"
      :aria-current="currentView === value ? 'page' : 'false'"
      @click="emit('select', value)"
    >
      {{ label }}
    </button>
  </div>
</template>
