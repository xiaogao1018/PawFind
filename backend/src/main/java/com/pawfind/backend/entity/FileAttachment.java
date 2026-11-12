
package com.pawfind.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("file_attachment")
public class FileAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long petLostId;
    private Long petFoundId;
    private Long communityPostId;

    private String originalFilename;
    private String storageKey;
    private String contentType;
    private Long fileSize;

    private LocalDateTime createdAt;
}
