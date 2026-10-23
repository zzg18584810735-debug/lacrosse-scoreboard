export function createMatchInfoDraft(match) {
  return {
    title: match.config.title,
    date: match.config.date,
    home: match.config.home,
    away: match.config.away,
  }
}
