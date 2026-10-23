package com.edus.score.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MatchEventCreateDTO {
    /** 独立新增事件时必填；随状态一起提交时使用外层版本号。 */
    private Integer expectedVersion;
    @NotBlank(message = "事件类型不能为空")
    private String eventType;
    /** SYSTEM、CORRECTION 事件由前端提交业务标题，其他类型由后端按结构化字段生成。 */
    @Size(max = 100, message = "事件标题不能超过100个字符")
    private String eventTitle;
    private Integer periodNo;
    private Integer remainingSeconds;
    private Integer durationSeconds;
    private LocalDateTime occurredAt;
    private String teamSide;
    private Long rosterId;
    @Size(max = 100, message = "人员姓名不能超过100个字符")
    private String subjectName;
    private Long assistRosterId;
    @Size(max = 100, message = "助攻球员不能超过100个字符")
    private String assistName;
    private Integer points;
    private String timeoutBucket;
    private String timeoutCondition;
    private String penaltySubject;
    private String penaltyType;
    private Integer penaltySeconds;
    private Long servedByRosterId;
    @Size(max = 100, message = "服罚球员不能超过100个字符")
    private String servedByName;
    @Size(max = 100, message = "犯规项不能超过100个字符")
    private String foulItem;
    private Long relatedEventId;
    private String eventStatus;
    @Size(max = 255, message = "更正前内容不能超过255个字符")
    private String beforeValue;
    @Size(max = 255, message = "更正后内容不能超过255个字符")
    private String afterValue;
    @Size(max = 200, message = "更正依据不能超过200个字符")
    private String correctionReason;
    /** SYSTEM 事件的业务说明；判罚事件可传“计时停止”作为停表说明。 */
    @Size(max = 480, message = "事件说明不能超过480个字符")
    private String description;
    /** 事件发生时实际采用的规则条款或本场规则来源。 */
    @Size(max = 100, message = "规则依据不能超过100个字符")
    private String ruleReference;
    private Long createdBy;
    private Integer penaltyRemainingSeconds;
    private String penaltyState;
}
