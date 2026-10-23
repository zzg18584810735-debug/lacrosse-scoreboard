package com.edus.score.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scoreboard_match_penalties")
public class ScoreboardMatchPenalty {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long matchId;
    private Long penaltyEventId;
    private Integer remainingSeconds;
    private String penaltyState;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
