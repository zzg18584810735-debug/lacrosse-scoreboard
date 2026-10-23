package com.edus.score.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MatchRosterDTO {
    @NotBlank(message = "请选择主队或客队")
    private String teamSide;
    private Long teamPlayerId;
    @NotBlank(message = "球员姓名不能为空")
    private String playerName;
    @Min(value = 0, message = "球衣号码不能小于0")
    @Max(value = 999, message = "球衣号码不能大于999")
    private Integer jerseyNo;
    private String positionName;
}
