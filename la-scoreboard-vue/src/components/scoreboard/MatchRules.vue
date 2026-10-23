<script setup>
import { computed } from 'vue'
import { actionUnavailable } from '../../core/actionPolicy.js'
import { isCustom } from '../../core/rules.js'
import { useScoreboardActions } from '../../composables/useScoreboardActions.js'
import { ruleSummaryItems } from './ruleView.js'

const props = defineProps({
  match: {
    type: Object,
    required: true,
  },
})
const performAction = useScoreboardActions()

const customized = computed(() => isCustom(props.match))
const editReason = computed(() => actionUnavailable(props.match, 'edit-rules'))
const summaryItems = computed(() => ruleSummaryItems(props.match.rules))
</script>

<template>
  <div class="section-heading">
    <h2>本场规则 · {{ customized ? '自定义' : '预设' }}</h2>
    <button
      type="button"
      @click="performAction('edit-rules', {}, $event)"
      :disabled="Boolean(editReason)"
      :title="editReason || undefined"
    >
      修改本场规则
    </button>
  </div>

  <p class="lead">
    {{ customized
      ? '以下为本场实际执行的参数；已修改项不代表原 PDF 的官方预设。'
      : '根据所选模板配置，本场计时、暂停、得分与罚时均按以下参数执行。' }}
  </p>

  <div class="rule-summary">
    <div v-for="item in summaryItems" :key="item.key">
      <span>{{ item.label }}</span><strong>{{ item.value }}</strong>
    </div>
  </div>

  <div class="rule-columns">
    <section>
      <h3>得分与判罚</h3>
      <p>
        有效进球 {{ match.rules.points }} 分。{{ match.rules.passRequired
          ? '首次射门前须完成至少一次两个球员之间的成功接传。'
          : '' }}是否有效由裁判确认，压哨或漏记的有效球可通过“补录进球”记录球员和发生时间。
      </p>
      <p>
        轻微犯规时对方无球权，可仅换球权；对方控球时执行绿牌罚时。严重犯规累计
        {{ match.rules.majorLimit }} 次罚退。官员代罚不累计到代罚球员；守门员犯规记在守门员名下。
      </p>
      <p>已执行罚时不因进球取消。中场返回须重新确认就座；满足就座和复赛两个条件才计时。</p>
    </section>
    <section>
      <h3>需裁判确认的情况</h3>
      <p>
        延缓轻微犯规可能因有效进球取消；延缓严重／驱逐犯规进球后，进球有效且球权保留给进球方。先记录进球，再按裁判报告录入判罚与球权。
      </p>
      <p>
        双方犯规、连续多项处罚、伤停、场上人员与装备资格、守门圈五秒和无效接传均由裁判核定；应用不自动判断。六人制弃权按原文判
        1:0，当前无自动弃权结算入口。因天气等未完成比赛的结果交赛事组委会裁定。
      </p>
      <p>
        六人制原文未明确加时球队暂停额度，模板默认不发放；特别规程允许时可在建赛前修改。六人制“人墙”正文与术语表分类不一致，保留原文并交裁判确认。
      </p>
    </section>
  </div>

  <p class="source-note">
    模板来源：中国 U12 2026（2026-04-27）第 10、13、15、17、19–21、27–28 页；六人制中文
    2026 V1.0（2026-04-16）第 15–17、24、29–34 页。原件与清洗索引保存在项目背景文档中。
  </p>
</template>
