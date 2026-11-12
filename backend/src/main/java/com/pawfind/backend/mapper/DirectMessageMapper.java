package com.pawfind.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pawfind.backend.entity.DirectMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DirectMessageMapper extends BaseMapper<DirectMessage> {
    @Select("""
        SELECT COUNT(DISTINCT CASE WHEN sender_id = #{userId} THEN recipient_id ELSE sender_id END)
        FROM direct_message WHERE sender_id = #{userId} OR recipient_id = #{userId}
        """)
    long countConversations(@Param("userId") Long userId);

    @Select("""
        SELECT t.partner_id AS userId, u.username, u.status,
               m.content AS preview, m.created_at AS updatedAt, t.unread AS unreadCount
        FROM (
            SELECT CASE WHEN sender_id = #{userId} THEN recipient_id ELSE sender_id END AS partner_id,
                   MAX(id) AS last_id,
                   SUM(CASE WHEN recipient_id = #{userId} AND is_read = FALSE THEN 1 ELSE 0 END) AS unread
            FROM direct_message
            WHERE sender_id = #{userId} OR recipient_id = #{userId}
            GROUP BY partner_id
        ) t
        JOIN direct_message m ON m.id = t.last_id
        JOIN sys_user u ON u.id = t.partner_id
        ORDER BY t.last_id DESC LIMIT #{size} OFFSET #{offset}
        """)
    List<Conversation> conversations(@Param("userId") Long userId, @Param("offset") long offset,
                                     @Param("size") int size);

    record Conversation(Long userId, String username, String status, String preview,
                        LocalDateTime updatedAt, long unreadCount) {
    }
}
