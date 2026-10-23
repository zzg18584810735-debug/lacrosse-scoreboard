package com.edus.score.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MatchOfficialDTO {
    private Long personId;
    @NotBlank(message = "请选择比赛人员角色")
    private String officialRole;
    @NotBlank(message = "人员姓名不能为空")
    private String personName;
    private Integer sortNo;
}
