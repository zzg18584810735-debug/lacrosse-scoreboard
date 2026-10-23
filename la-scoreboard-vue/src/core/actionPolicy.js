import { GOLDEN_CONFLICT_HINT } from './matchOutcome.js'
import { isOvertime, isPeriodActive, lastStartedPeriod, shotOff } from './timing.js'

export const editable = match => match.phase === 'ready'
  && match.period === 1
  && match.events.every(event => event.category === 'match-info')

// 没有实际比赛时仅开放页面级操作，避免占位状态被当作真实比赛修改。
export const actionAllowedWithoutMatch = action => ['setup', 'fullscreen', 'view'].includes(action)

// 不可用原因沿用动作本身的业务前置条件，不额外引入竞赛规则。
export function actionUnavailable(match, action, params = {}) {
  if (match.goldenConflict
    && !['resolve-golden-form', 'undo', 'match-info-form', 'view', 'record-filter', 'setup', 'reset-preset', 'export', 'export-csv', 'export-json', 'fullscreen'].includes(action)) {
    return GOLDEN_CONFLICT_HINT
  }
  const phaseHint = {
    ready: '请先鸣哨开赛。',
    timeout: '请先结束球队暂停，再按裁判指令复赛。',
    break: '请先准备下一节，再鸣哨开赛。',
    'period-end': '请先核对本节比分，再结束比赛或进入休息。',
    finished: '比赛已结束；修改总分请用“赛后更正比分”。',
  }[match.phase] || ''

  switch (action) {
    case 'goal':
      return phaseHint || (!(match.running && match.ball === 'live') ? '当前未复赛；请先鸣哨复赛，漏记请用“补录进球”。' : '')
    case 'backfill-goal-form':
      return lastStartedPeriod(match) < 1 ? '比赛尚未开赛，暂无可补录的节次。' : ''
    case 'resume':
      if (!['ready', 'period'].includes(match.phase)) return phaseHint
      if (match.running && match.ball === 'live') return '比赛正在进行，无需再次鸣哨。'
      if (match.main === 0) return '比赛时间已到；请核对比分，或更正比赛时间。'
      if (!match.periodStarted && !match.possession) return '请先确认本节开场球权。'
      return match.possession && !shotOff(match) && match.shot === 0 && !match.resetOnResume
        ? '进攻时间已到；请按裁判指令换球权、重置或更正进攻时间。'
        : ''
    case 'pause':
      if (match.phase !== 'period' || !match.periodStarted) return phaseHint || '请先鸣哨开赛。'
      return match.running ? '' : '比赛钟已经暂停，请鸣哨复赛。'
    case 'penalty-form':
      return match.phase === 'period' && match.periodStarted ? '' : phaseHint || '请先鸣哨开赛。'
    case 'timeout':
      if (match.timeouts[params.team] === 0) {
        return `${isOvertime(match) ? '本节加时' : '本半场'}已无可用暂停；误记请在“比赛记录”更正／作废暂停。`
      }
      return (isPeriodActive(match) && match.periodStarted === true)
        || (match.phase === 'break' && match.period === match.rules.halftimeAfter)
        ? ''
        : phaseHint || '请先鸣哨开赛。'
    case 'possession':
      return ['ready', 'period'].includes(match.phase) ? '' : phaseHint
    case 'wide':
      return phaseHint
    case 'whistle':
      return phaseHint || (match.ball !== 'live' ? '已经是死球，无需重复记录；恢复比赛请鸣哨复赛。' : '')
    case 'reset-shot':
      return phaseHint || (!match.possession ? '请先记录获得球权的球队，再重置进攻钟。' : '')
    case 'backcourt':
      return phaseHint || (match.ball !== 'live'
        ? '请先鸣哨复赛，再记录后场控球。'
        : !match.possession ? '请先记录获得球权的球队。' : '')
    case 'frontcourt':
      return phaseHint || (!match.possession ? '请先记录获得球权的球队。' : '')
    case 'alternate-award':
      return phaseHint || (!match.alternate ? '请先按裁判指令确认首次交替球权。' : '')
    case 'alternate-set':
      return ['ready', 'period', 'break'].includes(match.phase) ? '' : phaseHint
    case 'prepare':
      return params.carry === 'true' && !match.possession ? '没有可延续的球权；请选择中心开球／重新争球。' : ''
    case 'auxiliary':
      return match.auxiliary === 0 ? `倒计时已到；请${match.phase === 'timeout' ? '结束暂停' : '按裁判指令准备下一节'}。` : ''
    case 'undo':
      return match.undo ? '' : '没有可撤销的上一步（刷新后不保留）；记错内容请使用对应更正入口。'
    case 'edit-rules':
      return editable(match) ? '' : '已记录比赛操作；时间／比分用“比赛更正”，规则变更请新建比赛。'
    case 'correct-form':
      return match.phase === 'finished' ? phaseHint : ''
    default:
      return ''
  }
}

export function readCorrection(data) {
  const number = key => data[key] === undefined || String(data[key]).trim() === '' ? NaN : Number(data[key])
  const correction = { scope: data.scope, reason: data.reason }
  if (correction.scope === 'main') {
    const minutes = number('minutes')
    const seconds = number('seconds')
    requirePolicy(Number.isInteger(minutes)
      && minutes >= 0
      && Number.isFinite(seconds)
      && seconds >= 0
      && seconds < 60, '请填写非负整数分钟和 0–59.999 秒。')
    correction.main = Math.round(minutes * 60000 + seconds * 1000)
  } else if (correction.scope === 'shot') {
    correction.shot = number('shot') * 1000
  } else if (correction.scope === 'score') {
    correction.home = number('home')
    correction.away = number('away')
  } else if (correction.scope === 'penalty') {
    correction.penaltyId = data.penaltyId
    correction.penaltySeconds = number('penaltySeconds')
  } else {
    throw new Error('请选择要更正的项目。')
  }
  return correction
}

function requirePolicy(ok, message) {
  if (!ok) throw new Error(message)
}
