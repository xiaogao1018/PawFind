package com.pawfind.backend.controller;

import com.pawfind.backend.common.result.Result;
import com.pawfind.backend.service.AttachmentTarget;
import com.pawfind.backend.service.FileAttachmentService;
import com.pawfind.backend.vo.FileAttachmentVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping({"/api/pet-found/{recordId}/images", "/api/community/posts/{recordId}/images"})
public class RecordImageController {
    private final FileAttachmentService images;

    public RecordImageController(FileAttachmentService images) {
        this.images = images;
    }

    private AttachmentTarget target(jakarta.servlet.http.HttpServletRequest request) {
        // Both mappings share upload, validation and storage behavior.
        String pattern = (String) request.getAttribute(
                org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern.startsWith("/api/pet-found/") ? AttachmentTarget.FOUND : AttachmentTarget.POST;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Result<FileAttachmentVO> upload(@PathVariable Long recordId, @RequestParam MultipartFile file,
                                           @RequestAttribute Long currentUserId,
                                           jakarta.servlet.http.HttpServletRequest request) {
        return Result.success(images.uploadImage(target(request), recordId, currentUserId, file));
    }

    @GetMapping
    public Result<List<FileAttachmentVO>> list(@PathVariable Long recordId,
                                               @RequestAttribute(required = false) Long currentUserId,
                                               @RequestAttribute(required = false) String currentUserRole,
                                               jakarta.servlet.http.HttpServletRequest request) {
        return Result.success(images.listImages(target(request), recordId, currentUserId, currentUserRole));
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<byte[]> read(@PathVariable Long recordId, @PathVariable Long attachmentId,
                                       @RequestAttribute(required = false) Long currentUserId,
                                       @RequestAttribute(required = false) String currentUserRole,
                                       jakarta.servlet.http.HttpServletRequest request) {
        AttachmentTarget target = target(request);
        byte[] bytes = images.readImage(target, recordId, attachmentId, currentUserId, currentUserRole);
        String type = images.getImageContentType(target, recordId, attachmentId, currentUserId, currentUserRole);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type))
                .header("Cache-Control", "private, no-store")
                .header("X-Content-Type-Options", "nosniff").body(bytes);
    }
}
