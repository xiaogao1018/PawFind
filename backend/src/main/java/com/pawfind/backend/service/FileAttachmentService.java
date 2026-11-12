package com.pawfind.backend.service;

import com.pawfind.backend.vo.FileAttachmentVO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface FileAttachmentService {
    default FileAttachmentVO uploadImage(Long id, Long userId, MultipartFile file) {
        return uploadImage(AttachmentTarget.LOST, id, userId, file);
    }

    default List<FileAttachmentVO> listImages(Long id) {
        return listImages(AttachmentTarget.LOST, id, null, null);
    }

    default byte[] readImage(Long id, Long attachmentId) {
        return readImage(AttachmentTarget.LOST, id, attachmentId, null, null);
    }

    default String getImageContentType(Long id, Long attachmentId) {
        return getImageContentType(AttachmentTarget.LOST, id, attachmentId, null, null);
    }

    FileAttachmentVO uploadImage(AttachmentTarget target, Long id, Long userId, MultipartFile file);
    List<FileAttachmentVO> listImages(AttachmentTarget target, Long id, Long userId, String role);
    byte[] readImage(AttachmentTarget target, Long id, Long attachmentId, Long userId, String role);
    String getImageContentType(AttachmentTarget target, Long id, Long attachmentId, Long userId, String role);
}
