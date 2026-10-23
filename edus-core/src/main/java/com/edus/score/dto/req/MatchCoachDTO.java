package com.edus.score.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MatchCoachDTO {
    @NotBlank(message = "请选择教练所属球队")
    private String teamSide;
    private Long teamCoachRelId;
    private Long personId;
    @NotBlank(message = "请选择教练类型")
    private String coachType;
    @NotBlank(message = "教练姓名不能为空")
    private String personName;
    private Integer sortNo;
}
