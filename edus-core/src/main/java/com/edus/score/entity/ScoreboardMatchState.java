package com.edus.score.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scoreboard_match_state")
public class ScoreboardMatchState {
    @TableId(type = IdType.INPUT)
    private Long matchId;
    private String phase;
    private Integer currentPeriod;
    private Integer mainRemainingSeconds;
    private Integer shotRemainingSeconds;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer clearRemainingSeconds;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String auxiliaryType;
    private Integer auxiliaryRemainingSeconds;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long timeoutEventId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String timeoutReturnPhase;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String timeoutReturnAuxiliaryType;
    private Integer timeoutReturnAuxiliarySeconds;
    private Integer timeoutReturnAuxiliaryRunning;
    private Integer mainRunning;
    private Integer auxiliaryRunning;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime timerStartedAt;
    private String ballState;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String possessionSide;
    private Integer frontcourt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String alternateSide;
    private Integer resetShotOnResume;
    private Integer goldenGoal;
    private Integer goldenConflict;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer pendingGoalSequenceNo;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String pendingGoalSide;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer pendingGoalPeriod;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer pendingGoalRemainingSeconds;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long pendingGoalRosterId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long pendingGoalAssistRosterId;
    private Integer homeTimeoutsRemaining;
    private Integer awayTimeoutsRemaining;
    @Version
    private Integer stateVersion;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
