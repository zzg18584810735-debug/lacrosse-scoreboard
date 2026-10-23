import { actionUnavailable, editable, readCorrection } from '../core/actionPolicy.js'
import { createMatch } from '../core/matchModel.js'
import { exportCsv, exportFilename } from '../core/matchRecords.js'
import { act, requireState } from '../core/scoreboardActions.js'
import { freeze } from '../core/timing.js'
import {
  toMatchCreateRequest,
  toMatchInfoUpdateRequest,
  toScoreboardRuleRequest,
} from '../adapters/matchDetailAdapter.js'

export function createScoreboardController(engine, persistence) {
  function prepareDialog(name) {
    if (name === 'correct') requireState(!persistence.blocked, '请先刷新，读取最新比赛记录。')
    if (name === 'edit-rules') requireState(editable(engine.state), '本场已记录比赛事件；请新建比赛调整规则，或使用裁判更正调整时间与比分。')
    if (name === 'penalty' && engine.state.rules.foulStop) {
      // 判罚停表属于判罚动作本身，打开弹窗时不额外生成“裁判暂停”记录。
      freeze(engine.state)
      engine.commit()
    }
    if (['correct', 'correct-penalty', 'correct-timeout', 'goal-backfill'].includes(name)) {
      freeze(engine.state)
      engine.commit()
    }
    return name === 'edit-rules' ? 'setup' : name
  }

  function performAction(action, params = {}) {
    engine.settle()
    const unavailable = actionUnavailable(engine.state, action, params)
    requireState(!unavailable, unavailable)
    if (action === 'goal') {
      requireState(!persistence.blocked, '请先刷新，读取最新比赛记录。')
      act(engine.state, 'begin-goal', { team: params.team })
      engine.commit()
      return { dialog: 'goal-entry', goalTeam: params.team }
    }
    if (action === 'backfill-goal-form') {
      requireState(!persistence.blocked, '请先刷新，读取最新比赛记录。')
      prepareDialog('goal-backfill')
      return { dialog: 'goal-backfill', goalTeam: 'home' }
    }
    if (action === 'record-filter') return { recordFilter: params.filter }
    if (action === 'timeout') {
      prepareDialog('timeout')
      return { dialog: 'timeout', timeoutTeam: params.team }
    }
    if (action === 'goal-details') {
      prepareDialog('goal-details')
      return { dialog: 'goal-details', goalRecordId: Number(params.id) }
    }
    if (action === 'correct-penalty-form' || action === 'correct-timeout-form') {
      requireState(!persistence.blocked, '请先刷新，读取最新比赛记录。')
      const dialog = action.replace('-form', '')
      prepareDialog(dialog)
      return { dialog, correctionId: Number(params.id) }
    }
    if (action === 'view') return { view: params.view }
    if (['setup', 'edit-rules', 'penalty-form', 'correct-form', 'match-info-form', 'final-score-form', 'resolve-golden-form', 'clear-match-records-form', 'export'].includes(action)) {
      const name = action.replace('-form', '')
      const dialog = prepareDialog(name)
      return {
        dialog,
        editingRules: name === 'edit-rules',
        penaltyDefaults: name === 'penalty' ? { team: params.team || 'home', kind: params.kind || 'major' } : undefined,
      }
    }
    if (action === 'export-csv') return { download: { content: exportCsv(engine.state), filename: exportFilename(engine.state, 'csv'), type: 'text/csv;charset=utf-8' } }
    if (action === 'export-json') return { download: { content: JSON.stringify(engine.state, null, 2), filename: exportFilename(engine.state, 'json'), type: 'application/json' } }
    if (action === 'fullscreen') return { fullscreen: true }
    if (action === 'auxiliary') {
      engine.state.auxiliaryRunning = !engine.state.auxiliaryRunning && engine.state.auxiliary > 0
      engine.commit()
      return undefined
    }
    engine.dispatch(action, { team: params.team, id: params.id, carry: params.carry === true || params.carry === 'true' })
    return action === 'resume' ? { enableAudio: true } : undefined
  }

  async function submitForm(type, data, context = {}) {
    engine.settle()
    if (type === 'clear-match-records') {
      requireState(context.dialog === 'clear-match-records', '该表单已处理，请勿重复提交。')
      requireState(data.confirm === true, '请先确认清空比赛记录。')
      requireState(Boolean(engine.stateStore?.remoteMatchId), '只有服务器比赛可以清空记录。')
      await engine.waitForSync()
      await engine.stateStore.clearRemoteMatchRecords()
      persistence.reset()
      engine.init()
      engine.save()
    } else if (type === 'resolve-golden') {
      requireState(!persistence.blocked, '请先刷新，读取最新比赛记录。')
      requireState(context.dialog === 'resolve-golden', '该表单已处理，请勿重复提交。')
      act(engine.state, 'resolve-golden', { ...data, home: data.home === '' ? NaN : Number(data.home), away: data.away === '' ? NaN : Number(data.away) })
    } else if (type === 'match-info' || type === 'final-score') {
      requireState(!persistence.blocked, '请先刷新，读取最新比赛记录。')
      requireState(context.dialog === type, '该表单已处理，请勿重复提交。')
      if (type === 'match-info' && engine.stateStore?.remoteMatchId) {
        await engine.stateStore.syncMatchInfo(toMatchInfoUpdateRequest(engine.state, data))
        persistence.reset()
        return
      }
      act(engine.state, type, type === 'final-score'
        ? { home: data.home === '' ? NaN : Number(data.home), away: data.away === '' ? NaN : Number(data.away), reason: data.reason }
        : data)
    } else if (type === 'setup') {
      if (!context.editingRules && engine.state.events.length && !data.confirm) throw new Error('请确认替换当前比赛。')
      requireState(data.home.trim() && data.away.trim(), '请填写两队名称。')
      if (context.editingRules && engine.stateStore?.remoteMatchId) {
        await engine.stateStore.syncMatchRule(toScoreboardRuleRequest(data.rules))
        persistence.reset()
        return
      }
      if (!context.editingRules && data.ageGroup && engine.stateStore?.createRemoteMatch) {
        await engine.stateStore.createRemoteMatch(toMatchCreateRequest(data))
        persistence.reset()
        return { matchId: engine.stateStore.remoteMatchId }
      }
      engine.real = createMatch({ title: data.title.trim() || 'SLL 棍网球超级联赛', home: data.home.trim(), away: data.away.trim(), preset: data.preset, stage: data.stage || 'league', date: data.date, gender: data.gender, ageGroup: data.ageGroup, groupRuleId: data.groupRuleId || null, rules: data.rules })
      persistence.reset()
    } else if (type === 'goal-entry') {
      requireState(!persistence.blocked, '请先刷新，读取最新比赛记录。')
      act(engine.state, 'confirm-goal', { id: Number(data.goalId), team: data.team, player: data.player, assist: data.assist })
    } else if (type === 'goal-backfill') {
      requireState(context.dialog === 'goal-backfill', '该表单已处理，请勿重复确认。')
      requireState(!persistence.blocked, '请先刷新，读取最新比赛记录。')
      requireState(Number.isInteger(Number(data.seconds)) && Number(data.seconds) >= 0 && Number(data.seconds) < 60, '秒数须为 0–59。')
      act(engine.state, 'backfill-goal', { ...data, period: Number(data.period), main: Number(data.minutes) * 60000 + Number(data.seconds) * 1000 })
    } else if (type === 'penalty') {
      act(engine.state, 'penalty', data)
    } else if (type === 'record-correction') {
      requireState(!persistence.blocked, '请先刷新，读取最新比赛记录。')
      requireState(['correct-penalty', 'correct-timeout'].includes(context.dialog), '该更正表单已处理，请勿重复提交。')
      act(engine.state, context.dialog, { ...data, id: context.correctionId, remaining: data.remaining === '' ? NaN : Number(data.remaining) * 1000 })
    } else if (type === 'timeout') {
      act(engine.state, 'confirm-timeout', { team: context.timeoutTeam, condition: context.submitterValue })
    } else if (type === 'goal-details') {
      act(engine.state, 'goal-details', { id: context.goalRecordId, player: data.player, assist: data.assist })
    } else if (type === 'correct') {
      requireState(!persistence.blocked, '请先刷新，读取最新比赛记录。')
      requireState(context.dialog === 'correct', '该更正表单已处理，请勿重复提交。')
      act(engine.state, 'correct', readCorrection(data))
    } else {
      throw new Error('未知表单操作。')
    }
    // 表单只有在服务器状态与本次新增事件保存成功后才算提交完成。
    // 本地比赛会立即返回；服务器比赛则等待串行同步队列。
    await engine.commit()
  }

  function savePendingGoal(data) {
    if (engine.state.pendingGoal && !persistence.blocked) {
      Object.assign(engine.state.pendingGoal, { player: data.player, assist: data.assist, team: data.team })
      engine.save()
    }
  }

  function cancelDialog(dialog) {
    engine.settle()
    if (dialog === 'goal-entry' && engine.state.pendingGoal) {
      act(engine.state, 'cancel-goal')
      engine.commit()
    }
  }

  function setGoalDialogTeam(dialog, team) {
    requireState(['home', 'away'].includes(team), '未知球队。')
    if (dialog === 'goal-entry' && engine.state.pendingGoal && !persistence.blocked) {
      engine.state.pendingGoal.team = team
      engine.save()
    }
  }

  return { cancelDialog, performAction, savePendingGoal, setGoalDialogTeam, submitForm }
}
