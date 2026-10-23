package com.edus.score.mapper;

import com.edus.score.dto.res.ScoreboardDictItemVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ScoreboardDictionaryMapper {
    @Select("SELECT item_value AS itemValue, item_label AS itemLabel, sort_no AS sortNo "
            + "FROM sys_dict_item WHERE type_code = #{typeCode} AND status = 1 "
            + "ORDER BY sort_no ASC, id ASC")
    List<ScoreboardDictItemVO> selectEnabledItems(@Param("typeCode") String typeCode);

    @Select("SELECT COUNT(1) FROM sys_dict_item "
            + "WHERE type_code = #{typeCode} AND item_value = #{itemValue} AND status = 1")
    Long countEnabledItem(@Param("typeCode") String typeCode,
                          @Param("itemValue") String itemValue);
}
