package com.edus.score.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 比赛事件类型。
 */
@Getter
@AllArgsConstructor
public enum MatchEventType {
    MATCH_START("比赛开始"),
    PERIOD_START("本节开始"),
    PERIOD_END("本节结束"),
    MATCH_END("比赛结束"),
    GOAL("进球"),
    TEAM_TIMEOUT("球队暂停"),
    PENALTY("判罚"),
    POSSESSION("球权"),
    CORRECTION("更正"),
    SYSTEM("系统记录");

    private final String label;
}
