package com.edus.score.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MatchRecordClearDTO {
    @NotNull(message = "状态版本号不能为空")
    private Integer expectedVersion;
}
