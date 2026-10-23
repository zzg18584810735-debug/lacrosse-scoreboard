package com.edus.score.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 随比赛状态一起保存的现有罚时。
 */
@Data
public class MatchPenaltySaveDTO {
    @NotNull(message = "罚时ID不能为空")
    private Long id;
    @NotNull(message = "剩余罚时不能为空")
    @Min(value = 0, message = "剩余罚时不能小于0")
    private Integer remainingSeconds;
    @NotBlank(message = "罚时状态不能为空")
    private String penaltyState;
}
