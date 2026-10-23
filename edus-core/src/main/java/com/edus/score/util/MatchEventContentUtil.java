package com.edus.score.util;

import cn.hutool.core.util.StrUtil;
import com.edus.score.entity.ScoreboardMatch;
import com.edus.score.entity.ScoreboardMatchEvent;
import com.edus.score.entity.ScoreboardMatchRoster;
import com.edus.score.enums.MatchEventType;

/**
 * 比赛事件文案生成工具。
 * 结构化字段是业务事实，生成的文案和规则依据作为事件发生时的展示快照。
 */
public final class MatchEventContentUtil {
    private MatchEventContentUtil() {
    }

    public static String build(MatchEventType type, ScoreboardMatchEvent event,
                               ScoreboardMatch match, ScoreboardMatchRoster subject,
                               ScoreboardMatchRoster assist, ScoreboardMatchRoster server,
                               String description) {
        return switch (type) {
            case MATCH_START -> startText("比赛开始", event, match);
            case PERIOD_START -> startText(StrUtil.format("第{}节开始", event.getPeriodNo()), event, match);
            case PERIOD_END -> StrUtil.format("第{}节结束。", event.getPeriodNo());
            case MATCH_END -> StrUtil.format("比赛结束，比分{}:{}。",
                    match.getHomeScore(), match.getAwayScore());
            case GOAL -> buildGoal(event, match, subject, assist);
            case TEAM_TIMEOUT -> buildTimeout(event, match);
            case PENALTY -> buildPenalty(event, match, subject, server, description);
            case POSSESSION -> StrUtil.format("球权判给{}。", teamName(match, event.getTeamSide()));
            case CORRECTION -> StrUtil.addSuffixIfNot(StrUtil.format("更正事件#{}：{}",
                    event.getRelatedEventId(), StrUtil.trim(event.getCorrectionReason())), "。");
            case SYSTEM -> StrUtil.addSuffixIfNot(StrUtil.trim(description), "。");
        };
    }

    public static String title(MatchEventType type, ScoreboardMatchEvent event,
                               ScoreboardMatch match, String requestedTitle) {
        return switch (type) {
            case MATCH_START -> "比赛开始";
            case PERIOD_START -> StrUtil.format("第{}节开始", event.getPeriodNo());
            case PERIOD_END -> StrUtil.format("第{}节结束", event.getPeriodNo());
            case MATCH_END -> "比赛结束";
            case GOAL -> StrUtil.format("{}进球", teamName(match, event.getTeamSide()));
            case TEAM_TIMEOUT -> StrUtil.format("{}暂停{}", teamName(match, event.getTeamSide()),
                    durationText(event, "（", "）"));
            case PENALTY -> StrUtil.format("{}判罚", teamName(match, event.getTeamSide()));
            case POSSESSION -> StrUtil.format("{}获得球权", teamName(match, event.getTeamSide()));
            case CORRECTION -> StrUtil.blankToDefault(StrUtil.trim(requestedTitle), "比赛记录更正");
            case SYSTEM -> StrUtil.blankToDefault(StrUtil.trim(requestedTitle), "比赛操作");
        };
    }

    private static String buildGoal(ScoreboardMatchEvent event, ScoreboardMatch match,
                                    ScoreboardMatchRoster scorer, ScoreboardMatchRoster assist) {
        String scorerName = scorer == null
                ? StrUtil.blankToDefault(event.getSubjectName(), "球员待补录")
                : playerName(scorer);
        String assistName = assist == null ? StrUtil.trim(event.getAssistName()) : playerName(assist);
        String assistText = StrUtil.isBlank(assistName) ? StrUtil.EMPTY
                : StrUtil.format("，助攻：{}", assistName);
        return StrUtil.format("{} {}进球{}，增加{}分。",
                teamName(match, event.getTeamSide()), scorerName, assistText, event.getPoints());
    }

    private static String startText(String prefix, ScoreboardMatchEvent event, ScoreboardMatch match) {
        return StrUtil.isBlank(event.getTeamSide())
                ? StrUtil.addSuffixIfNot(prefix, "。")
                : StrUtil.format("{}，{}获得开场球权。", prefix, teamName(match, event.getTeamSide()));
    }

    private static String buildTimeout(ScoreboardMatchEvent event, ScoreboardMatch match) {
        String condition = switch (StrUtil.blankToDefault(event.getTimeoutCondition(), StrUtil.EMPTY)) {
            case "BEFORE_START" -> "赛前";
            case "HALFTIME" -> "中场";
            case "FRONTCOURT" -> "前场控球";
            case "DEAD_BALL" -> "死球";
            default -> StrUtil.EMPTY;
        };
        String duration = durationText(event, "", "");
        return StrUtil.isBlank(condition)
                ? StrUtil.format("{}使用{}球队暂停。", teamName(match, event.getTeamSide()), duration)
                : StrUtil.format("{}在{}条件下使用{}球队暂停。",
                teamName(match, event.getTeamSide()), condition, duration);
    }

    private static String durationText(ScoreboardMatchEvent event, String prefix, String suffix) {
        return event.getDurationSeconds() == null || event.getDurationSeconds() <= 0
                ? StrUtil.EMPTY : StrUtil.format("{}{}秒{}", prefix, event.getDurationSeconds(), suffix);
    }

    private static String buildPenalty(ScoreboardMatchEvent event, ScoreboardMatch match,
                                       ScoreboardMatchRoster subject, ScoreboardMatchRoster server,
                                       String description) {
        String subjectName = "official".equals(event.getPenaltySubject())
                ? StrUtil.blankToDefault(event.getSubjectName(), "球队官员")
                : subject == null
                ? StrUtil.blankToDefault(StrUtil.trim(event.getSubjectName()), "球员待补录")
                : playerName(subject);
        String penaltyName = switch (event.getPenaltyType()) {
            case "turnover" -> "仅换球权";
            case "minor" -> "绿牌";
            case "major" -> "黄牌";
            case "expulsion" -> "红牌";
            default -> "判罚";
        };
        String penaltyTime = event.getPenaltySeconds() == null || event.getPenaltySeconds() <= 0
                ? StrUtil.EMPTY : StrUtil.format("，罚时{}秒", event.getPenaltySeconds());
        String serverName = server == null ? StrUtil.trim(event.getServedByName()) : playerName(server);
        String serverText = StrUtil.isBlank(serverName) ? StrUtil.EMPTY
                : StrUtil.format("，由{}服罚", serverName);
        String foulText = StrUtil.isBlank(event.getFoulItem()) ? StrUtil.EMPTY
                : StrUtil.format("，犯规项：{}", event.getFoulItem());
        String clockText = penaltyClockText(event, description);
        return StrUtil.format("{} {}被判{}{}{}{}。{}", teamName(match, event.getTeamSide()),
                subjectName, penaltyName, penaltyTime, serverText, foulText, clockText);
    }

    private static String penaltyClockText(ScoreboardMatchEvent event, String description) {
        boolean stopped = !"turnover".equals(event.getPenaltyType())
                || "计时停止".equals(StrUtil.trim(description));
        if (!stopped) {
            return StrUtil.EMPTY;
        }
        if (event.getRemainingSeconds() == null) {
            return "计时停止。";
        }
        int remainingSeconds = Math.max(0, event.getRemainingSeconds());
        return StrUtil.format("计时停止于 {}。",
                String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60));
    }

    private static String teamName(ScoreboardMatch match, String teamSide) {
        if ("home".equals(teamSide)) {
            return StrUtil.blankToDefault(match.getHomeTeamName(), "主队");
        }
        if ("away".equals(teamSide)) {
            return StrUtil.blankToDefault(match.getAwayTeamName(), "客队");
        }
        return "未指定球队";
    }

    private static String playerName(ScoreboardMatchRoster roster) {
        return StrUtil.format("{}号{}", roster.getJerseyNo(), roster.getPlayerName());
    }
}
