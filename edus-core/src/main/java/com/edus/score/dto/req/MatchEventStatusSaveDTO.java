package com.edus.score.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MatchEventStatusSaveDTO {
    @NotNull(message = "事件ID不能为空")
    private Long id;
    @NotBlank(message = "事件状态不能为空")
    private String eventStatus;
}
