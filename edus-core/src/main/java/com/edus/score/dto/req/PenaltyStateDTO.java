package com.edus.score.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PenaltyStateDTO {
    @NotNull(message = "状态版本号不能为空")
    private Integer expectedVersion;
    @NotNull(message = "剩余罚时不能为空")
    @Min(value = 0, message = "剩余罚时不能小于0")
    private Integer remainingSeconds;
    @NotBlank(message = "罚时状态不能为空")
    private String penaltyState;
}
