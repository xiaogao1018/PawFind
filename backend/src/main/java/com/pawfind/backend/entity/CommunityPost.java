package com.pawfind.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_post")
public class CommunityPost {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("author_id")
    private Long userId;
    private String title;
    private String content;
    private String category;
    private String auditStatus;
    private String visibilityStatus;
    private String moderationReason;
    private LocalDateTime createdAt;
    @TableField(exist = false)
    private String authorName;
    @TableField(exist = false)
    private Long commentCount;
}
