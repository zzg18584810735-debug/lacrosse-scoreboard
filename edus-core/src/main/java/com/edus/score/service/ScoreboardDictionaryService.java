package com.edus.score.service;

import com.edus.score.dto.res.ScoreboardDictItemVO;

import java.util.List;

public interface ScoreboardDictionaryService {
    List<ScoreboardDictItemVO> listAgeGroups();

    String requireEnabledAgeGroup(String ageGroup);
}
