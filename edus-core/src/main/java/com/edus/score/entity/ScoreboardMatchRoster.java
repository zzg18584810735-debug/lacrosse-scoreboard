package com.edus.score.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scoreboard_match_roster")
public class ScoreboardMatchRoster {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long matchId;
    private String teamSide;
    private Long teamPlayerId;
    private String playerName;
    private Integer jerseyNo;
    private String positionName;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
