package com.edus.score.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MatchInfoUpdateDTO {
    @NotBlank(message = "比赛名称不能为空")
    private String title;
    private String matchNo;
    @NotNull(message = "比赛日期不能为空")
    private LocalDate matchDate;
    private LocalTime matchTime;
    private String venueName;
    @NotBlank(message = "主队名称不能为空")
    private String homeTeamName;
    private String homeTeamColor;
    @NotBlank(message = "客队名称不能为空")
    private String awayTeamName;
    private String awayTeamColor;
}
