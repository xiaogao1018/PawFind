
package com.pawfind.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pawfind.backend.entity.PetLost;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PetLostMapper extends BaseMapper<PetLost> {

    // 查询正常展示的走失信息分页数据
    @Select("""
        SELECT
            id,
            user_id AS userId,
            pet_name AS petName,
            species,
            description,
            lost_time AS lostTime,
            lost_location AS lostLocation,
            status,
            created_at AS createdAt,
            updated_at AS updatedAt
        FROM pet_lost
        WHERE status IN ('LOOKING', 'FOUND')
        ORDER BY created_at DESC, id DESC
        LIMIT #{offset}, #{size}
        """)
    List<PetLost> selectVisiblePage(
            @Param("offset") long offset,
            @Param("size") int size
    );

    // 查询正常展示的走失信息总数
    @Select("""
        SELECT COUNT(*)
        FROM pet_lost
        WHERE status IN ('LOOKING', 'FOUND')
        """)
    long countVisible();
}
