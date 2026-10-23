package com.edus.score.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreboardDictItemVO {
    private String itemValue;
    private String itemLabel;
    private Integer sortNo;
}
