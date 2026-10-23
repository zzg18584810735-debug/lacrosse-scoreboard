import { matchRecords } from '../../core/matchRecords.js'
import { createPenaltyDraft } from './penaltyDialog.js'

export function findCorrectionEvent(match, type, correctionId) {
  const category = type === 'penalty' ? 'penalty' : 'timeout'
  return matchRecords(match, category).find(event => event.id === Number(correctionId) && !event.voided)
}

export function createRecordCorrectionDraft(match, type, correctionId) {
  const event = findCorrectionEvent(match, type, correctionId)
  if (!event) return null

  if (type === 'timeout') {
    return {
      mode: 'edit',
      team: event.team,
      correctionReason: '',
    }
  }

  const livePenalty = match.penalties.find(penalty => penalty.id === event.id)
  return {
    mode: 'edit',
    ...createPenaltyDraft({ ...event, ...livePenalty, reason: event.reason || '' }),
    remaining: livePenalty ? Math.ceil(livePenalty.remaining) / 1000 : 0,
    correctionReason: '',
  }
}
