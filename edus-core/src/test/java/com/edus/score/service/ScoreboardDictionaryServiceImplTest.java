package com.edus.score.service;

import com.edus.common.exception.BusinessException;
import com.edus.score.dto.res.ScoreboardDictItemVO;
import com.edus.score.mapper.ScoreboardDictionaryMapper;
import com.edus.score.service.impl.ScoreboardDictionaryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreboardDictionaryServiceImplTest {
    @Mock
    private ScoreboardDictionaryMapper dictionaryMapper;

    @Test
    void listAgeGroupsReadsEnabledDictionaryItems() {
        List<ScoreboardDictItemVO> items = List.of(
                new ScoreboardDictItemVO("U8", "U8组", 1),
                new ScoreboardDictItemVO("U10", "U10组", 2));
        when(dictionaryMapper.selectEnabledItems("team_age_group")).thenReturn(items);

        ScoreboardDictionaryService service = new ScoreboardDictionaryServiceImpl(dictionaryMapper);

        assertEquals(items, service.listAgeGroups());
    }

    @Test
    void requireEnabledAgeGroupRejectsValueOutsideDictionary() {
        when(dictionaryMapper.countEnabledItem("team_age_group", "手工组别")).thenReturn(0L);
        ScoreboardDictionaryService service = new ScoreboardDictionaryServiceImpl(dictionaryMapper);

        assertThrows(BusinessException.class,
                () -> service.requireEnabledAgeGroup("手工组别"));
    }
}
