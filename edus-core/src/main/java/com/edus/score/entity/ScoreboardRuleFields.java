package com.edus.score.entity;

import lombok.Data;

/**
 * 规则模板与本场规则快照共用字段。
 */
@Data
public class ScoreboardRuleFields {
    private Integer playersPerTeam;
    private Integer regularPeriods;
    private Integer periodMinutes;
    private Integer halftimeAfterPeriod;
    private Integer shotClockSeconds;
    private Integer clearSeconds;
    private Integer pointsPerGoal;
    private Integer periodBreakMinutes;
    private Integer halftimeMinutes;
    private Integer timeoutSeconds;
    private Integer timeoutsPerHalf;
    private Integer overtimeTimeouts;
    private Integer stopWindowSeconds;
    private Integer allDeadStop;
    private Integer foulStop;
    private Integer overtimeStopAll;
    private Integer shotLastOff;
    private String overtimePolicy;
    private Integer overtimeMinutes;
    private Integer goldenGoal;
    private Integer overtimeBreakMinutes;
    private Integer minorPenaltySeconds;
    private Integer majorPenaltySeconds;
    private Integer expulsionPenaltySeconds;
    private Integer majorLimit;
    private Integer goalieProxy;
    private Integer passRequired;
    private String alternateFirst;
    private Integer alternateReset;
}
