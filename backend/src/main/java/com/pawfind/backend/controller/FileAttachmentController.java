
package com.pawfind.backend.controller;

import com.pawfind.backend.common.exception.InvalidCredentialsException;
import com.pawfind.backend.common.result.Result;
import com.pawfind.backend.service.FileAttachmentService;
import com.pawfind.backend.vo.FileAttachmentVO;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/pet-lost/{petLostId}/images")
public class FileAttachmentController {

    private final FileAttachmentService attachmentService;

    public FileAttachmentController(
            FileAttachmentService attachmentService) {

        this.attachmentService = attachmentService;
    }

    // 上传单张图片
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Result<FileAttachmentVO>> uploadImage(
            @PathVariable Long petLostId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        Long currentUserId =
                (Long) request.getAttribute("currentUserId");

        if (currentUserId == null) {
            throw new InvalidCredentialsException("请先登录");
        }

        FileAttachmentVO result = attachmentService.uploadImage(
                petLostId,
                currentUserId,
                file
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Result.success(result));
    }

    // 查询一条走失记录的所有图片
    @GetMapping
    public Result<List<FileAttachmentVO>> listImages(
            @PathVariable Long petLostId) {

        return Result.success(
                attachmentService.listImages(petLostId)
        );
    }

    // 返回图片文件
    @GetMapping("/{attachmentId}")
    public ResponseEntity<byte[]> getImage(
            @PathVariable Long petLostId,
            @PathVariable Long attachmentId) {

        byte[] image = attachmentService.readImage(
                petLostId,
                attachmentId
        );

        String contentType = attachmentService.getImageContentType(
                petLostId,
                attachmentId
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(image);
    }
}
