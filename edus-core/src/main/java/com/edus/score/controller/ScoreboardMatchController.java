package com.edus.score.controller;

import com.edus.common.utils.Result;
import com.edus.score.dto.req.MatchCoachDTO;
import com.edus.score.dto.req.MatchCreateDTO;
import com.edus.score.dto.req.MatchEventCreateDTO;
import com.edus.score.dto.req.MatchInfoUpdateDTO;
import com.edus.score.dto.req.MatchOfficialDTO;
import com.edus.score.dto.req.MatchRosterDTO;
import com.edus.score.dto.req.MatchRecordClearDTO;
import com.edus.score.dto.req.MatchStateSaveDTO;
import com.edus.score.dto.req.PenaltyStateDTO;
import com.edus.score.dto.req.ScoreboardRuleDTO;
import com.edus.score.dto.res.MatchDetailVO;
import com.edus.score.dto.res.MatchOperationVO;
import com.edus.score.dto.res.MatchStateSaveVO;
import com.edus.score.entity.ScoreboardMatch;
import com.edus.score.entity.ScoreboardMatchEvent;
import com.edus.score.entity.ScoreboardMatchPenalty;
import com.edus.score.service.ScoreboardMatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 比赛、名单、计分状态和事件接口。
 */
@Validated
@RestController
@RequestMapping("/scoreboard/matches")
@RequiredArgsConstructor
public class ScoreboardMatchController {
    private final ScoreboardMatchService matchService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody MatchCreateDTO request) {
        return Result.ok("比赛创建成功", matchService.createMatch(request));
    }

    @GetMapping
    public Result<List<ScoreboardMatch>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate matchDate,
            @RequestParam(required = false) String status) {
        return Result.ok(matchService.listMatches(matchDate, status));
    }

    @GetMapping("/{matchId}")
    public Result<MatchDetailVO> detail(@PathVariable Long matchId) {
        return Result.ok(matchService.getDetail(matchId));
    }

    @PostMapping("/{matchId}/records/clear")
    public Result<Void> clearRecords(@PathVariable Long matchId,
                                     @Valid @RequestBody MatchRecordClearDTO request) {
        matchService.clearMatchRecords(matchId, request);
        return Result.ok("比赛记录已清空，赛程已保留");
    }

    @PutMapping("/{matchId}/info")
    public Result<Void> updateInfo(@PathVariable Long matchId,
                                   @Valid @RequestBody MatchInfoUpdateDTO request) {
        matchService.updateMatchInfo(matchId, request);
        return Result.ok("比赛信息修改成功");
    }

    @PutMapping("/{matchId}/rule")
    public Result<Void> updateRule(@PathVariable Long matchId,
                                   @Valid @RequestBody ScoreboardRuleDTO request) {
        matchService.updateMatchRule(matchId, request);
        return Result.ok("本场规则修改成功");
    }

    @PutMapping("/{matchId}/roster")
    public Result<Void> replaceRoster(@PathVariable Long matchId,
                                      @Valid @RequestBody List<MatchRosterDTO> roster) {
        matchService.replaceRoster(matchId, roster);
        return Result.ok("本场名单保存成功");
    }

    @PutMapping("/{matchId}/coaches")
    public Result<Void> replaceCoaches(@PathVariable Long matchId,
                                       @Valid @RequestBody List<MatchCoachDTO> coaches) {
        matchService.replaceCoaches(matchId, coaches);
        return Result.ok("教练信息保存成功");
    }

    @PutMapping("/{matchId}/officials")
    public Result<Void> replaceOfficials(@PathVariable Long matchId,
                                         @Valid @RequestBody List<MatchOfficialDTO> officials) {
        matchService.replaceOfficials(matchId, officials);
        return Result.ok("执裁人员保存成功");
    }

    @PutMapping("/{matchId}/state")
    public Result<MatchStateSaveVO> saveState(@PathVariable Long matchId,
                                              @Valid @RequestBody MatchStateSaveDTO request) {
        return Result.ok("比赛状态保存成功", matchService.saveState(matchId, request));
    }

    @PostMapping("/{matchId}/events")
    public Result<MatchOperationVO<ScoreboardMatchEvent>> appendEvent(
            @PathVariable Long matchId, @Valid @RequestBody MatchEventCreateDTO request) {
        return Result.ok("比赛事件记录成功", matchService.appendEvent(matchId, request));
    }

    @PutMapping("/{matchId}/penalties/{penaltyId}")
    public Result<MatchOperationVO<ScoreboardMatchPenalty>> updatePenaltyState(
            @PathVariable Long matchId, @PathVariable Long penaltyId,
            @Valid @RequestBody PenaltyStateDTO request) {
        return Result.ok("罚时状态修改成功", matchService.updatePenaltyState(matchId, penaltyId, request));
    }
}
