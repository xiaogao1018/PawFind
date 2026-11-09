
package com.pawfind.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pawfind.backend.entity.FileAttachment;
import com.pawfind.backend.entity.PetLost;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FileAttachmentMapper
        extends BaseMapper<FileAttachment> {

    // 查询某条走失记录已上传的图片
    @Select("""
        SELECT *
        FROM file_attachment
        WHERE pet_lost_id = #{petLostId}
        ORDER BY created_at ASC, id ASC
        """)
    List<FileAttachment> selectByPetLostId(
            @Param("petLostId") Long petLostId
    );

    // 统计图片数量
    @Select("""
        SELECT COUNT(*)
        FROM file_attachment
        WHERE pet_lost_id = #{petLostId}
        """)
    long countByPetLostId(
            @Param("petLostId") Long petLostId
    );

    // 锁定走失记录，避免同时上传超过5张
    @Select("""
        SELECT
            id,
            user_id AS userId,
            status
        FROM pet_lost
        WHERE id = #{petLostId}
        FOR UPDATE
        """)
    PetLost lockPetLost(
            @Param("petLostId") Long petLostId
    );

    @Select("""
        SELECT
            id,
            user_id AS userId,
            status
        FROM pet_lost
        WHERE id = #{petLostId}
        """)
    PetLost lockFreeGetPet(
            @Param("petLostId") Long petLostId
    );
}
