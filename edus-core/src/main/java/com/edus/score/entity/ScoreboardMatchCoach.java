package com.edus.score.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scoreboard_match_coaches")
public class ScoreboardMatchCoach {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long matchId;
    private String teamSide;
    private Long teamCoachRelId;
    private Long personId;
    private String coachType;
    private String personName;
    private Integer sortNo;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
