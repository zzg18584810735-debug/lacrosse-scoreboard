package com.edus.score.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scoreboard_match_officials")
public class ScoreboardMatchOfficial {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long matchId;
    private Long personId;
    private String officialRole;
    private String personName;
    private Integer sortNo;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
