package com.edus.score.dto.res;

import com.edus.score.entity.*;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MatchDetailVO {
    private ScoreboardMatch match;
    private ScoreboardMatchRule rule;
    private ScoreboardMatchState state;
    private List<ScoreboardMatchRoster> roster;
    private List<ScoreboardMatchEvent> events;
    private List<ScoreboardMatchPenalty> penalties;
    private List<ScoreboardMatchCoach> coaches;
    private List<ScoreboardMatchOfficial> officials;
}
