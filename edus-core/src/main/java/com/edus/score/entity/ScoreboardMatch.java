package com.edus.score.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("scoreboard_matches")
public class ScoreboardMatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String matchNo;
    private LocalDate matchDate;
    private LocalTime matchTime;
    private String venueName;
    private String ageGroup;
    private Long groupRuleId;
    private String stage;
    private Long homeTeamId;
    private String homeTeamName;
    private String homeTeamColor;
    private Long awayTeamId;
    private String awayTeamName;
    private String awayTeamColor;
    private String status;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime startedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime finishedAt;
    private Integer homeScore;
    private Integer awayScore;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
