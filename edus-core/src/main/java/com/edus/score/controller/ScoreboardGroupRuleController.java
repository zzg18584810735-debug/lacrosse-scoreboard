package com.edus.score.controller;

import com.edus.common.utils.Result;
import com.edus.score.entity.ScoreboardGroupRule;
import com.edus.score.service.ScoreboardGroupRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 计分器规则模板接口。
 */
@Validated
@RestController
@RequestMapping("/scoreboard/group-rules")
@RequiredArgsConstructor
public class ScoreboardGroupRuleController {
    private final ScoreboardGroupRuleService groupRuleService;

    @GetMapping
    public Result<List<ScoreboardGroupRule>> listEnabled() {
        return Result.ok(groupRuleService.listEnabled());
    }

    @PostMapping
    public Result<ScoreboardGroupRule> create(@Valid @RequestBody ScoreboardGroupRule rule) {
        return Result.ok("规则模板创建成功", groupRuleService.saveRule(rule));
    }

    @PutMapping("/{id}")
    public Result<ScoreboardGroupRule> update(@PathVariable Long id,
                                               @Valid @RequestBody ScoreboardGroupRule rule) {
        return Result.ok("规则模板修改成功", groupRuleService.updateRule(id, rule));
    }
}
