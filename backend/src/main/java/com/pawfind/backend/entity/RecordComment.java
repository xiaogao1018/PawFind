package com.pawfind.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("record_comment")
public class RecordComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long petLostId;
    private Long petFoundId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
}
