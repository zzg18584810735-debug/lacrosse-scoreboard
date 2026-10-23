<script setup>
import { computed, reactive } from 'vue'
import BaseDialog from '../common/BaseDialog.vue'
import { revealInvalidSection, useScoreboardForm } from '../../composables/useScoreboardForm.js'
import { createMatch } from '../../core/matchModel.js'
import { PRESETS, resolveRules } from '../../core/rules.js'
import { toFrontendRules } from '../../adapters/matchDetailAdapter.js'
import {
  groupedRuleFields,
  ruleFormValues,
  rulesFromForm,
} from './setupDialog.js'

const props = defineProps({
  match: {
    type: Object,
    required: true,
  },
  editingRules: Boolean,
  ageGroups: {
    type: Array,
    default: () => [],
  },
  groupRules: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['close'])
const { errorElement, errorMessage, submitForm } = useScoreboardForm()
const base = props.editingRules ? props.match : createMatch({ preset: 'sll-u8' })
const initialAgeGroup = props.editingRules
  ? base.config.ageGroup
  : props.ageGroups[0]?.itemValue || ''
const initialRule = props.groupRules.find(rule => rule.ageGroup === initialAgeGroup)
const config = reactive({
  title: props.editingRules ? base.config.title : '',
  date: base.config.date,
  home: base.config.home,
  away: base.config.away,
  preset: base.config.preset,
  stage: base.config.stage,
  gender: base.config.gender,
  confirm: false,
  ageGroup: initialAgeGroup,
  groupRuleId: props.editingRules
    ? base.config.groupRuleId
    : initialRule?.id || '',
  saveAsTemplate: false,
  templateName: '',
})
const ruleValues = reactive(ruleFormValues(base.rules))
const ruleGroups = groupedRuleFields()
const availableTemplates = computed(() => props.groupRules
  .filter(rule => rule.ageGroup === config.ageGroup))

if (!props.editingRules && config.groupRuleId) applyGroupRule()

const showGender = computed(() => config.preset === 'sll-u12' && config.date >= '2027-01-01')
const preview = computed(() => {
  try {
    const rules = resolveRules(config.preset, rulesFromForm(ruleValues, base.rules))
    const source = config.groupRuleId ? '已选模板' : '本场自定义'
    const destination = config.saveAsTemplate ? `并保存为模板“${config.templateName || '未命名'}”` : '仅用于本场比赛'
    return `${source} · ${rules.periods} 节 × ${rules.period / 60000} 分钟 · ${rules.players} 人 · ${rules.shot ? `${rules.shot / 1000} 秒进攻钟` : '无进攻钟'}。${destination}。`
  } catch (error) {
    return error.message
  }
})

function applyRules(rules) {
  Object.assign(ruleValues, ruleFormValues(rules))
}

function onAgeGroupChange() {
  config.groupRuleId = availableTemplates.value[0]?.id || ''
  config.saveAsTemplate = false
  config.templateName = ''
  if (config.groupRuleId) applyGroupRule()
}

function onTemplateChange() {
  if (config.groupRuleId) {
    config.saveAsTemplate = false
    config.templateName = ''
    applyGroupRule()
  } else {
    config.preset = 'custom'
  }
}

function applyGroupRule() {
  const selected = props.groupRules.find(rule => String(rule.id) === String(config.groupRuleId))
  if (!selected) return
  const preset = `sll-${String(selected.ageGroup || '').toLowerCase()}`
  config.preset = Object.hasOwn(PRESETS, preset) ? preset : 'custom'
  applyRules(toFrontendRules(selected))
}

function resetPreset() {
  applyGroupRule()
}

function submitSetup(event) {
  submitForm('setup', () => ({ ...config, rules: rulesFromForm(ruleValues, base.rules) }), event)
}
</script>

<template>
  <BaseDialog
    open
    wide
    :title="editingRules ? '修改本场规则' : '新建比赛'"
    form-id="setup-form"
    :confirm-label="editingRules ? '保存本场规则' : '创建比赛'"
    @close="emit('close')"
  >
    <form id="setup-form" @submit.prevent="submitSetup" @invalid.capture="revealInvalidSection">
      <p v-if="!editingRules" class="setup-clock-note">
        打开和关闭此表单不改变当前计时。比赛钟
        <strong data-clock="main"></strong>；需要停表请返回计时台按“裁判暂停”。确认创建才替换当前比赛。
      </p>
      <p>模板可选；套用后所有规则字段均可修改。开赛前可再次修改本场规则。</p>

      <div class="form-grid">
        <label>比赛名称<input v-model="config.title" name="title" maxlength="80" placeholder="SLL 棍网球超级联赛"></label>
        <label>比赛日期<input v-model="config.date" name="date" type="date" required></label>
        <label>主队名称<input v-model="config.home" name="home" maxlength="40" required></label>
        <label>客队名称<input v-model="config.away" name="away" maxlength="40" required></label>
      </div>

      <label v-if="!editingRules">
        比赛组别
        <select v-model="config.ageGroup" name="ageGroup" required @change="onAgeGroupChange">
          <option value="">请选择组别</option>
          <option v-for="ageGroup in ageGroups" :key="ageGroup.itemValue" :value="ageGroup.itemValue">
            {{ ageGroup.itemLabel }}
          </option>
        </select>
      </label>

      <label v-if="!editingRules">
        规则模板
        <select id="setup-preset" v-model="config.groupRuleId" name="groupRuleId" @change="onTemplateChange">
          <option value="">不选模板，本场自定义</option>
          <option v-for="rule in availableTemplates" :key="rule.id" :value="rule.id">
            {{ rule.templateName || `${rule.ageGroup} 模板` }}
          </option>
        </select>
      </label>

      <label v-if="!editingRules" class="checkbox">
        <input v-model="config.saveAsTemplate" name="saveAsTemplate" type="checkbox">
        将当前规则另存为可复用模板
      </label>
      <label v-if="!editingRules && config.saveAsTemplate">
        模板名称
        <input v-model.trim="config.templateName" name="templateName" maxlength="80" required placeholder="例如：U12 淘汰赛模板">
      </label>

      <div class="form-grid">
        <label>
          比赛阶段
          <select v-model="config.stage" name="stage">
            <option value="league">常规赛</option>
            <option value="knockout">淘汰／名次赛</option>
          </select>
        </label>
        <label v-if="showGender" id="gender-field">
          U12 性别组
          <select v-model="config.gender" name="gender" required>
            <option value="">请选择</option>
            <option value="boys">男子组</option>
            <option value="girls">女子组</option>
          </select>
        </label>
      </div>

      <div id="preset-preview" class="preset-preview" aria-live="polite">{{ preview }}</div>
      <button v-if="config.groupRuleId" type="button" class="text-button" @click="resetPreset">恢复所选模板默认值</button>

      <details
        v-for="(group, index) in ruleGroups"
        :key="group.name"
        class="rule-field-group"
        :open="index === 0"
      >
        <summary>{{ group.name }}</summary>
        <div class="form-grid">
          <label v-for="field in group.fields" :key="field.key">
            {{ field.label }}
            <select v-if="field.options" v-model="ruleValues[field.key]" :name="`rule_${field.key}`">
              <option v-for="([value, label]) in field.options" :key="value" :value="value">{{ label }}</option>
            </select>
            <input
              v-else
              v-model="ruleValues[field.key]"
              :name="`rule_${field.key}`"
              type="number"
              required
              :min="field.min"
              :max="field.max"
              :step="field.step"
            >
          </label>
        </div>
      </details>

      <label v-if="!editingRules && match.events.length" class="checkbox">
        <input v-model="config.confirm" name="confirm" type="checkbox" required>
        替换当前比赛，重新开始
      </label>
      <p v-if="errorMessage" ref="errorElement" class="form-error error" role="alert" tabindex="-1">{{ errorMessage }}</p>
    </form>
  </BaseDialog>
</template>
