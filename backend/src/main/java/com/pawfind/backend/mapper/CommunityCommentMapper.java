package com.pawfind.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pawfind.backend.entity.CommunityComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface CommunityCommentMapper extends BaseMapper<CommunityComment> {
    @Select("""
        <script>
        SELECT post_id AS postId, COUNT(*) AS total FROM community_comment
        WHERE visibility_status = 'VISIBLE' AND post_id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
        GROUP BY post_id
        </script>
        """)
    List<CommentTotal> countVisibleByPosts(@Param("ids") List<Long> ids);

    record CommentTotal(Long postId, long total) {
    }
}
