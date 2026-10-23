package com.edus.score.service.impl;

import cn.hutool.core.util.StrUtil;
import com.edus.common.exception.BusinessException;
import com.edus.score.dto.res.ScoreboardDictItemVO;
import com.edus.score.mapper.ScoreboardDictionaryMapper;
import com.edus.score.service.ScoreboardDictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoreboardDictionaryServiceImpl implements ScoreboardDictionaryService {
    private static final String AGE_GROUP_TYPE = "team_age_group";

    private final ScoreboardDictionaryMapper dictionaryMapper;

    @Override
    public List<ScoreboardDictItemVO> listAgeGroups() {
        return dictionaryMapper.selectEnabledItems(AGE_GROUP_TYPE);
    }

    @Override
    public String requireEnabledAgeGroup(String ageGroup) {
        String value = StrUtil.trim(ageGroup);
        if (StrUtil.isBlank(value)
                || dictionaryMapper.countEnabledItem(AGE_GROUP_TYPE, value) <= 0) {
            throw BusinessException.validationError("请选择有效的比赛组别");
        }
        return value;
    }
}
