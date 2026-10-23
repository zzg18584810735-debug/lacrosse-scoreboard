package com.edus.score.service;

import com.edus.score.dto.req.*;
import com.edus.score.dto.res.MatchDetailVO;
import com.edus.score.dto.res.MatchOperationVO;
import com.edus.score.dto.res.MatchStateSaveVO;
import com.edus.score.entity.ScoreboardMatch;
import com.edus.score.entity.ScoreboardMatchEvent;
import com.edus.score.entity.ScoreboardMatchPenalty;

import java.time.LocalDate;
import java.util.List;

public interface ScoreboardMatchService {
    Long createMatch(MatchCreateDTO request);

    List<ScoreboardMatch> listMatches(LocalDate matchDate, String status);

    MatchDetailVO getDetail(Long matchId);

    void clearMatchRecords(Long matchId, MatchRecordClearDTO request);

    void updateMatchInfo(Long matchId, MatchInfoUpdateDTO request);

    void updateMatchRule(Long matchId, ScoreboardRuleDTO request);

    void replaceRoster(Long matchId, List<MatchRosterDTO> roster);

    void replaceCoaches(Long matchId, List<MatchCoachDTO> coaches);

    void replaceOfficials(Long matchId, List<MatchOfficialDTO> officials);

    MatchStateSaveVO saveState(Long matchId, MatchStateSaveDTO request);

    MatchOperationVO<ScoreboardMatchEvent> appendEvent(Long matchId, MatchEventCreateDTO request);

    MatchOperationVO<ScoreboardMatchPenalty> updatePenaltyState(Long matchId, Long penaltyId, PenaltyStateDTO request);
}
