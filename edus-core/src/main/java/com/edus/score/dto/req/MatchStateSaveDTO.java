package com.edus.score.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MatchStateSaveDTO {
    @NotNull(message = "状态版本号不能为空")
    private Integer expectedVersion;
    private String matchStatus;
    private Integer homeScore;
    private Integer awayScore;
    private String phase;
    private Integer currentPeriod;
    private Integer mainRemainingSeconds;
    private Integer shotRemainingSeconds;
    private Integer clearRemainingSeconds;
    private String auxiliaryType;
    private Integer auxiliaryRemainingSeconds;
    private Long timeoutEventId;
    private String timeoutReturnPhase;
    private String timeoutReturnAuxiliaryType;
    private Integer timeoutReturnAuxiliarySeconds;
    private Integer timeoutReturnAuxiliaryRunning;
    private Integer mainRunning;
    private Integer auxiliaryRunning;
    private LocalDateTime timerStartedAt;
    private String ballState;
    private String possessionSide;
    private Integer frontcourt;
    private String alternateSide;
    private Integer resetShotOnResume;
    private Integer goldenGoal;
    private Integer goldenConflict;
    private Integer pendingGoalSequenceNo;
    private String pendingGoalSide;
    private Integer pendingGoalPeriod;
    private Integer pendingGoalRemainingSeconds;
    private Long pendingGoalRosterId;
    private Long pendingGoalAssistRosterId;
    private Integer homeTimeoutsRemaining;
    private Integer awayTimeoutsRemaining;
    @Valid
    @Size(max = 20, message = "单次操作最多提交20条事件")
    private List<MatchEventCreateDTO> events = new ArrayList<>();
    @Valid
    @Size(max = 100, message = "单次操作最多同步100条事件状态")
    private List<MatchEventStatusSaveDTO> eventStatuses = new ArrayList<>();
    @Valid
    @Size(max = 100, message = "单次操作最多同步100条罚时")
    private List<MatchPenaltySaveDTO> penalties = new ArrayList<>();
}
