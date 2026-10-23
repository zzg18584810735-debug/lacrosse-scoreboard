package com.edus.score.service;

import cn.hutool.core.bean.BeanUtil;
import com.edus.common.exception.BusinessException;
import com.edus.score.dto.req.MatchCreateDTO;
import com.edus.score.dto.req.MatchEventCreateDTO;
import com.edus.score.dto.req.MatchEventStatusSaveDTO;
import com.edus.score.dto.req.MatchPenaltySaveDTO;
import com.edus.score.dto.req.MatchRecordClearDTO;
import com.edus.score.dto.req.MatchStateSaveDTO;
import com.edus.score.dto.req.ScoreboardRuleDTO;
import com.edus.score.dto.res.MatchStateSaveVO;
import com.edus.score.entity.ScoreboardGroupRule;
import com.edus.score.entity.ScoreboardMatch;
import com.edus.score.entity.ScoreboardMatchRoster;
import com.edus.score.entity.ScoreboardMatchEvent;
import com.edus.score.entity.ScoreboardMatchPenalty;
import com.edus.score.entity.ScoreboardMatchRule;
import com.edus.score.entity.ScoreboardMatchState;
import com.edus.score.mapper.ScoreboardGroupRuleMapper;
import com.edus.score.mapper.ScoreboardMatchCoachMapper;
import com.edus.score.mapper.ScoreboardMatchEventMapper;
import com.edus.score.mapper.ScoreboardMatchMapper;
import com.edus.score.mapper.ScoreboardMatchOfficialMapper;
import com.edus.score.mapper.ScoreboardMatchPenaltyMapper;
import com.edus.score.mapper.ScoreboardMatchRosterMapper;
import com.edus.score.mapper.ScoreboardMatchRuleMapper;
import com.edus.score.mapper.ScoreboardMatchStateMapper;
import com.edus.score.service.impl.ScoreboardMatchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreboardMatchServiceImplTest {
    @Mock private ScoreboardGroupRuleMapper groupRuleMapper;
    @Mock private ScoreboardDictionaryService dictionaryService;
    @Mock private ScoreboardMatchMapper matchMapper;
    @Mock private ScoreboardMatchRuleMapper matchRuleMapper;
    @Mock private ScoreboardMatchStateMapper stateMapper;
    @Mock private ScoreboardMatchRosterMapper rosterMapper;
    @Mock private ScoreboardMatchEventMapper eventMapper;
    @Mock private ScoreboardMatchPenaltyMapper penaltyMapper;
    @Mock private ScoreboardMatchCoachMapper coachMapper;
    @Mock private ScoreboardMatchOfficialMapper officialMapper;

    private ScoreboardMatchService service;

    @BeforeEach
    void setUp() {
        lenient().when(dictionaryService.requireEnabledAgeGroup(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new ScoreboardMatchServiceImpl(groupRuleMapper, dictionaryService, matchMapper, matchRuleMapper,
                stateMapper, rosterMapper, eventMapper, penaltyMapper, coachMapper, officialMapper);
    }

    @Test
    void createMatchCopiesRuleAndInitializesState() {
        ScoreboardGroupRule groupRule = defaultRule();
        groupRule.setAlternateFirst("away");
        when(groupRuleMapper.selectById(1L)).thenReturn(groupRule);
        doAnswer(invocation -> {
            invocation.<ScoreboardMatch>getArgument(0).setId(10L);
            return 1;
        }).when(matchMapper).insert(any(ScoreboardMatch.class));

        MatchCreateDTO request = baseCreateRequest();
        assertEquals(10L, service.createMatch(request));

        ArgumentCaptor<ScoreboardMatchRule> ruleCaptor = ArgumentCaptor.forClass(ScoreboardMatchRule.class);
        verify(matchRuleMapper).insert(ruleCaptor.capture());
        assertEquals(0, ruleCaptor.getValue().getIsCustomized());
        assertEquals(8, ruleCaptor.getValue().getPeriodMinutes());

        ArgumentCaptor<ScoreboardMatchState> stateCaptor = ArgumentCaptor.forClass(ScoreboardMatchState.class);
        verify(stateMapper).insert(stateCaptor.capture());
        assertEquals(480, stateCaptor.getValue().getMainRemainingSeconds());
        assertEquals("READY", stateCaptor.getValue().getPhase());
        assertEquals("away", stateCaptor.getValue().getPossessionSide());
        assertEquals("away", stateCaptor.getValue().getAlternateSide());
    }

    @Test
    void createMatchLeavesOpeningPossessionEmptyWhenRefereeMustDecide() {
        when(groupRuleMapper.selectById(1L)).thenReturn(defaultRule());
        doAnswer(invocation -> {
            invocation.<ScoreboardMatch>getArgument(0).setId(10L);
            return 1;
        }).when(matchMapper).insert(any(ScoreboardMatch.class));

        service.createMatch(baseCreateRequest());

        ArgumentCaptor<ScoreboardMatchState> stateCaptor = ArgumentCaptor.forClass(ScoreboardMatchState.class);
        verify(stateMapper).insert(stateCaptor.capture());
        assertEquals(null, stateCaptor.getValue().getPossessionSide());
        assertEquals(null, stateCaptor.getValue().getAlternateSide());
    }

    @Test
    void createMatchMarksActuallyChangedRuleAsCustomized() {
        when(groupRuleMapper.selectById(1L)).thenReturn(defaultRule());
        doAnswer(invocation -> {
            invocation.<ScoreboardMatch>getArgument(0).setId(10L);
            return 1;
        }).when(matchMapper).insert(any(ScoreboardMatch.class));
        MatchCreateDTO request = baseCreateRequest();
        ScoreboardRuleDTO overrides = new ScoreboardRuleDTO();
        overrides.setPeriodMinutes(10);
        request.setRules(overrides);

        service.createMatch(request);

        ArgumentCaptor<ScoreboardMatchRule> captor = ArgumentCaptor.forClass(ScoreboardMatchRule.class);
        verify(matchRuleMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getIsCustomized());
        assertEquals(10, captor.getValue().getPeriodMinutes());
    }

    @Test
    void createMatchSupportsOneTimeCustomRuleWithoutTemplate() {
        doAnswer(invocation -> {
            invocation.<ScoreboardMatch>getArgument(0).setId(10L);
            return 1;
        }).when(matchMapper).insert(any(ScoreboardMatch.class));
        MatchCreateDTO request = baseCreateRequest();
        request.setGroupRuleId(null);
        ScoreboardRuleDTO rules = new ScoreboardRuleDTO();
        BeanUtil.copyProperties(defaultRule(), rules);
        request.setRules(rules);

        service.createMatch(request);

        ArgumentCaptor<ScoreboardMatch> matchCaptor = ArgumentCaptor.forClass(ScoreboardMatch.class);
        verify(matchMapper).insert(matchCaptor.capture());
        assertEquals("sll-sixes", matchCaptor.getValue().getAgeGroup());
        assertEquals(null, matchCaptor.getValue().getGroupRuleId());
        ArgumentCaptor<ScoreboardMatchRule> ruleCaptor = ArgumentCaptor.forClass(ScoreboardMatchRule.class);
        verify(matchRuleMapper).insert(ruleCaptor.capture());
        assertEquals(1, ruleCaptor.getValue().getIsCustomized());
    }

    @Test
    void createMatchCanSaveCustomRuleAsTemplate() {
        doAnswer(invocation -> {
            invocation.<ScoreboardGroupRule>getArgument(0).setId(2L);
            return 1;
        }).when(groupRuleMapper).insert(any(ScoreboardGroupRule.class));
        doAnswer(invocation -> {
            invocation.<ScoreboardMatch>getArgument(0).setId(10L);
            return 1;
        }).when(matchMapper).insert(any(ScoreboardMatch.class));
        MatchCreateDTO request = baseCreateRequest();
        request.setGroupRuleId(null);
        request.setSaveAsTemplate(true);
        request.setTemplateName("联赛标准模板");
        ScoreboardRuleDTO rules = new ScoreboardRuleDTO();
        BeanUtil.copyProperties(defaultRule(), rules);
        request.setRules(rules);

        service.createMatch(request);

        ArgumentCaptor<ScoreboardGroupRule> templateCaptor = ArgumentCaptor.forClass(ScoreboardGroupRule.class);
        verify(groupRuleMapper).insert(templateCaptor.capture());
        assertEquals("联赛标准模板", templateCaptor.getValue().getTemplateName());
        ArgumentCaptor<ScoreboardMatch> matchCaptor = ArgumentCaptor.forClass(ScoreboardMatch.class);
        verify(matchMapper).insert(matchCaptor.capture());
        assertEquals(2L, matchCaptor.getValue().getGroupRuleId());
        ArgumentCaptor<ScoreboardMatchRule> ruleCaptor = ArgumentCaptor.forClass(ScoreboardMatchRule.class);
        verify(matchRuleMapper).insert(ruleCaptor.capture());
        assertEquals(0, ruleCaptor.getValue().getIsCustomized());
    }

    @Test
    void clearMatchRecordsKeepsScheduleAndClearsCompetitionProgress() {
        ScoreboardMatch match = new ScoreboardMatch();
        match.setId(10L);
        match.setStatus("FINISHED");
        match.setHomeScore(6);
        match.setAwayScore(4);
        match.setStartedAt(LocalDateTime.now().minusHours(1));
        match.setFinishedAt(LocalDateTime.now());
        when(matchMapper.selectById(10L)).thenReturn(match);
        ScoreboardMatchRule rule = new ScoreboardMatchRule();
        BeanUtil.copyProperties(defaultRule(), rule);
        rule.setMatchId(10L);
        when(matchRuleMapper.selectById(10L)).thenReturn(rule);
        ScoreboardMatchState state = readyState(10L, 7);
        state.setPhase("FINISHED");
        when(stateMapper.selectById(10L)).thenReturn(state);
        when(stateMapper.updateById(any(ScoreboardMatchState.class))).thenReturn(1);
        when(matchMapper.updateById(match)).thenReturn(1);

        MatchRecordClearDTO request = new MatchRecordClearDTO();
        request.setExpectedVersion(7);
        service.clearMatchRecords(10L, request);

        verify(penaltyMapper).delete(any());
        verify(eventMapper).delete(any());
        ArgumentCaptor<ScoreboardMatchState> stateCaptor = ArgumentCaptor.forClass(ScoreboardMatchState.class);
        verify(stateMapper).updateById(stateCaptor.capture());
        assertEquals("READY", stateCaptor.getValue().getPhase());
        assertEquals(480, stateCaptor.getValue().getMainRemainingSeconds());
        assertEquals(7, stateCaptor.getValue().getStateVersion());
        assertEquals("READY", match.getStatus());
        assertEquals(0, match.getHomeScore());
        assertEquals(0, match.getAwayScore());
        assertEquals(null, match.getStartedAt());
        assertEquals(null, match.getFinishedAt());
        verify(matchMapper).updateById(match);
    }

    @Test
    void clearMatchRecordsRejectsStaleVersionBeforeDeletingRecords() {
        ScoreboardMatch match = new ScoreboardMatch();
        match.setId(10L);
        when(matchMapper.selectById(10L)).thenReturn(match);
        when(matchRuleMapper.selectById(10L)).thenReturn(new ScoreboardMatchRule());
        when(stateMapper.selectById(10L)).thenReturn(readyState(10L, 4));
        MatchRecordClearDTO request = new MatchRecordClearDTO();
        request.setExpectedVersion(3);

        assertThrows(BusinessException.class, () -> service.clearMatchRecords(10L, request));

        verify(penaltyMapper, never()).delete(any());
        verify(eventMapper, never()).delete(any());
        verify(matchMapper, never()).updateById(any(ScoreboardMatch.class));
    }

    @Test
    void saveStateRejectsStaleVersionBeforeWriting() {
        ScoreboardMatch match = new ScoreboardMatch();
        match.setId(10L);
        match.setStatus("ONGOING");
        when(matchMapper.selectById(10L)).thenReturn(match);
        ScoreboardMatchState state = new ScoreboardMatchState();
        state.setMatchId(10L);
        state.setStateVersion(3);
        when(stateMapper.selectById(10L)).thenReturn(state);
        MatchStateSaveDTO request = new MatchStateSaveDTO();
        request.setExpectedVersion(2);

        assertThrows(BusinessException.class, () -> service.saveState(10L, request));
        verify(stateMapper, never()).updateById(any(ScoreboardMatchState.class));
    }

    @Test
    void saveStateWritesMultipleEventsAndReturnsCreatedPenalty() {
        ScoreboardMatch match = new ScoreboardMatch();
        match.setId(10L);
        match.setStatus("READY");
        match.setHomeTeamName("主队");
        match.setAwayTeamName("客队");
        when(matchMapper.selectById(10L)).thenReturn(match);

        ScoreboardMatchState state = readyState(10L, 2);
        when(stateMapper.selectById(10L)).thenReturn(state);
        when(stateMapper.updateById(state)).thenReturn(1);
        when(eventMapper.selectOne(any())).thenReturn(null);

        ScoreboardMatchRoster player = new ScoreboardMatchRoster();
        player.setId(20L);
        player.setMatchId(10L);
        player.setTeamSide("home");
        player.setJerseyNo(8);
        player.setPlayerName("张三");
        when(rosterMapper.selectById(20L)).thenReturn(player);

        int[] eventId = {100};
        doAnswer(invocation -> {
            invocation.<ScoreboardMatchEvent>getArgument(0).setId((long) ++eventId[0]);
            return 1;
        }).when(eventMapper).insert(any(ScoreboardMatchEvent.class));
        doAnswer(invocation -> {
            invocation.<com.edus.score.entity.ScoreboardMatchPenalty>getArgument(0).setId(201L);
            return 1;
        }).when(penaltyMapper).insert(any(com.edus.score.entity.ScoreboardMatchPenalty.class));
        ScoreboardMatchPenalty existingPenalty = new ScoreboardMatchPenalty();
        existingPenalty.setId(301L);
        existingPenalty.setMatchId(10L);
        existingPenalty.setRemainingSeconds(20);
        existingPenalty.setPenaltyState("ACTIVE");
        when(penaltyMapper.selectById(301L)).thenReturn(existingPenalty);

        MatchEventCreateDTO systemEvent = new MatchEventCreateDTO();
        systemEvent.setEventType("SYSTEM");
        systemEvent.setDescription("鸣哨开赛");
        MatchEventCreateDTO penaltyEvent = new MatchEventCreateDTO();
        penaltyEvent.setEventType("PENALTY");
        penaltyEvent.setTeamSide("home");
        penaltyEvent.setRosterId(20L);
        penaltyEvent.setPenaltySubject("player");
        penaltyEvent.setPenaltyType("minor");
        penaltyEvent.setPenaltySeconds(30);
        penaltyEvent.setServedByRosterId(20L);

        MatchStateSaveDTO request = new MatchStateSaveDTO();
        request.setExpectedVersion(2);
        request.setEvents(List.of(systemEvent, penaltyEvent));
        MatchPenaltySaveDTO penaltySave = new MatchPenaltySaveDTO();
        penaltySave.setId(301L);
        penaltySave.setRemainingSeconds(12);
        penaltySave.setPenaltyState("ACTIVE");
        request.setPenalties(List.of(penaltySave));

        MatchStateSaveVO result = service.saveState(10L, request);

        assertEquals(2, result.getEvents().size());
        assertEquals(1, result.getEvents().get(0).getSequenceNo());
        assertEquals(2, result.getEvents().get(1).getSequenceNo());
        assertEquals(2, result.getPenalties().size());
        assertEquals(201L, result.getPenalties().get(0).getId());
        assertEquals(102L, result.getPenalties().get(0).getPenaltyEventId());
        assertEquals(301L, result.getPenalties().get(1).getId());
        assertEquals(12, result.getPenalties().get(1).getRemainingSeconds());
        verify(penaltyMapper).updateById(existingPenalty);
    }

    @Test
    void matchStartUsesMatchStartEventOccurredAt() {
        ScoreboardMatch match = new ScoreboardMatch();
        match.setId(10L);
        match.setStatus("READY");
        when(matchMapper.selectById(10L)).thenReturn(match);

        ScoreboardMatchState state = readyState(10L, 2);
        when(stateMapper.selectById(10L)).thenReturn(state);
        when(stateMapper.updateById(state)).thenReturn(1);
        when(eventMapper.selectOne(any())).thenReturn(null);

        LocalDateTime whistleTime = LocalDateTime.of(2026, 10, 22, 10, 30, 15);
        MatchEventCreateDTO startEvent = new MatchEventCreateDTO();
        startEvent.setEventType("MATCH_START");
        startEvent.setTeamSide("home");
        startEvent.setOccurredAt(whistleTime);

        MatchStateSaveDTO request = new MatchStateSaveDTO();
        request.setExpectedVersion(2);
        request.setMatchStatus("ONGOING");
        request.setPhase("PERIOD");
        request.setMainRemainingSeconds(480);
        request.setMainRunning(1);
        request.setEvents(List.of(startEvent));

        service.saveState(10L, request);

        assertEquals(whistleTime, match.getStartedAt());
        verify(matchMapper).updateById(match);
    }

    @Test
    void saveStateCancelsExistingPenaltyEventAndRemovesRuntimePenalty() {
        ScoreboardMatch match = new ScoreboardMatch();
        match.setId(10L);
        match.setStatus("READY");
        when(matchMapper.selectById(10L)).thenReturn(match);
        ScoreboardMatchState state = readyState(10L, 2);
        when(stateMapper.selectById(10L)).thenReturn(state);
        when(stateMapper.updateById(state)).thenReturn(1);
        when(eventMapper.selectOne(any())).thenReturn(null);

        ScoreboardMatchEvent event = new ScoreboardMatchEvent();
        event.setId(90L);
        event.setMatchId(10L);
        event.setEventType("PENALTY");
        event.setEventStatus("VALID");
        event.setSequenceNo(1);
        when(eventMapper.selectById(90L)).thenReturn(event);

        MatchEventStatusSaveDTO status = new MatchEventStatusSaveDTO();
        status.setId(90L);
        status.setEventStatus("CANCELLED");
        MatchStateSaveDTO request = new MatchStateSaveDTO();
        request.setExpectedVersion(2);
        request.setEventStatuses(List.of(status));

        MatchStateSaveVO result = service.saveState(10L, request);

        assertEquals("CANCELLED", result.getEvents().get(0).getEventStatus());
        verify(eventMapper).updateById(event);
        verify(penaltyMapper).delete(any());
    }

    @Test
    void saveStateClearsNullableTimeoutAndPendingGoalFields() {
        ScoreboardMatch match = new ScoreboardMatch();
        match.setId(10L);
        match.setStatus("ONGOING");
        when(matchMapper.selectById(10L)).thenReturn(match);
        ScoreboardMatchState state = readyState(10L, 2);
        state.setPhase("TIMEOUT");
        state.setAuxiliaryType("TIMEOUT");
        state.setTimeoutEventId(90L);
        state.setTimeoutReturnPhase("PERIOD");
        state.setPendingGoalSequenceNo(3);
        state.setPendingGoalSide("home");
        state.setPendingGoalPeriod(1);
        state.setPendingGoalRemainingSeconds(420);
        when(stateMapper.selectById(10L)).thenReturn(state);
        when(stateMapper.updateById(state)).thenReturn(1);

        MatchStateSaveDTO request = new MatchStateSaveDTO();
        request.setExpectedVersion(2);
        request.setPhase("PERIOD");

        service.saveState(10L, request);

        assertEquals(null, state.getAuxiliaryType());
        assertEquals(null, state.getTimeoutEventId());
        assertEquals(null, state.getTimeoutReturnPhase());
        assertEquals(null, state.getPendingGoalSequenceNo());
        assertEquals(null, state.getPendingGoalSide());
        assertEquals(null, state.getPendingGoalPeriod());
        assertEquals(null, state.getPendingGoalRemainingSeconds());
    }

    @Test
    void finalScoreCorrectionCanReopenFinishedMatch() {
        ScoreboardMatch match = new ScoreboardMatch();
        match.setId(10L);
        match.setStatus("FINISHED");
        match.setFinishedAt(LocalDateTime.now());
        when(matchMapper.selectById(10L)).thenReturn(match);
        ScoreboardMatchState state = readyState(10L, 2);
        state.setPhase("FINISHED");
        when(stateMapper.selectById(10L)).thenReturn(state);
        when(stateMapper.updateById(state)).thenReturn(1);
        when(eventMapper.selectOne(any())).thenReturn(null);

        MatchStateSaveDTO request = new MatchStateSaveDTO();
        request.setExpectedVersion(2);
        request.setPhase("PERIOD_END");
        request.setHomeScore(3);
        request.setAwayScore(3);

        service.saveState(10L, request);

        assertEquals("ONGOING", match.getStatus());
        assertEquals(null, match.getFinishedAt());
        verify(matchMapper).updateById(match);
    }

    @Test
    void goalEventRejectsPlayerFromOtherTeam() {
        ScoreboardMatch match = new ScoreboardMatch();
        match.setId(10L);
        when(matchMapper.selectById(10L)).thenReturn(match);
        ScoreboardMatchState state = new ScoreboardMatchState();
        state.setCurrentPeriod(1);
        state.setMainRemainingSeconds(300);
        state.setStateVersion(2);
        when(stateMapper.selectById(10L)).thenReturn(state);
        ScoreboardMatchRoster player = new ScoreboardMatchRoster();
        player.setId(20L);
        player.setMatchId(10L);
        player.setTeamSide("away");
        when(rosterMapper.selectById(20L)).thenReturn(player);
        MatchEventCreateDTO request = new MatchEventCreateDTO();
        request.setExpectedVersion(2);
        request.setEventType("GOAL");
        request.setTeamSide("home");
        request.setRosterId(20L);
        request.setPoints(1);

        assertThrows(BusinessException.class, () -> service.appendEvent(10L, request));
        verify(eventMapper, never()).insert(any(com.edus.score.entity.ScoreboardMatchEvent.class));
    }

    @Test
    void goalEventTextIsGeneratedByBackend() {
        ScoreboardMatch match = new ScoreboardMatch();
        match.setId(10L);
        match.setHomeTeamName("主队");
        match.setAwayTeamName("客队");
        when(matchMapper.selectById(10L)).thenReturn(match);
        ScoreboardMatchState state = new ScoreboardMatchState();
        state.setMatchId(10L);
        state.setCurrentPeriod(1);
        state.setMainRemainingSeconds(300);
        state.setStateVersion(2);
        when(stateMapper.selectById(10L)).thenReturn(state);
        when(stateMapper.updateById(state)).thenReturn(1);
        ScoreboardMatchRoster player = new ScoreboardMatchRoster();
        player.setId(20L);
        player.setMatchId(10L);
        player.setTeamSide("home");
        player.setJerseyNo(8);
        player.setPlayerName("张三");
        when(rosterMapper.selectById(20L)).thenReturn(player);

        MatchEventCreateDTO request = new MatchEventCreateDTO();
        request.setExpectedVersion(2);
        request.setEventType("GOAL");
        request.setTeamSide("home");
        request.setRosterId(20L);
        request.setAssistName("12号李四");
        request.setPoints(1);
        request.setRuleReference("6.6 / 9.4");
        service.appendEvent(10L, request);

        ArgumentCaptor<ScoreboardMatchEvent> captor = ArgumentCaptor.forClass(ScoreboardMatchEvent.class);
        verify(eventMapper).insert(captor.capture());
        assertEquals("主队进球", captor.getValue().getEventTitle());
        assertEquals("主队 8号张三进球，助攻：12号李四，增加1分。", captor.getValue().getEventText());
        assertEquals("12号李四", captor.getValue().getAssistName());
        assertEquals("6.6 / 9.4", captor.getValue().getRuleReference());
    }

    @Test
    void penaltyEventAllowsManualPlayerSnapshotsWithoutRoster() {
        ScoreboardMatch match = new ScoreboardMatch();
        match.setId(10L);
        match.setHomeTeamName("主队");
        match.setAwayTeamName("客队");
        when(matchMapper.selectById(10L)).thenReturn(match);
        ScoreboardMatchState state = new ScoreboardMatchState();
        state.setMatchId(10L);
        state.setCurrentPeriod(1);
        state.setMainRemainingSeconds(300);
        state.setStateVersion(2);
        when(stateMapper.selectById(10L)).thenReturn(state);
        when(stateMapper.updateById(state)).thenReturn(1);
        doAnswer(invocation -> {
            invocation.<ScoreboardMatchEvent>getArgument(0).setId(101L);
            return 1;
        }).when(eventMapper).insert(any(ScoreboardMatchEvent.class));

        MatchEventCreateDTO request = new MatchEventCreateDTO();
        request.setExpectedVersion(2);
        request.setEventType("PENALTY");
        request.setTeamSide("away");
        request.setPenaltySubject("player");
        request.setPenaltyType("major");
        request.setPenaltySeconds(60);
        request.setSubjectName("  临时7号  ");
        request.setServedByName("  临时8号  ");

        service.appendEvent(10L, request);

        ArgumentCaptor<ScoreboardMatchEvent> captor = ArgumentCaptor.forClass(ScoreboardMatchEvent.class);
        verify(eventMapper).insert(captor.capture());
        assertEquals("临时7号", captor.getValue().getSubjectName());
        assertEquals("临时8号", captor.getValue().getServedByName());
        assertEquals("客队 临时7号被判黄牌，罚时60秒，由临时8号服罚。计时停止于 05:00。",
                captor.getValue().getEventText());
        verify(penaltyMapper).insert(any(ScoreboardMatchPenalty.class));
    }

    private MatchCreateDTO baseCreateRequest() {
        MatchCreateDTO request = new MatchCreateDTO();
        request.setTitle("测试比赛");
        request.setMatchDate(LocalDate.of(2026, 10, 21));
        request.setGroupRuleId(1L);
        request.setAgeGroup("sll-sixes");
        request.setHomeTeamName("主队");
        request.setAwayTeamName("客队");
        return request;
    }

    private ScoreboardMatchState readyState(Long matchId, int version) {
        ScoreboardMatchState state = new ScoreboardMatchState();
        state.setMatchId(matchId);
        state.setPhase("READY");
        state.setCurrentPeriod(1);
        state.setMainRemainingSeconds(480);
        state.setShotRemainingSeconds(30);
        state.setAuxiliaryRemainingSeconds(0);
        state.setMainRunning(0);
        state.setAuxiliaryRunning(0);
        state.setBallState("dead");
        state.setFrontcourt(0);
        state.setResetShotOnResume(0);
        state.setGoldenGoal(0);
        state.setGoldenConflict(0);
        state.setHomeTimeoutsRemaining(1);
        state.setAwayTimeoutsRemaining(1);
        state.setStateVersion(version);
        return state;
    }

    private ScoreboardGroupRule defaultRule() {
        ScoreboardGroupRule rule = new ScoreboardGroupRule();
        rule.setId(1L);
        rule.setAgeGroup("sll-sixes");
        rule.setTemplateName("六人制默认模板");
        rule.setEnabled(1);
        rule.setPlayersPerTeam(6);
        rule.setRegularPeriods(4);
        rule.setPeriodMinutes(8);
        rule.setHalftimeAfterPeriod(2);
        rule.setShotClockSeconds(30);
        rule.setClearSeconds(10);
        rule.setPointsPerGoal(1);
        rule.setPeriodBreakMinutes(2);
        rule.setHalftimeMinutes(5);
        rule.setTimeoutSeconds(60);
        rule.setTimeoutsPerHalf(1);
        rule.setOvertimeTimeouts(0);
        rule.setStopWindowSeconds(120);
        rule.setAllDeadStop(0);
        rule.setFoulStop(0);
        rule.setOvertimeStopAll(1);
        rule.setShotLastOff(1);
        rule.setOvertimePolicy("always");
        rule.setOvertimeMinutes(3);
        rule.setGoldenGoal(0);
        rule.setOvertimeBreakMinutes(2);
        rule.setMinorPenaltySeconds(30);
        rule.setMajorPenaltySeconds(60);
        rule.setExpulsionPenaltySeconds(120);
        rule.setMajorLimit(3);
        rule.setGoalieProxy(1);
        rule.setPassRequired(0);
        rule.setAlternateFirst("manual");
        rule.setAlternateReset(0);
        return rule;
    }
}
