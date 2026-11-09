
package com.pawfind.backend.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class FileAttachmentVO {

    private Long id;
    private Long petLostId;

    private String originalFilename;
    private String contentType;
    private Long fileSize;

    // 图片访问接口地址
    private String url;

    private LocalDateTime createdAt;
}
