package com.edus.score.controller;

import com.edus.common.utils.Result;
import com.edus.score.dto.res.ScoreboardDictItemVO;
import com.edus.score.service.ScoreboardDictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 计分器使用的数据字典接口。
 */
@RestController
@RequestMapping("/scoreboard/dictionaries")
@RequiredArgsConstructor
public class ScoreboardDictionaryController {
    private final ScoreboardDictionaryService dictionaryService;

    @GetMapping("/age-groups")
    public Result<List<ScoreboardDictItemVO>> listAgeGroups() {
        return Result.ok(dictionaryService.listAgeGroups());
    }
}
