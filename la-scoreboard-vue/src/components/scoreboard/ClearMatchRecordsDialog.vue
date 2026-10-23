<script setup>
import { reactive } from 'vue'
import BaseDialog from '../common/BaseDialog.vue'
import { useScoreboardForm } from '../../composables/useScoreboardForm.js'

defineEmits(['close'])

const draft = reactive({ confirm: false })
const { errorElement, errorMessage, submitForm } = useScoreboardForm()
</script>

<template>
  <BaseDialog
    open
    title="清空比赛记录"
    form-id="clear-match-records-form"
    confirm-label="清空记录"
    danger
    @close="$emit('close')"
  >
    <form id="clear-match-records-form" class="clear-records-form" @submit.prevent="submitForm('clear-match-records', { ...draft }, $event)">
      <div class="clear-records-intro">
        <span aria-hidden="true">!</span>
        <div>
          <h3>赛程不会删除</h3>
          <p>仅清空这场比赛已经产生的计分和过程记录，并恢复到赛前状态。</p>
        </div>
      </div>
      <div class="clear-records-scope">
        <section>
          <h3>继续保留</h3>
          <p>比赛日期和场地、主客队、本场规则、球员名单、教练及裁判安排</p>
        </section>
        <section class="will-clear">
          <h3>将被清空</h3>
          <p>比分和计时进度、进球、暂停、犯规、更正及罚时记录</p>
        </section>
      </div>
      <label class="clear-records-confirm">
        <input v-model="draft.confirm" type="checkbox" required>
        <span>我已核对比赛，确认清空上述记录</span>
      </label>
      <p v-if="errorMessage" ref="errorElement" class="error" tabindex="-1" role="alert">{{ errorMessage }}</p>
    </form>
  </BaseDialog>
</template>
