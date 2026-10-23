<script setup>
import BaseDialog from '../common/BaseDialog.vue'
import { useScoreboardActions } from '../../composables/useScoreboardActions.js'

const emit = defineEmits(['close'])
const performAction = useScoreboardActions()
</script>

<template>
  <BaseDialog open title="导出比赛记录" @close="emit('close')">
    <div class="export-options">
      <p>导出完整比赛记录，包含已作废、已撤销及更正痕迹，不受当前筛选影响。</p>
      <p>
        CSV 用于 WPS／Excel 查阅：包含比赛信息、导出时总比分、球员／助攻、犯规备注与更正依据。球队列使用当前队名，历史原文保留当时称呼。
      </p>
      <p>JSON 保留完整本地比赛数据，便于备份与技术核对。</p>
    </div>

    <template #footer>
      <button type="button" @click="emit('close')">关闭</button>
      <button type="button" @click="performAction('export-json', {}, $event)">下载 JSON 数据</button>
      <button type="button" class="primary" @click="performAction('export-csv', {}, $event)">下载 CSV 表格</button>
    </template>
  </BaseDialog>
</template>
