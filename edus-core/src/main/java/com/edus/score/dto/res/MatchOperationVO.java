package com.edus.score.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 独立比赛操作结果，携带下一次请求需要使用的状态版本号。
 */
@Data
@AllArgsConstructor
public class MatchOperationVO<T> {
    private Integer stateVersion;
    private T data;
}
