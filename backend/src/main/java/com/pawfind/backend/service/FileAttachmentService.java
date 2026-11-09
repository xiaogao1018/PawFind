
package com.pawfind.backend.service;

import com.pawfind.backend.vo.FileAttachmentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileAttachmentService {

    // 上传并关联图片
    FileAttachmentVO uploadImage(
            Long petLostId,
            Long currentUserId,
            MultipartFile file
    );

    // 查询走失记录的图片列表
    List<FileAttachmentVO> listImages(Long petLostId);

    // 读取指定图片
    byte[] readImage(
            Long petLostId,
            Long attachmentId
    );

    // 获取图片类型，用于响应头
    String getImageContentType(
            Long petLostId,
            Long attachmentId
    );
}
