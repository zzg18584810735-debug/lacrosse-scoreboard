import { get, post, put } from './request.js'

const MATCHES = '/scoreboard/matches'

export const scoreboardApi = {
  listAgeGroups: () => get('/scoreboard/dictionaries/age-groups'),
  listGroupRules: () => get('/scoreboard/group-rules'),
  createGroupRule: data => post('/scoreboard/group-rules', data),
  updateGroupRule: (id, data) => put(`/scoreboard/group-rules/${encode(id)}`, data),

  listMatches: params => get(MATCHES, params),
  createMatch: data => post(MATCHES, data),
  getMatchDetail: matchId => get(`${MATCHES}/${encode(matchId)}`),
  clearMatchRecords: (matchId, data) => post(`${MATCHES}/${encode(matchId)}/records/clear`, data),
  updateMatchInfo: (matchId, data) => put(`${MATCHES}/${encode(matchId)}/info`, data),
  updateMatchRule: (matchId, data) => put(`${MATCHES}/${encode(matchId)}/rule`, data),
  replaceRoster: (matchId, roster) => put(`${MATCHES}/${encode(matchId)}/roster`, roster),
  replaceCoaches: (matchId, coaches) => put(`${MATCHES}/${encode(matchId)}/coaches`, coaches),
  replaceOfficials: (matchId, officials) => put(`${MATCHES}/${encode(matchId)}/officials`, officials),
  saveMatchState: (matchId, data) => put(`${MATCHES}/${encode(matchId)}/state`, data),
  appendMatchEvent: (matchId, data) => post(`${MATCHES}/${encode(matchId)}/events`, data),
  updatePenaltyState: (matchId, penaltyId, data) =>
    put(`${MATCHES}/${encode(matchId)}/penalties/${encode(penaltyId)}`, data),
}

function encode(value) {
  return encodeURIComponent(String(value))
}
