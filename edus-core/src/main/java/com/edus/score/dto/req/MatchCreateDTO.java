package com.edus.score.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MatchCreateDTO {
    @NotBlank(message = "比赛名称不能为空")
    private String title;
    private String matchNo;
    @NotNull(message = "比赛日期不能为空")
    private LocalDate matchDate;
    private LocalTime matchTime;
    private String venueName;
    private Long groupRuleId;
    @NotBlank(message = "请选择比赛组别")
    private String ageGroup;
    private Boolean saveAsTemplate;
    private String templateName;
    private String stage;
    private Long homeTeamId;
    @NotBlank(message = "主队名称不能为空")
    private String homeTeamName;
    private String homeTeamColor;
    private Long awayTeamId;
    @NotBlank(message = "客队名称不能为空")
    private String awayTeamName;
    private String awayTeamColor;
    private Long createdBy;
    @Valid
    private ScoreboardRuleDTO rules;
    @Valid
    private List<MatchRosterDTO> roster = new ArrayList<>();
    @Valid
    private List<MatchCoachDTO> coaches = new ArrayList<>();
    @Valid
    private List<MatchOfficialDTO> officials = new ArrayList<>();
}
