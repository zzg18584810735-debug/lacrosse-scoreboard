package com.edus.score.service;

import com.edus.score.entity.ScoreboardGroupRule;

import java.util.List;

public interface ScoreboardGroupRuleService {
    List<ScoreboardGroupRule> listEnabled();

    ScoreboardGroupRule saveRule(ScoreboardGroupRule rule);

    ScoreboardGroupRule updateRule(Long id, ScoreboardGroupRule rule);
}
