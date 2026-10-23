package com.edus.score.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edus.common.exception.BusinessException;
import com.edus.score.dto.req.*;
import com.edus.score.dto.res.MatchDetailVO;
import com.edus.score.dto.res.MatchOperationVO;
import com.edus.score.dto.res.MatchStateSaveVO;
import com.edus.score.entity.*;
import com.edus.score.enums.MatchEventType;
import com.edus.score.mapper.*;
import com.edus.score.service.ScoreboardDictionaryService;
import com.edus.score.service.ScoreboardMatchService;
import com.edus.score.util.MatchEventContentUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ScoreboardMatchServiceImpl implements ScoreboardMatchService {
    private static final CopyOptions IGNORE_NULL = CopyOptions.create().setIgnoreNullValue(true);
    private static final List<String> TEAM_SIDES = List.of("home", "away");
    private static final List<String> MATCH_STATUSES = List.of("READY", "ONGOING", "FINISHED");
    private static final List<String> PHASES = List.of("READY", "PERIOD", "PERIOD_END", "BREAK", "TIMEOUT", "FINISHED");
    private static final List<String> EVENT_STATUSES = List.of("VALID", "CANCELLED", "REPLACED");
    private static final List<String> PENALTY_STATES = List.of("WAITING", "SEATED", "ACTIVE", "EXPIRED", "RELEASED");
    private static final List<String> PENALTY_SUBJECTS = List.of("player", "goalie", "official");
    private static final List<String> PENALTY_TYPES = List.of("turnover", "minor", "major", "expulsion");
    private static final Map<String, List<String>> PHASE_TRANSITIONS = Map.of(
            "READY", List.of("READY", "PERIOD", "TIMEOUT"),
            "PERIOD", List.of("PERIOD", "PERIOD_END", "TIMEOUT"),
            "PERIOD_END", List.of("PERIOD_END", "BREAK", "FINISHED"),
            "BREAK", List.of("BREAK", "READY", "TIMEOUT"),
            "TIMEOUT", List.of("TIMEOUT", "READY", "PERIOD", "BREAK"),
            "FINISHED", List.of("FINISHED", "PERIOD", "PERIOD_END")
    );

    private final ScoreboardGroupRuleMapper groupRuleMapper;
    private final ScoreboardDictionaryService dictionaryService;
    private final ScoreboardMatchMapper matchMapper;
    private final ScoreboardMatchRuleMapper matchRuleMapper;
    private final ScoreboardMatchStateMapper stateMapper;
    private final ScoreboardMatchRosterMapper rosterMapper;
    private final ScoreboardMatchEventMapper eventMapper;
    private final ScoreboardMatchPenaltyMapper penaltyMapper;
    private final ScoreboardMatchCoachMapper coachMapper;
    private final ScoreboardMatchOfficialMapper officialMapper;

    @Override
    public Long createMatch(MatchCreateDTO request) {
        String ageGroup = dictionaryService.requireEnabledAgeGroup(request.getAgeGroup());
        ScoreboardGroupRule groupRule = null;
        if (request.getGroupRuleId() != null) {
            groupRule = groupRuleMapper.selectById(request.getGroupRuleId());
            if (groupRule == null || !Integer.valueOf(1).equals(groupRule.getEnabled())) {
                throw BusinessException.validationError("所选规则模板不存在或已停用");
            }
            if (!ageGroup.equals(groupRule.getAgeGroup())) {
                throw BusinessException.validationError("规则模板与比赛组别不一致");
            }
        }
        validateStage(request.getStage());

        ScoreboardMatchRule rule = new ScoreboardMatchRule();
        if (groupRule != null) {
            BeanUtil.copyProperties(groupRule, rule);
        }
        if (request.getRules() != null) {
            BeanUtil.copyProperties(request.getRules(), rule, IGNORE_NULL);
        }
        if (groupRule == null && request.getRules() == null) {
            throw BusinessException.validationError("自定义规则必须填写完整规则内容");
        }
        validateRule(rule);

        boolean saveAsTemplate = Boolean.TRUE.equals(request.getSaveAsTemplate());
        if (saveAsTemplate) {
            groupRule = createRuleTemplate(ageGroup, request.getTemplateName(), rule);
        }

        ScoreboardMatch match = new ScoreboardMatch();
        BeanUtil.copyProperties(request, match);
        match.setAgeGroup(ageGroup);
        match.setGroupRuleId(groupRule == null ? null : groupRule.getId());
        match.setStage(StrUtil.blankToDefault(request.getStage(), "league"));
        match.setStatus("READY");
        match.setHomeScore(0);
        match.setAwayScore(0);
        match.setDeleted(0);
        matchMapper.insert(match);

        rule.setMatchId(match.getId());
        rule.setIsCustomized(saveAsTemplate ? 0 : groupRule == null
                ? 1 : isRuleCustomized(groupRule, request.getRules()) ? 1 : 0);
        matchRuleMapper.insert(rule);

        ScoreboardMatchState state = initialState(match.getId(), rule);
        stateMapper.insert(state);

        if (CollUtil.isNotEmpty(request.getRoster())) {
            replaceRoster(match.getId(), request.getRoster());
        }
        if (CollUtil.isNotEmpty(request.getCoaches())) {
            replaceCoaches(match.getId(), request.getCoaches());
        }
        if (CollUtil.isNotEmpty(request.getOfficials())) {
            replaceOfficials(match.getId(), request.getOfficials());
        }
        return match.getId();
    }

    private ScoreboardGroupRule createRuleTemplate(String ageGroup, String templateName,
                                                    ScoreboardMatchRule rule) {
        String name = StrUtil.trim(templateName);
        if (StrUtil.isBlank(name)) {
            throw BusinessException.validationError("保存规则模板时必须填写模板名称");
        }
        if (name.length() > 80) {
            throw BusinessException.validationError("规则模板名称不能超过80个字符");
        }
        Long count = groupRuleMapper.selectCount(new LambdaQueryWrapper<ScoreboardGroupRule>()
                .eq(ScoreboardGroupRule::getAgeGroup, ageGroup)
                .eq(ScoreboardGroupRule::getTemplateName, name));
        if (count != null && count > 0) {
            throw BusinessException.validationError("该组别下已存在同名规则模板");
        }
        ScoreboardGroupRule template = new ScoreboardGroupRule();
        BeanUtil.copyProperties(rule, template);
        template.setId(null);
        template.setAgeGroup(ageGroup);
        template.setTemplateName(name);
        template.setEnabled(1);
        groupRuleMapper.insert(template);
        return template;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreboardMatch> listMatches(LocalDate matchDate, String status) {
        if (StrUtil.isNotBlank(status) && !MATCH_STATUSES.contains(status)) {
            throw BusinessException.validationError("比赛状态不正确");
        }
        LambdaQueryWrapper<ScoreboardMatch> wrapper = new LambdaQueryWrapper<ScoreboardMatch>()
                .eq(matchDate != null, ScoreboardMatch::getMatchDate, matchDate)
                .eq(StrUtil.isNotBlank(status), ScoreboardMatch::getStatus, status)
                .orderByDesc(ScoreboardMatch::getMatchDate)
                .orderByDesc(ScoreboardMatch::getMatchTime)
                .orderByDesc(ScoreboardMatch::getId);
        return matchMapper.selectList(wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public MatchDetailVO getDetail(Long matchId) {
        ScoreboardMatch match = requireMatch(matchId);
        return MatchDetailVO.builder()
                .match(match)
                .rule(matchRuleMapper.selectById(matchId))
                .state(stateMapper.selectById(matchId))
                .roster(rosterMapper.selectList(new LambdaQueryWrapper<ScoreboardMatchRoster>()
                        .eq(ScoreboardMatchRoster::getMatchId, matchId)
                        .orderByAsc(ScoreboardMatchRoster::getTeamSide, ScoreboardMatchRoster::getJerseyNo)))
                .events(eventMapper.selectList(new LambdaQueryWrapper<ScoreboardMatchEvent>()
                        .eq(ScoreboardMatchEvent::getMatchId, matchId)
                        .orderByAsc(ScoreboardMatchEvent::getSequenceNo)))
                .penalties(penaltyMapper.selectList(new LambdaQueryWrapper<ScoreboardMatchPenalty>()
                        .eq(ScoreboardMatchPenalty::getMatchId, matchId)
                        .orderByAsc(ScoreboardMatchPenalty::getId)))
                .coaches(coachMapper.selectList(new LambdaQueryWrapper<ScoreboardMatchCoach>()
                        .eq(ScoreboardMatchCoach::getMatchId, matchId)
                        .orderByAsc(ScoreboardMatchCoach::getTeamSide, ScoreboardMatchCoach::getCoachType, ScoreboardMatchCoach::getSortNo)))
                .officials(officialMapper.selectList(new LambdaQueryWrapper<ScoreboardMatchOfficial>()
                        .eq(ScoreboardMatchOfficial::getMatchId, matchId)
                        .orderByAsc(ScoreboardMatchOfficial::getOfficialRole, ScoreboardMatchOfficial::getSortNo)))
                .build();
    }

    @Override
    public void clearMatchRecords(Long matchId, MatchRecordClearDTO request) {
        ScoreboardMatch match = requireMatch(matchId);
        ScoreboardMatchRule rule = matchRuleMapper.selectById(matchId);
        if (rule == null) {
            throw BusinessException.validationError("本场规则不存在，不能重置比赛");
        }
        ScoreboardMatchState currentState = stateMapper.selectById(matchId);
        requireExpectedVersion(currentState, request.getExpectedVersion());

        // 罚时引用比赛事件，必须先清理罚时运行记录。
        penaltyMapper.delete(new LambdaQueryWrapper<ScoreboardMatchPenalty>()
                .eq(ScoreboardMatchPenalty::getMatchId, matchId));
        eventMapper.delete(new LambdaQueryWrapper<ScoreboardMatchEvent>()
                .eq(ScoreboardMatchEvent::getMatchId, matchId));

        ScoreboardMatchState resetState = initialState(matchId, rule);
        resetState.setStateVersion(currentState.getStateVersion());
        if (stateMapper.updateById(resetState) != 1) {
            throw BusinessException.validationError("比赛状态已经变化，请刷新后重试");
        }

        match.setStatus("READY");
        match.setHomeScore(0);
        match.setAwayScore(0);
        match.setStartedAt(null);
        match.setFinishedAt(null);
        if (matchMapper.updateById(match) != 1) {
            throw BusinessException.validationError("比赛基础状态重置失败，请刷新后重试");
        }
    }

    @Override
    public void updateMatchInfo(Long matchId, MatchInfoUpdateDTO request) {
        ScoreboardMatch match = requireMatch(matchId);
        if (!"READY".equals(match.getStatus()) || hasEvents(matchId)) {
            throw BusinessException.validationError("比赛已经开始，不能再修改基础信息");
        }
        BeanUtil.copyProperties(request, match, IGNORE_NULL);
        matchMapper.updateById(match);
    }

    @Override
    public void updateMatchRule(Long matchId, ScoreboardRuleDTO request) {
        ScoreboardMatch match = requireMatch(matchId);
        if (!"READY".equals(match.getStatus()) || hasEvents(matchId)) {
            throw BusinessException.validationError("比赛已有记录，不能再修改本场规则");
        }
        ScoreboardMatchRule rule = matchRuleMapper.selectById(matchId);
        BeanUtil.copyProperties(request, rule, IGNORE_NULL);
        rule.setIsCustomized(1);
        validateRule(rule);
        matchRuleMapper.updateById(rule);

        ScoreboardMatchState state = initialState(matchId, rule);
        state.setStateVersion(stateMapper.selectById(matchId).getStateVersion());
        stateMapper.updateById(state);
    }

    @Override
    public void replaceRoster(Long matchId, List<MatchRosterDTO> roster) {
        requireEditableRoster(matchId);
        rosterMapper.delete(new LambdaQueryWrapper<ScoreboardMatchRoster>().eq(ScoreboardMatchRoster::getMatchId, matchId));
        if (CollUtil.isEmpty(roster)) {
            return;
        }
        for (MatchRosterDTO item : roster) {
            validateTeamSide(item.getTeamSide());
            ScoreboardMatchRoster entity = new ScoreboardMatchRoster();
            BeanUtil.copyProperties(item, entity);
            entity.setMatchId(matchId);
            rosterMapper.insert(entity);
        }
    }

    @Override
    public void replaceCoaches(Long matchId, List<MatchCoachDTO> coaches) {
        requireMatch(matchId);
        coachMapper.delete(new LambdaQueryWrapper<ScoreboardMatchCoach>().eq(ScoreboardMatchCoach::getMatchId, matchId));
        if (CollUtil.isEmpty(coaches)) {
            return;
        }
        for (MatchCoachDTO item : coaches) {
            validateTeamSide(item.getTeamSide());
            ScoreboardMatchCoach entity = new ScoreboardMatchCoach();
            BeanUtil.copyProperties(item, entity);
            entity.setMatchId(matchId);
            entity.setSortNo(item.getSortNo() == null ? 0 : item.getSortNo());
            coachMapper.insert(entity);
        }
    }

    @Override
    public void replaceOfficials(Long matchId, List<MatchOfficialDTO> officials) {
        requireMatch(matchId);
        officialMapper.delete(new LambdaQueryWrapper<ScoreboardMatchOfficial>().eq(ScoreboardMatchOfficial::getMatchId, matchId));
        if (CollUtil.isEmpty(officials)) {
            return;
        }
        for (MatchOfficialDTO item : officials) {
            ScoreboardMatchOfficial entity = new ScoreboardMatchOfficial();
            BeanUtil.copyProperties(item, entity);
            entity.setMatchId(matchId);
            entity.setSortNo(item.getSortNo() == null ? 0 : item.getSortNo());
            officialMapper.insert(entity);
        }
    }

    @Override
    public MatchStateSaveVO saveState(Long matchId, MatchStateSaveDTO request) {
        ScoreboardMatch match = requireMatch(matchId);
        ScoreboardMatchState state = stateMapper.selectById(matchId);
        if (state == null || !request.getExpectedVersion().equals(state.getStateVersion())) {
            throw BusinessException.validationError("比赛状态已经变化，请刷新后重试");
        }
        String previousPhase = state.getPhase();
        BeanUtil.copyProperties(request, state, IGNORE_NULL);
        copyNullableState(request, state);
        List<ScoreboardMatchEvent> createdEvents = new ArrayList<>();
        List<ScoreboardMatchPenalty> createdPenalties = new ArrayList<>();
        int nextSequence = nextEventSequence(matchId);
        if (CollUtil.isNotEmpty(request.getEventStatuses())) {
            for (MatchEventStatusSaveDTO statusRequest : request.getEventStatuses()) {
                ScoreboardMatchEvent event = eventMapper.selectById(statusRequest.getId());
                if (event == null || !matchId.equals(event.getMatchId())) {
                    throw BusinessException.validationError("同步的事件不属于当前比赛");
                }
                if (!EVENT_STATUSES.contains(statusRequest.getEventStatus())) {
                    throw BusinessException.validationError("事件状态不正确");
                }
                event.setEventStatus(statusRequest.getEventStatus());
                eventMapper.updateById(event);
                if ("PENALTY".equals(event.getEventType())
                        && !"VALID".equals(statusRequest.getEventStatus())) {
                    penaltyMapper.delete(new LambdaQueryWrapper<ScoreboardMatchPenalty>()
                            .eq(ScoreboardMatchPenalty::getPenaltyEventId, event.getId()));
                }
                createdEvents.add(event);
            }
        }
        if (CollUtil.isNotEmpty(request.getEvents())) {
            for (MatchEventCreateDTO eventRequest : request.getEvents()) {
                EventAppendResult appended = appendEventInternal(match, eventRequest, state, nextSequence++);
                createdEvents.add(appended.event());
                if (appended.penalty() != null) {
                    createdPenalties.add(appended.penalty());
                }
                if ("TIMEOUT".equals(state.getPhase()) && state.getTimeoutEventId() == null
                        && "TEAM_TIMEOUT".equals(appended.event().getEventType())) {
                    state.setTimeoutEventId(appended.event().getId());
                }
            }
        }
        if (CollUtil.isNotEmpty(request.getPenalties())) {
            for (MatchPenaltySaveDTO penaltyRequest : request.getPenalties()) {
                ScoreboardMatchPenalty penalty = penaltyMapper.selectById(penaltyRequest.getId());
                if (penalty == null || !matchId.equals(penalty.getMatchId())) {
                    throw BusinessException.validationError("同步的罚时不属于当前比赛");
                }
                if (!PENALTY_STATES.contains(penaltyRequest.getPenaltyState())) {
                    throw BusinessException.validationError("罚时状态不正确");
                }
                penalty.setRemainingSeconds(penaltyRequest.getRemainingSeconds());
                penalty.setPenaltyState(penaltyRequest.getPenaltyState());
                penaltyMapper.updateById(penalty);
                createdPenalties.add(penalty);
            }
        }
        validateState(match, request.getMatchStatus(), previousPhase, state);
        if ((Integer.valueOf(1).equals(state.getMainRunning()) || Integer.valueOf(1).equals(state.getAuxiliaryRunning()))
                && state.getTimerStartedAt() == null) {
            state.setTimerStartedAt(LocalDateTime.now());
        }
        if (Integer.valueOf(0).equals(state.getMainRunning()) && Integer.valueOf(0).equals(state.getAuxiliaryRunning())) {
            state.setTimerStartedAt(null);
        }
        if (stateMapper.updateById(state) != 1) {
            throw BusinessException.validationError("比赛状态已经变化，请刷新后重试");
        }

        updateMatchSummary(match, request, state, createdEvents);
        return new MatchStateSaveVO(stateMapper.selectById(matchId), createdEvents, createdPenalties);
    }

    /**
     * 状态保存是完整快照，可空字段的 null 表示明确清除，不能被 IGNORE_NULL 吞掉。
     */
    private void copyNullableState(MatchStateSaveDTO request, ScoreboardMatchState state) {
        state.setClearRemainingSeconds(request.getClearRemainingSeconds());
        state.setAuxiliaryType(request.getAuxiliaryType());
        state.setTimeoutEventId(request.getTimeoutEventId());
        state.setTimeoutReturnPhase(request.getTimeoutReturnPhase());
        state.setTimeoutReturnAuxiliaryType(request.getTimeoutReturnAuxiliaryType());
        state.setPossessionSide(request.getPossessionSide());
        state.setAlternateSide(request.getAlternateSide());
        state.setPendingGoalSequenceNo(request.getPendingGoalSequenceNo());
        state.setPendingGoalSide(request.getPendingGoalSide());
        state.setPendingGoalPeriod(request.getPendingGoalPeriod());
        state.setPendingGoalRemainingSeconds(request.getPendingGoalRemainingSeconds());
        state.setPendingGoalRosterId(request.getPendingGoalRosterId());
        state.setPendingGoalAssistRosterId(request.getPendingGoalAssistRosterId());
    }

    @Override
    public MatchOperationVO<ScoreboardMatchEvent> appendEvent(Long matchId, MatchEventCreateDTO request) {
        ScoreboardMatch match = requireMatch(matchId);
        ScoreboardMatchState state = stateMapper.selectById(matchId);
        requireExpectedVersion(state, request.getExpectedVersion());
        EventAppendResult appended = appendEventInternal(
                match, request, state, nextEventSequence(matchId));
        if (stateMapper.updateById(state) != 1) {
            throw BusinessException.validationError("比赛状态已经变化，请刷新后重试");
        }
        return new MatchOperationVO<>(stateMapper.selectById(matchId).getStateVersion(), appended.event());
    }

    @Override
    public MatchOperationVO<ScoreboardMatchPenalty> updatePenaltyState(Long matchId, Long penaltyId,
                                                                        PenaltyStateDTO request) {
        requireMatch(matchId);
        ScoreboardMatchState state = stateMapper.selectById(matchId);
        requireExpectedVersion(state, request.getExpectedVersion());
        if (!PENALTY_STATES.contains(request.getPenaltyState())) {
            throw BusinessException.validationError("罚时状态不正确");
        }
        ScoreboardMatchPenalty penalty = penaltyMapper.selectById(penaltyId);
        if (penalty == null || !matchId.equals(penalty.getMatchId())) {
            throw BusinessException.dataNotFound();
        }
        penalty.setRemainingSeconds(request.getRemainingSeconds());
        penalty.setPenaltyState(request.getPenaltyState());
        penaltyMapper.updateById(penalty);
        if (stateMapper.updateById(state) != 1) {
            throw BusinessException.validationError("比赛状态已经变化，请刷新后重试");
        }
        return new MatchOperationVO<>(stateMapper.selectById(matchId).getStateVersion(), penalty);
    }

    private EventAppendResult appendEventInternal(ScoreboardMatch match, MatchEventCreateDTO request,
                                                  ScoreboardMatchState state, int sequenceNo) {
        MatchEventType eventType = EnumUtil.fromStringQuietly(MatchEventType.class, request.getEventType());
        if (eventType == null) {
            throw BusinessException.validationError("事件类型不正确");
        }
        String eventStatus = StrUtil.blankToDefault(request.getEventStatus(), "VALID");
        if (!EVENT_STATUSES.contains(eventStatus)) {
            throw BusinessException.validationError("事件状态不正确");
        }
        validateTeamSideIfPresent(request.getTeamSide());
        EventRosterContext rosterContext = validateEvent(match.getId(), eventType, request);
        validateRelatedEvent(match.getId(), request.getRelatedEventId());

        ScoreboardMatchEvent event = new ScoreboardMatchEvent();
        BeanUtil.copyProperties(request, event);
        event.setMatchId(match.getId());
        event.setEventType(eventType.name());
        event.setSubjectName(StrUtil.isBlank(request.getSubjectName())
                ? null : StrUtil.trim(request.getSubjectName()));
        event.setAssistName(StrUtil.isBlank(request.getAssistName())
                ? null : StrUtil.trim(request.getAssistName()));
        event.setServedByName(StrUtil.isBlank(request.getServedByName())
                ? null : StrUtil.trim(request.getServedByName()));
        event.setSequenceNo(sequenceNo);
        event.setPeriodNo(request.getPeriodNo() == null ? state.getCurrentPeriod() : request.getPeriodNo());
        event.setRemainingSeconds(request.getRemainingSeconds() == null ? state.getMainRemainingSeconds() : request.getRemainingSeconds());
        event.setOccurredAt(request.getOccurredAt() == null ? LocalDateTime.now() : request.getOccurredAt());
        event.setEventStatus(eventStatus);
        event.setEventTitle(MatchEventContentUtil.title(eventType, event, match, request.getEventTitle()));
        event.setEventText(MatchEventContentUtil.build(eventType, event, match,
                rosterContext.subject(), rosterContext.assist(), rosterContext.server(),
                request.getDescription()));
        event.setRuleReference(StrUtil.isBlank(request.getRuleReference())
                ? null : StrUtil.trim(request.getRuleReference()));
        eventMapper.insert(event);

        ScoreboardMatchPenalty penalty = null;
        if (eventType == MatchEventType.PENALTY
                && event.getPenaltySeconds() != null && event.getPenaltySeconds() > 0) {
            penalty = new ScoreboardMatchPenalty();
            penalty.setMatchId(match.getId());
            penalty.setPenaltyEventId(event.getId());
            penalty.setRemainingSeconds(request.getPenaltyRemainingSeconds() == null ? event.getPenaltySeconds() : request.getPenaltyRemainingSeconds());
            penalty.setPenaltyState(StrUtil.blankToDefault(request.getPenaltyState(), "WAITING"));
            if (!PENALTY_STATES.contains(penalty.getPenaltyState())) {
                throw BusinessException.validationError("罚时状态不正确");
            }
            penaltyMapper.insert(penalty);
        }
        return new EventAppendResult(event, penalty);
    }

    private int nextEventSequence(Long matchId) {
        ScoreboardMatchEvent latest = eventMapper.selectOne(new LambdaQueryWrapper<ScoreboardMatchEvent>()
                .eq(ScoreboardMatchEvent::getMatchId, matchId)
                .orderByDesc(ScoreboardMatchEvent::getSequenceNo)
                .last("LIMIT 1"));
        return latest == null ? 1 : latest.getSequenceNo() + 1;
    }

    private void updateMatchSummary(ScoreboardMatch match, MatchStateSaveDTO request,
                                    ScoreboardMatchState state,
                                    List<ScoreboardMatchEvent> createdEvents) {
        if (request.getHomeScore() != null) {
            requireNonNegative(request.getHomeScore(), "主队比分");
            match.setHomeScore(request.getHomeScore());
        }
        if (request.getAwayScore() != null) {
            requireNonNegative(request.getAwayScore(), "客队比分");
            match.setAwayScore(request.getAwayScore());
        }
        String derivedStatus = deriveMatchStatus(match.getStatus(), state.getPhase());
        if (StrUtil.isNotBlank(request.getMatchStatus()) && !derivedStatus.equals(request.getMatchStatus())) {
            throw BusinessException.validationError("比赛状态与场内阶段不一致");
        }
        match.setStatus(derivedStatus);
        if ("ONGOING".equals(derivedStatus) && match.getStartedAt() == null) {
            LocalDateTime matchStartedAt = createdEvents.stream()
                    .filter(event -> MatchEventType.MATCH_START.name().equals(event.getEventType()))
                    .map(ScoreboardMatchEvent::getOccurredAt)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseGet(LocalDateTime::now);
            match.setStartedAt(matchStartedAt);
        }
        if ("FINISHED".equals(derivedStatus) && match.getFinishedAt() == null) {
                match.setFinishedAt(LocalDateTime.now());
        } else if (!"FINISHED".equals(derivedStatus)) {
            match.setFinishedAt(null);
        }
        matchMapper.updateById(match);
    }

    private ScoreboardMatchState initialState(Long matchId, ScoreboardMatchRule rule) {
        ScoreboardMatchState state = new ScoreboardMatchState();
        state.setMatchId(matchId);
        state.setPhase("READY");
        state.setCurrentPeriod(1);
        state.setMainRemainingSeconds(rule.getPeriodMinutes() * 60);
        state.setShotRemainingSeconds(rule.getShotClockSeconds());
        state.setClearRemainingSeconds(null);
        state.setAuxiliaryRemainingSeconds(0);
        state.setTimeoutReturnAuxiliarySeconds(0);
        state.setTimeoutReturnAuxiliaryRunning(0);
        state.setMainRunning(0);
        state.setAuxiliaryRunning(0);
        state.setBallState("dead");
        state.setPossessionSide("manual".equals(rule.getAlternateFirst()) ? null : rule.getAlternateFirst());
        state.setFrontcourt(0);
        state.setAlternateSide("manual".equals(rule.getAlternateFirst()) ? null : rule.getAlternateFirst());
        state.setResetShotOnResume(0);
        state.setGoldenGoal(0);
        state.setGoldenConflict(0);
        state.setHomeTimeoutsRemaining(rule.getTimeoutsPerHalf());
        state.setAwayTimeoutsRemaining(rule.getTimeoutsPerHalf());
        state.setStateVersion(0);
        return state;
    }

    private void validateState(ScoreboardMatch match, String requestedStatus,
                               String previousPhase, ScoreboardMatchState state) {
        if (!PHASES.contains(state.getPhase())) {
            throw BusinessException.validationError("场内阶段不正确");
        }
        if (!PHASE_TRANSITIONS.get(previousPhase).contains(state.getPhase())) {
            throw BusinessException.validationError("不能从当前阶段直接切换到目标阶段");
        }
        if (state.getCurrentPeriod() == null || state.getCurrentPeriod() < 1) {
            throw BusinessException.validationError("当前节次必须从1开始");
        }
        validateTeamSideIfPresent(state.getPossessionSide());
        validateTeamSideIfPresent(state.getAlternateSide());
        validateTeamSideIfPresent(state.getPendingGoalSide());
        requireNonNegative(state.getMainRemainingSeconds(), "比赛剩余时间");
        requireNonNegative(state.getShotRemainingSeconds(), "进攻剩余时间");
        requireNonNegative(state.getAuxiliaryRemainingSeconds(), "辅助剩余时间");
        requireNonNegative(state.getHomeTimeoutsRemaining(), "主队剩余暂停次数");
        requireNonNegative(state.getAwayTimeoutsRemaining(), "客队剩余暂停次数");
        validateBoolean(state.getMainRunning(), "比赛钟运行状态");
        validateBoolean(state.getAuxiliaryRunning(), "辅助计时运行状态");
        validateBoolean(state.getFrontcourt(), "前场状态");
        validateBoolean(state.getResetShotOnResume(), "进攻钟重置状态");
        validateBoolean(state.getGoldenGoal(), "金球状态");
        validateBoolean(state.getGoldenConflict(), "金球冲突状态");
        if (Integer.valueOf(1).equals(state.getMainRunning()) && Integer.valueOf(1).equals(state.getAuxiliaryRunning())) {
            throw BusinessException.validationError("比赛钟和辅助计时不能同时运行");
        }
        if ("PERIOD".equals(state.getPhase()) && Integer.valueOf(1).equals(state.getAuxiliaryRunning())) {
            throw BusinessException.validationError("比赛节内不能运行休息或暂停计时");
        }
        if (("BREAK".equals(state.getPhase()) || "TIMEOUT".equals(state.getPhase()))
                && Integer.valueOf(1).equals(state.getMainRunning())) {
            throw BusinessException.validationError("休息或球队暂停时不能运行比赛钟");
        }
        if ("TIMEOUT".equals(state.getPhase()) && state.getTimeoutEventId() == null) {
            throw BusinessException.validationError("球队暂停缺少对应事件");
        }
        if (!"TIMEOUT".equals(state.getPhase()) && state.getTimeoutEventId() != null) {
            throw BusinessException.validationError("非球队暂停阶段不能保留暂停事件");
        }
        ScoreboardMatchRoster scorer = requireRoster(match.getId(), state.getPendingGoalRosterId());
        ScoreboardMatchRoster assist = requireRoster(match.getId(), state.getPendingGoalAssistRosterId());
        if ((scorer != null || assist != null) && state.getPendingGoalSide() == null) {
            throw BusinessException.validationError("待确认进球球员缺少所属球队");
        }
        validateGoalRoster(state.getPendingGoalSide(), scorer, assist);

        String nextStatus = StrUtil.blankToDefault(requestedStatus,
                deriveMatchStatus(match.getStatus(), state.getPhase()));
        if (!MATCH_STATUSES.contains(nextStatus)) {
            throw BusinessException.validationError("比赛状态不正确");
        }
        if (!deriveMatchStatus(match.getStatus(), state.getPhase()).equals(nextStatus)) {
            throw BusinessException.validationError("比赛状态与场内阶段不一致");
        }
    }

    private void validateRule(ScoreboardRuleFields rule) {
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
            throw BusinessException.validationError("本场规则缺少有效的基础数值");
        }
        if (!List.of("always", "knockout", "none").contains(rule.getOvertimePolicy())) {
            throw BusinessException.validationError("加时规则不正确");
        }
        if (!List.of("manual", "home", "away").contains(rule.getAlternateFirst())) {
            throw BusinessException.validationError("首次交替球权规则不正确");
        }
        validateBoolean(rule.getAllDeadStop(), "普通阶段停表规则");
        validateBoolean(rule.getFoulStop(), "犯规停表规则");
        validateBoolean(rule.getOvertimeStopAll(), "加时停表规则");
        validateBoolean(rule.getShotLastOff(), "末段进攻钟规则");
        validateBoolean(rule.getGoldenGoal(), "金球规则");
        validateBoolean(rule.getGoalieProxy(), "守门员代罚规则");
        validateBoolean(rule.getPassRequired(), "接传规则");
        validateBoolean(rule.getAlternateReset(), "交替球权重置规则");
    }

    private ScoreboardMatch requireMatch(Long matchId) {
        ScoreboardMatch match = matchMapper.selectById(matchId);
        if (match == null) {
            throw BusinessException.dataNotFound();
        }
        return match;
    }

    private void requireEditableRoster(Long matchId) {
        ScoreboardMatch match = requireMatch(matchId);
        if (!"READY".equals(match.getStatus()) || hasEvents(matchId)) {
            throw BusinessException.validationError("比赛已有记录，不能整体替换本场名单");
        }
    }

    private boolean hasEvents(Long matchId) {
        return eventMapper.selectCount(new LambdaQueryWrapper<ScoreboardMatchEvent>()
                .eq(ScoreboardMatchEvent::getMatchId, matchId)) > 0;
    }

    private ScoreboardMatchRoster requireRoster(Long matchId, Long rosterId) {
        if (rosterId == null) {
            return null;
        }
        ScoreboardMatchRoster roster = rosterMapper.selectById(rosterId);
        if (roster == null || !matchId.equals(roster.getMatchId())) {
            throw BusinessException.validationError("所选球员不在本场名单中");
        }
        return roster;
    }

    private void validateRelatedEvent(Long matchId, Long eventId) {
        if (eventId == null) {
            return;
        }
        long count = eventMapper.selectCount(new LambdaQueryWrapper<ScoreboardMatchEvent>()
                .eq(ScoreboardMatchEvent::getId, eventId)
                .eq(ScoreboardMatchEvent::getMatchId, matchId));
        if (count == 0) {
            throw BusinessException.validationError("关联事件不属于当前比赛");
        }
    }

    private void validateStage(String stage) {
        if (StrUtil.isNotBlank(stage) && !List.of("league", "knockout").contains(stage)) {
            throw BusinessException.validationError("比赛阶段不正确");
        }
    }

    private void validateTeamSide(String teamSide) {
        if (!TEAM_SIDES.contains(teamSide)) {
            throw BusinessException.validationError("球队身份不正确");
        }
    }

    private void validateTeamSideIfPresent(String teamSide) {
        if (teamSide != null) {
            validateTeamSide(teamSide);
        }
    }

    private void requireNonNegative(Integer value, String fieldName) {
        if (value == null || value < 0) {
            throw BusinessException.validationError(fieldName + "不能小于0");
        }
    }

    private void validateBoolean(Integer value, String fieldName) {
        if (value == null || (value != 0 && value != 1)) {
            throw BusinessException.validationError(fieldName + "只能为0或1");
        }
    }

    private void requireExpectedVersion(ScoreboardMatchState state, Integer expectedVersion) {
        if (state == null) {
            throw BusinessException.dataNotFound();
        }
        if (expectedVersion == null || !expectedVersion.equals(state.getStateVersion())) {
            throw BusinessException.validationError("比赛状态已经变化，请刷新后重试");
        }
    }

    private String deriveMatchStatus(String currentStatus, String phase) {
        if ("FINISHED".equals(phase)) {
            return "FINISHED";
        }
        if ("ONGOING".equals(currentStatus)
                || List.of("PERIOD", "PERIOD_END", "BREAK").contains(phase)) {
            return "ONGOING";
        }
        return "READY";
    }

    private boolean isRuleCustomized(ScoreboardGroupRule defaultRule, ScoreboardRuleDTO overrides) {
        if (overrides == null) {
            return false;
        }
        return BeanUtil.beanToMap(overrides, false, true).entrySet().stream()
                .anyMatch(entry -> !Objects.equals(entry.getValue(), BeanUtil.getProperty(defaultRule, entry.getKey())));
    }

    private EventRosterContext validateEvent(Long matchId, MatchEventType eventType,
                                             MatchEventCreateDTO request) {
        if (request.getPeriodNo() != null && request.getPeriodNo() < 1) {
            throw BusinessException.validationError("事件节次必须从1开始");
        }
        if (request.getRemainingSeconds() != null && request.getRemainingSeconds() < 0) {
            throw BusinessException.validationError("事件发生时剩余时间不能小于0");
        }
        ScoreboardMatchRoster roster = requireRoster(matchId, request.getRosterId());
        ScoreboardMatchRoster assist = requireRoster(matchId, request.getAssistRosterId());
        ScoreboardMatchRoster server = requireRoster(matchId, request.getServedByRosterId());

        if (eventType == MatchEventType.GOAL) {
            if (request.getTeamSide() == null || request.getPoints() == null || request.getPoints() <= 0) {
                throw BusinessException.validationError("进球事件缺少球队或得分");
            }
            validateGoalRoster(request.getTeamSide(), roster, assist);
        }
        if (eventType == MatchEventType.TEAM_TIMEOUT
                && (request.getTeamSide() == null || request.getDurationSeconds() == null
                || request.getDurationSeconds() <= 0)) {
            throw BusinessException.validationError("球队暂停事件缺少球队或暂停时长");
        }
        if (eventType == MatchEventType.PENALTY) {
            validatePenaltyEvent(request, roster, server);
        }
        if (eventType == MatchEventType.CORRECTION
                && (request.getRelatedEventId() == null || StrUtil.isBlank(request.getCorrectionReason()))) {
            throw BusinessException.validationError("更正事件必须关联原记录并填写依据");
        }
        if (eventType == MatchEventType.SYSTEM && StrUtil.isBlank(request.getDescription())) {
            throw BusinessException.validationError("系统事件必须填写说明");
        }
        return new EventRosterContext(roster, assist, server);
    }

    private void validateGoalRoster(String teamSide, ScoreboardMatchRoster scorer, ScoreboardMatchRoster assist) {
        if (scorer != null && !Objects.equals(teamSide, scorer.getTeamSide())) {
            throw BusinessException.validationError("进球球员与进球队伍不一致");
        }
        if (assist != null && !Objects.equals(teamSide, assist.getTeamSide())) {
            throw BusinessException.validationError("助攻球员与进球队伍不一致");
        }
        if (scorer != null && assist != null && scorer.getId().equals(assist.getId())) {
            throw BusinessException.validationError("进球球员不能同时记为助攻球员");
        }
    }

    private void validatePenaltyEvent(MatchEventCreateDTO request,
                                      ScoreboardMatchRoster roster,
                                      ScoreboardMatchRoster server) {
        if (request.getTeamSide() == null || !PENALTY_SUBJECTS.contains(request.getPenaltySubject())
                || !PENALTY_TYPES.contains(request.getPenaltyType())) {
            throw BusinessException.validationError("判罚事件缺少球队、犯规主体或判罚类型");
        }
        if (!"official".equals(request.getPenaltySubject())
                && roster == null && StrUtil.isBlank(request.getSubjectName())) {
            throw BusinessException.validationError("球员或守门员判罚必须填写号码或姓名");
        }
        if ("official".equals(request.getPenaltySubject()) && StrUtil.isBlank(request.getSubjectName())) {
            throw BusinessException.validationError("球队官员判罚必须填写姓名");
        }
        if (roster != null && !request.getTeamSide().equals(roster.getTeamSide())) {
            throw BusinessException.validationError("犯规球员与犯规球队不一致");
        }
        if ("turnover".equals(request.getPenaltyType())) {
            if (request.getPenaltySeconds() != null && request.getPenaltySeconds() > 0) {
                throw BusinessException.validationError("仅换球权判罚不能设置罚时");
            }
            return;
        }
        if (request.getPenaltySeconds() == null || request.getPenaltySeconds() <= 0
                || (server == null && StrUtil.isBlank(request.getServedByName()))) {
            throw BusinessException.validationError("有罚时判罚必须填写罚时和服罚球员");
        }
        if (server != null && !request.getTeamSide().equals(server.getTeamSide())) {
            throw BusinessException.validationError("服罚球员与犯规球队不一致");
        }
    }

    private record EventRosterContext(ScoreboardMatchRoster subject,
                                      ScoreboardMatchRoster assist,
                                      ScoreboardMatchRoster server) {
    }

    private record EventAppendResult(ScoreboardMatchEvent event,
                                     ScoreboardMatchPenalty penalty) {
    }
}
