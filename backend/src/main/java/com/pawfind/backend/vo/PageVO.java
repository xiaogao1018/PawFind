package com.pawfind.backend.vo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.List;
import java.util.function.Function;

public record PageVO<T>(int page, int size, long total, long totalPages, List<T> records) {

    public static long offset(int page, int size) {
        if (page < 1 || size < 1 || size > 50) {
            throw new IllegalArgumentException("页码必须大于0，每页数量必须在1到50之间");
        }
        return ((long) page - 1) * size;
    }

    public static <E, T> PageVO<T> query(BaseMapper<E> mapper, LambdaQueryWrapper<E> filter,
                                        SFunction<E, ?> id, int page, int size, Function<E, T> toView) {
        long offset = offset(page, size);
        long total = mapper.selectCount(filter.clone().select(id));
        List<T> records = offset >= total ? List.of() : mapper
                .selectList(filter.orderByDesc(id).last("LIMIT " + size + " OFFSET " + offset))
                .stream().map(toView).toList();
        return new PageVO<>(page, size, total, (total + size - 1) / size, records);
    }
}
