export function canClearMatchRecords(remoteMatchId) {
  return remoteMatchId !== null
    && remoteMatchId !== undefined
    && String(remoteMatchId).trim() !== ''
}
