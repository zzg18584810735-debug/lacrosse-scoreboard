<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { actionUnavailable } from '../../core/actionPolicy.js'
import { matchRecords } from '../../core/matchRecords.js'
import { useScoreboardActions } from '../../composables/useScoreboardActions.js'
import {
  correctionAction,
  emptyRecordText,
  recordAssistLabel,
  recordPersonLabel,
  recordTeamLabel,
  recordTeamSide,
  RECORD_FILTERS,
  visibleRecords,
} from './recordList.js'

const props = defineProps({
  match: {
    type: Object,
    required: true,
  },
  filter: {
    type: String,
    default: 'all',
  },
  compact: Boolean,
})
const performAction = useScoreboardActions()

const scrollRoot = ref(null)
const scrollPositions = new Map()
const expanded = reactive(new Set())

const allRecords = computed(() => matchRecords(props.match, props.filter).slice().reverse())
const rows = computed(() => visibleRecords(allRecords.value, props.compact))
const emptyText = computed(() => emptyRecordText(props.filter))
const viewKey = computed(() => `${props.compact ? 'desk' : 'log'}:${props.filter}`)

function rememberScroll(event) {
  scrollPositions.set(viewKey.value, event.currentTarget.scrollTop)
}

function rememberExpanded(recordId, event) {
  const key = String(recordId)
  if (event.currentTarget.open) expanded.add(key)
  else expanded.delete(key)
}

function isExpanded(recordId) {
  return expanded.has(String(recordId))
}

function restoreScroll() {
  nextTick(() => {
    if (scrollRoot.value) scrollRoot.value.scrollTop = scrollPositions.get(viewKey.value) ?? 0
  })
}

watch(viewKey, restoreScroll, { immediate: true })
</script>

<template>
  <div class="record-heading">
    <button
      type="button"
      class="record-backfill"
      @click="performAction('backfill-goal-form', {}, $event)"
      :disabled="Boolean(actionUnavailable(match, 'backfill-goal-form'))"
    >
      补录进球
    </button>
    <div class="record-tabs" aria-label="记录分类">
      <button
        v-for="([key, label]) in RECORD_FILTERS"
        :key="key"
        type="button"
        @click="performAction('record-filter', { filter: key }, $event)"
        :aria-pressed="filter === key"
      >
        {{ label }}
      </button>
    </div>
  </div>

  <div
    ref="scrollRoot"
    class="records-scroll"
    :data-view="compact ? 'desk' : 'log'"
    :data-filter="filter"
    tabindex="0"
    :aria-label="compact ? '最近比赛记录' : '完整比赛记录'"
    @scroll="rememberScroll"
  >
    <table class="records-table">
      <thead>
        <tr><th scope="col">时间</th><th scope="col">相关队伍／球员</th><th scope="col">详细信息</th></tr>
      </thead>
      <tbody>
        <tr
          v-for="record in rows"
          :key="record.id"
          :data-record-id="record.id"
          :class="{ voided: record.voided }"
        >
          <td><time>{{ record.time }}</time><small>{{ record.period }}</small></td>
          <td>
            <span :class="recordTeamSide(record, match) === 'home' ? 'home-ink' : recordTeamSide(record, match) === 'away' ? 'away-ink' : ''">
              {{ recordTeamLabel(record, match) }}
            </span>
            <small v-if="recordPersonLabel(record, match)">{{ recordPersonLabel(record, match) }}{{ record.category === 'goal' ? ' 进球' : '' }}</small>
            <small v-if="recordAssistLabel(record, match)">助攻 {{ recordAssistLabel(record, match) }}</small>
          </td>
          <td>
            <details
              v-if="compact"
              class="record-detail"
              :data-record-detail="record.id"
              :open="isExpanded(record.id)"
              @toggle="rememberExpanded(record.id, $event)"
            >
              <summary>
                <span>{{ record.voided ? `${record.cancelled ? '已作废' : '已撤销'} · ` : '' }}{{ record.title }}</span>
                <button
                  v-if="correctionAction(record)"
                  type="button"
                  class="record-edit"
                  @click.stop.prevent="performAction(correctionAction(record).action, { id: record.id }, $event)"
                >
                  {{ correctionAction(record).label }}
                </button>
              </summary>
              <strong v-if="record.voided" class="void-label">
                {{ record.cancelled ? '已作废' : '已撤销' }} ·
              </strong>
              <span class="record-text">{{ record.text }}</span>
              <small v-if="record.rule">依据：{{ record.rule }}</small>
            </details>
            <template v-else>
              <div class="record-title-line">
                <span class="record-title-copy">
                  <strong v-if="record.voided" class="void-label">
                    {{ record.cancelled ? '已作废' : '已撤销' }} ·
                  </strong>
                  <strong>{{ record.title }}</strong>
                </span>
                <button
                  v-if="correctionAction(record)"
                  type="button"
                  class="record-edit"
                  @click="performAction(correctionAction(record).action, { id: record.id }, $event)"
                >
                  {{ correctionAction(record).label }}
                </button>
              </div>
              <span class="record-text">{{ record.text }}</span>
              <small v-if="record.rule">依据：{{ record.rule }}</small>
            </template>

            <small v-if="record.reason" class="foul-reason">
              {{ record.category === 'penalty' ? '犯规备注' : '记录依据' }}：{{ record.reason }}
            </small>
            <template v-if="record.correctionReason">
              <small>更正依据：{{ record.correctionReason }}</small>
              <details v-if="record.originalText"><summary>查看原记录</summary>{{ record.originalText }}</details>
            </template>

          </td>
        </tr>
        <tr v-if="!rows.length">
          <td colspan="3" class="records-empty">
            {{ emptyText[0] }}<small>{{ emptyText[1] }}</small>
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <small v-if="compact && allRecords.length > 30" class="record-more">
    最近 30 条 · 更早记录见“比赛记录”
  </small>
</template>
