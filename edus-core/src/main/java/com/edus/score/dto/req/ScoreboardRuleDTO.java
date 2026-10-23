package com.edus.score.dto.req;

import com.edus.score.entity.ScoreboardRuleFields;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 本场规则覆盖项；字段为空时沿用组别默认值。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScoreboardRuleDTO extends ScoreboardRuleFields {
}
