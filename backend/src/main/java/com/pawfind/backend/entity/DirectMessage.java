package com.pawfind.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("direct_message")
public class DirectMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long senderId;
    private Long recipientId;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
