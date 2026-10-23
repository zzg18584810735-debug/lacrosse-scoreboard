package com.edus.score.dto.res;

import com.edus.score.entity.ScoreboardMatchEvent;
import com.edus.score.entity.ScoreboardMatchPenalty;
import com.edus.score.entity.ScoreboardMatchState;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 比赛状态保存结果。
 */
@Data
@AllArgsConstructor
public class MatchStateSaveVO {
    private ScoreboardMatchState state;
    private List<ScoreboardMatchEvent> events;
    private List<ScoreboardMatchPenalty> penalties;
}
