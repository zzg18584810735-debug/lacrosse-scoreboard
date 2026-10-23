package com.edus.score.util;

import com.edus.score.entity.ScoreboardMatch;
import com.edus.score.entity.ScoreboardMatchEvent;
import com.edus.score.entity.ScoreboardMatchRoster;
import com.edus.score.enums.MatchEventType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchEventContentUtilTest {

    @Test
    void matchStartIncludesOpeningPossessionTeam() {
        ScoreboardMatchEvent event = new ScoreboardMatchEvent();
        event.setTeamSide("away");

        assertEquals("比赛开始，客队获得开场球权。",
                MatchEventContentUtil.build(MatchEventType.MATCH_START, event, match(),
                        null, null, null, null));
    }
    @Test
    void buildsGoalSnapshotFromStructuredFields() {
        ScoreboardMatch match = match();
        ScoreboardMatchEvent event = new ScoreboardMatchEvent();
        event.setTeamSide("home");
        event.setPoints(1);
        ScoreboardMatchRoster scorer = player(8, "张三");
        ScoreboardMatchRoster assist = player(12, "李四");

        assertEquals("主队 8号张三进球，助攻：12号李四，增加1分。",
                MatchEventContentUtil.build(MatchEventType.GOAL, event, match,
                        scorer, assist, null, null));
        assertEquals("主队进球", MatchEventContentUtil.title(MatchEventType.GOAL, event, match, null));
    }

    @Test
    void buildsGoalSnapshotFromManualPlayerTextWhenRosterIsMissing() {
        ScoreboardMatchEvent event = new ScoreboardMatchEvent();
        event.setTeamSide("home");
        event.setSubjectName("8号张三");
        event.setAssistName("12号李四");
        event.setPoints(1);

        assertEquals("主队 8号张三进球，助攻：12号李四，增加1分。",
                MatchEventContentUtil.build(MatchEventType.GOAL, event, match(),
                        null, null, null, null));
    }

    @Test
    void buildsPenaltySnapshotFromStructuredFields() {
        ScoreboardMatchEvent event = new ScoreboardMatchEvent();
        event.setTeamSide("away");
        event.setPenaltySubject("goalie");
        event.setPenaltyType("major");
        event.setPenaltySeconds(60);
        event.setFoulItem("非法身体接触");
        event.setRemainingSeconds(300);

        assertEquals("客队 1号王五被判黄牌，罚时60秒，由9号赵六服罚，犯规项：非法身体接触。计时停止于 05:00。",
                MatchEventContentUtil.build(MatchEventType.PENALTY, event, match(),
                        player(1, "王五"), null, player(9, "赵六"), null));
    }

    @Test
    void buildsPenaltySnapshotFromManualPlayerTextWhenRosterIsMissing() {
        ScoreboardMatchEvent event = new ScoreboardMatchEvent();
        event.setTeamSide("away");
        event.setPenaltySubject("player");
        event.setPenaltyType("major");
        event.setPenaltySeconds(60);
        event.setSubjectName("临时7号");
        event.setServedByName("临时8号");
        event.setRemainingSeconds(290);

        assertEquals("客队 临时7号被判黄牌，罚时60秒，由临时8号服罚。计时停止于 04:50。",
                MatchEventContentUtil.build(MatchEventType.PENALTY, event, match(),
                        null, null, null, null));
    }

    @Test
    void timeoutTitleAndDescriptionIncludeConfiguredDuration() {
        ScoreboardMatchEvent event = new ScoreboardMatchEvent();
        event.setTeamSide("home");
        event.setTimeoutCondition("FRONTCOURT");
        event.setDurationSeconds(60);

        assertEquals("主队暂停（60秒）",
                MatchEventContentUtil.title(MatchEventType.TEAM_TIMEOUT, event, match(), null));
        assertEquals("主队在前场控球条件下使用60秒球队暂停。",
                MatchEventContentUtil.build(MatchEventType.TEAM_TIMEOUT, event, match(),
                        null, null, null, null));
    }

    @Test
    void systemDescriptionGetsChinesePeriod() {
        assertEquals("计分台恢复完成。",
                MatchEventContentUtil.build(MatchEventType.SYSTEM, new ScoreboardMatchEvent(),
                        match(), null, null, null, "计分台恢复完成"));
        assertEquals("恢复计时", MatchEventContentUtil.title(MatchEventType.SYSTEM,
                new ScoreboardMatchEvent(), match(), "恢复计时"));
    }

    private ScoreboardMatch match() {
        ScoreboardMatch match = new ScoreboardMatch();
        match.setHomeTeamName("主队");
        match.setAwayTeamName("客队");
        return match;
    }

    private ScoreboardMatchRoster player(int jerseyNo, String name) {
        ScoreboardMatchRoster roster = new ScoreboardMatchRoster();
        roster.setJerseyNo(jerseyNo);
        roster.setPlayerName(name);
        return roster;
    }
}
