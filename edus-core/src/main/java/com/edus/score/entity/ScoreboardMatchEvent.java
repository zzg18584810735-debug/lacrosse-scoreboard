package com.edus.score.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scoreboard_match_events")
public class ScoreboardMatchEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long matchId;
    private Integer sequenceNo;
    private String eventType;
    private String eventTitle;
    private Integer periodNo;
    private Integer remainingSeconds;
    private Integer durationSeconds;
    private LocalDateTime occurredAt;
    private String teamSide;
    private Long rosterId;
    private String subjectName;
    private Long assistRosterId;
    private String assistName;
    private Integer points;
    private String timeoutBucket;
    private String timeoutCondition;
    private String penaltySubject;
    private String penaltyType;
    private Integer penaltySeconds;
    private Long servedByRosterId;
    private String servedByName;
    private String foulItem;
    private Long relatedEventId;
    private String eventStatus;
    private String beforeValue;
    private String afterValue;
    private String correctionReason;
    private String eventText;
    private String ruleReference;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
