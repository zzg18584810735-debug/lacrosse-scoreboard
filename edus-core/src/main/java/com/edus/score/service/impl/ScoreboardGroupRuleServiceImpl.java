package com.edus.score.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edus.common.exception.BusinessException;
import com.edus.score.entity.ScoreboardGroupRule;
import com.edus.score.entity.ScoreboardRuleFields;
import com.edus.score.mapper.ScoreboardGroupRuleMapper;
import com.edus.score.service.ScoreboardDictionaryService;
import com.edus.score.service.ScoreboardGroupRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ScoreboardGroupRuleServiceImpl implements ScoreboardGroupRuleService {
    private final ScoreboardGroupRuleMapper groupRuleMapper;
    private final ScoreboardDictionaryService dictionaryService;

    @Override
    @Transactional(readOnly = true)
    public List<ScoreboardGroupRule> listEnabled() {
        return groupRuleMapper.selectList(new LambdaQueryWrapper<ScoreboardGroupRule>()
                .eq(ScoreboardGroupRule::getEnabled, 1)
                .orderByAsc(ScoreboardGroupRule::getAgeGroup)
                .orderByAsc(ScoreboardGroupRule::getTemplateName));
    }

    @Override
    public ScoreboardGroupRule saveRule(ScoreboardGroupRule rule) {
        validate(rule);
        ensureTemplateUnique(rule.getAgeGroup(), rule.getTemplateName(), null);
        if (rule.getEnabled() == null) {
            rule.setEnabled(1);
        }
        groupRuleMapper.insert(rule);
        return rule;
    }

    @Override
    public ScoreboardGroupRule updateRule(Long id, ScoreboardGroupRule rule) {
        ScoreboardGroupRule current = requireRule(id);
        validate(rule);
        ensureTemplateUnique(rule.getAgeGroup(), rule.getTemplateName(), id);
        rule.setId(id);
        if (rule.getEnabled() == null) {
            rule.setEnabled(current.getEnabled());
        }
        groupRuleMapper.updateById(rule);
        return groupRuleMapper.selectById(id);
    }

    private ScoreboardGroupRule requireRule(Long id) {
        ScoreboardGroupRule rule = groupRuleMapper.selectById(id);
        if (rule == null) {
            throw BusinessException.dataNotFound();
        }
        return rule;
    }

    private void ensureTemplateUnique(String ageGroup, String templateName, Long excludeId) {
        LambdaQueryWrapper<ScoreboardGroupRule> wrapper = new LambdaQueryWrapper<ScoreboardGroupRule>()
                .eq(ScoreboardGroupRule::getAgeGroup, ageGroup)
                .eq(ScoreboardGroupRule::getTemplateName, templateName)
                .ne(excludeId != null, ScoreboardGroupRule::getId, excludeId);
        if (groupRuleMapper.selectCount(wrapper) > 0) {
            throw BusinessException.validationError("该组别下已存在同名规则模板");
        }
    }

    private void validate(ScoreboardGroupRule rule) {
        if (rule == null || StrUtil.isBlank(rule.getAgeGroup()) || StrUtil.isBlank(rule.getTemplateName())) {
            throw BusinessException.validationError("组别编码和模板名称不能为空");
        }
        rule.setAgeGroup(StrUtil.trim(rule.getAgeGroup()));
        rule.setTemplateName(StrUtil.trim(rule.getTemplateName()));
        rule.setAgeGroup(dictionaryService.requireEnabledAgeGroup(rule.getAgeGroup()));
        if (rule.getAgeGroup().length() > 16 || rule.getTemplateName().length() > 80) {
            throw BusinessException.validationError("组别编码或模板名称过长");
        }
        if (rule.getPlayersPerTeam() == null || rule.getPlayersPerTeam() <= 0
                || rule.getRegularPeriods() == null || rule.getRegularPeriods() <= 0
                || rule.getPeriodMinutes() == null || rule.getPeriodMinutes() <= 0
                || rule.getHalftimeAfterPeriod() == null || rule.getHalftimeAfterPeriod() < 0
                || rule.getPointsPerGoal() == null || rule.getPointsPerGoal() <= 0
                || rule.getTimeoutSeconds() == null || rule.getTimeoutSeconds() <= 0
                || rule.getOvertimeMinutes() == null || rule.getOvertimeMinutes() < 0
                || rule.getMinorPenaltySeconds() == null || rule.getMinorPenaltySeconds() < 0
                || rule.getMajorPenaltySeconds() == null || rule.getMajorPenaltySeconds() < 0
                || rule.getExpulsionPenaltySeconds() == null || rule.getExpulsionPenaltySeconds() < 0
                || rule.getMajorLimit() == null || rule.getMajorLimit() <= 0) {
            throw BusinessException.validationError("组别规则缺少有效的基础数值");
        }
        if (!List.of("always", "knockout", "none").contains(rule.getOvertimePolicy())) {
            throw BusinessException.validationError("加时规则不正确");
        }
        if (!List.of("manual", "home", "away").contains(rule.getAlternateFirst())) {
            throw BusinessException.validationError("首次交替球权规则不正确");
        }
        validateBooleans(rule);
    }

    private void validateBooleans(ScoreboardRuleFields rule) {
        validateBoolean(rule.getAllDeadStop(), "普通阶段停表规则");
        validateBoolean(rule.getFoulStop(), "犯规停表规则");
        validateBoolean(rule.getOvertimeStopAll(), "加时停表规则");
        validateBoolean(rule.getShotLastOff(), "末段进攻钟规则");
        validateBoolean(rule.getGoldenGoal(), "金球规则");
        validateBoolean(rule.getGoalieProxy(), "守门员代罚规则");
        validateBoolean(rule.getPassRequired(), "接传规则");
        validateBoolean(rule.getAlternateReset(), "交替球权重置规则");
    }

    private void validateBoolean(Integer value, String fieldName) {
        if (value == null || (value != 0 && value != 1)) {
            throw BusinessException.validationError(fieldName + "只能为0或1");
        }
    }
}
