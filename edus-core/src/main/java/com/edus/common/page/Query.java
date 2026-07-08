package com.edus.common.page;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.HashMap;
import java.util.Map;

/**
 * 查询参数 - 参照人人开发平台设计（极简版）
 *
 * @author edus
 */
public class Query<T> {

    private static final long DEFAULT_PAGE = 1L;
    private static final long DEFAULT_LIMIT = 20L;
    private static final long MAX_LIMIT = 1000L;

    /**
     * 获取分页对象 - 简单版本（无排序）
     */
    public IPage<T> getPage(Map<String, Object> params) {
        return this.getPage(params, (OrderItem[]) null);
    }

    /**
     * 获取分页对象 - 支持多字段排序
     * @param params 查询参数
     * @param orderItems 排序项数组
     * @return 分页对象
     */
    @SafeVarargs
    public final IPage<T> getPage(Map<String, Object> params, OrderItem... orderItems) {
        if (params == null) {
            params = new HashMap<>();
        }

        // 解析分页参数
        long curPage = getCurPage(params);
        long limit = getLimit(params);

        // 创建分页对象
        Page<T> page = new Page<>(curPage, limit);
        params.put(Constant.PAGE, page);

        // 应用排序
        applySort(page, params, orderItems);

        return page;
    }

    /**
     * 解析当前页码
     */
    private long getCurPage(Map<String, Object> params) {
        return parsePositiveLong(params.get(Constant.PAGE), DEFAULT_PAGE, Long.MAX_VALUE);
    }

    /**
     * 解析每页大小
     */
    private long getLimit(Map<String, Object> params) {
        return parsePositiveLong(params.get(Constant.LIMIT), DEFAULT_LIMIT, MAX_LIMIT);
    }

    private long parsePositiveLong(Object value, long defaultValue, long maxValue) {
        if (value == null) {
            return defaultValue;
        }

        String text = StrUtil.toString(value).trim();
        if (StrUtil.isBlank(text)) {
            return defaultValue;
        }

        try {
            long parsed = Long.parseLong(text);
            if (parsed <= 0) {
                return defaultValue;
            }
            return Math.min(parsed, maxValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 应用排序 - 优先使用前端排序，否则使用默认排序
     */
    private void applySort(Page<T> page, Map<String, Object> params, OrderItem... defaultOrderItems) {
        // 前端字段排序优先
        if(hasFrontendSort(params)) {
            applyFrontendSort(page, params);
            return;
        }

        // 没有前端排序，使用默认排序
        if(defaultOrderItems != null && defaultOrderItems.length > 0) {
            page.addOrder(defaultOrderItems);
        }
    }

    /**
     * 检查是否有前端排序
     */
    private boolean hasFrontendSort(Map<String, Object> params) {
        String orderField = (String)params.get(Constant.ORDER_FIELD);
        String order = (String)params.get(Constant.ORDER);
        return StrUtil.isNotEmpty(orderField) && StrUtil.isNotEmpty(order);
    }

    /**
     * 应用前端排序 - 使用 SQLFilter 防止 SQL 注入
     */
    private void applyFrontendSort(Page<T> page, Map<String, Object> params) {
        String orderField = (String)params.get(Constant.ORDER_FIELD);
        String order = (String)params.get(Constant.ORDER);

        // SQL注入过滤
        orderField = SQLFilter.sqlInject(orderField);

        if(Constant.ASC.equalsIgnoreCase(order)) {
            page.addOrder(OrderItem.asc(orderField));
        } else {
            page.addOrder(OrderItem.desc(orderField));
        }
    }
}
