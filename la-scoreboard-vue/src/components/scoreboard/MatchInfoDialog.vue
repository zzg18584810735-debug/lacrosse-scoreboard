<script setup>
import { reactive } from 'vue'
import BaseDialog from '../common/BaseDialog.vue'
import { useScoreboardForm } from '../../composables/useScoreboardForm.js'
import { createMatchInfoDraft } from './matchInfoDialog.js'

const props = defineProps({
  match: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['close'])
const { errorElement, errorMessage, submitForm } = useScoreboardForm()
const draft = reactive(createMatchInfoDraft(props.match))
</script>

<template>
  <BaseDialog
    open
    title="修改比赛信息"
    form-id="match-info-form"
    confirm-label="保存比赛信息"
    @close="emit('close')"
  >
    <form id="match-info-form" @submit.prevent="submitForm('match-info', { ...draft }, $event)">
      <p>
        仅修改比赛名称、日期和队名。比分、球员记录、规则和当前计时状态保持不变；主客队身份不交换。历史原文保留当时称呼。
      </p>
      <label>比赛名称<input v-model="draft.title" name="title" required maxlength="80"></label>
      <label>比赛日期<input v-model="draft.date" name="date" type="date" required></label>
      <div class="form-grid">
        <label>主队名称<input v-model="draft.home" name="home" required maxlength="40"></label>
        <label>客队名称<input v-model="draft.away" name="away" required maxlength="40"></label>
      </div>
      <p v-if="errorMessage" ref="errorElement" class="form-error error" role="alert" tabindex="-1">{{ errorMessage }}</p>
    </form>
  </BaseDialog>
</template>
