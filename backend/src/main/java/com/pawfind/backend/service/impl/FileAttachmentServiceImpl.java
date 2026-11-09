
package com.pawfind.backend.service.impl;

import com.pawfind.backend.common.exception.ForbiddenException;
import com.pawfind.backend.common.exception.ResourceNotFoundException;
import com.pawfind.backend.entity.FileAttachment;
import com.pawfind.backend.entity.PetLost;
import com.pawfind.backend.mapper.FileAttachmentMapper;
import com.pawfind.backend.service.FileAttachmentService;
import com.pawfind.backend.vo.FileAttachmentVO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileAttachmentServiceImpl
        implements FileAttachmentService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_IMAGES = 5;

    private final FileAttachmentMapper attachmentMapper;
    private final Path uploadRoot;

    public FileAttachmentServiceImpl(
            FileAttachmentMapper attachmentMapper,
            @Value("${pawfind.upload-dir:uploads}") String uploadDir) {

        this.attachmentMapper = attachmentMapper;
        this.uploadRoot = Path.of(uploadDir)
                .toAbsolutePath()
                .normalize();
    }

    // 上传图片
    @Override
    @Transactional
    public FileAttachmentVO uploadImage(
            Long petLostId,
            Long currentUserId,
            MultipartFile file) {

        if (petLostId == null || petLostId <= 0) {
            throw new IllegalArgumentException("走失记录ID不合法");
        }

        if (currentUserId == null || currentUserId <= 0) {
            throw new IllegalArgumentException("请先登录");
        }

        // 锁定所属走失记录
        PetLost petLost = attachmentMapper.lockPetLost(petLostId);

        if (petLost == null) {
            throw new ResourceNotFoundException("走失信息不存在");
        }

        // 只有发布者可以上传
        if (!Objects.equals(petLost.getUserId(), currentUserId)) {
            throw new ForbiddenException("只能上传自己发布的信息的图片");
        }

        // 只允许寻找中的记录上传图片
        if (!"LOOKING".equals(petLost.getStatus())) {
            throw new IllegalArgumentException(
                    "只有寻找中的走失信息可以上传图片"
            );
        }

        // 最多5张
        long count = attachmentMapper.countByPetLostId(petLostId);

        if (count >= MAX_IMAGES) {
            throw new IllegalArgumentException(
                    "每条走失信息最多上传5张图片"
            );
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择图片");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "单张图片不能超过5MB"
            );
        }

        // 验证文件实际内容
        byte[] bytes;

        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("读取上传图片失败", e);
        }

        String format = detectImageFormat(bytes);

        String contentType;
        String extension;

        switch (format) {
            case "JPEG" -> {
                contentType = "image/jpeg";
                extension = ".jpg";
            }
            case "PNG" -> {
                contentType = "image/png";
                extension = ".png";
            }
            case "WEBP" -> {
                contentType = "image/webp";
                extension = ".webp";
            }
            default -> throw new IllegalArgumentException(
                    "只允许上传JPEG、PNG、WebP图片"
            );
        }

        // 检查扩展名和实际格式是否一致
        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        originalName = originalName
                .replace('\\', '/');

        originalName = originalName.substring(
                originalName.lastIndexOf('/') + 1
        );

        if (originalName.isBlank()
                || originalName.length() > 255) {
            throw new IllegalArgumentException("文件名不合法");
        }

        String lowerName = originalName.toLowerCase(
                java.util.Locale.ROOT
        );

        boolean extensionMatches = switch (format) {
            case "JPEG" -> lowerName.endsWith(".jpg")
                    || lowerName.endsWith(".jpeg");
            case "PNG" -> lowerName.endsWith(".png");
            case "WEBP" -> lowerName.endsWith(".webp");
            default -> false;
        };

        if (!extensionMatches) {
            throw new IllegalArgumentException(
                    "文件扩展名与图片格式不一致"
            );
        }

        // UUID生成随机文件名
        String storageKey = UUID.randomUUID() + extension;

        Path target = uploadRoot
                .resolve(storageKey)
                .normalize();

        if (!target.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("非法文件路径");
        }

        // 保存文件
        try {
            Files.createDirectories(uploadRoot);

            Files.write(
                    target,
                    bytes,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            throw new IllegalStateException("保存图片失败", e);
        }

        // 数据库事务失败时，清理已写入的文件
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            try {
                                Files.deleteIfExists(target);
                            } catch (IOException ignored) {
                                // 清理失败时保留日志排查空间
                            }
                        }
                    }
                }
        );

        // 保存图片和走失记录的关联
        FileAttachment attachment = new FileAttachment();

        attachment.setPetLostId(petLostId);
        attachment.setOriginalFilename(originalName);
        attachment.setStorageKey(storageKey);
        attachment.setContentType(contentType);
        attachment.setFileSize((long) bytes.length);
        attachment.setCreatedAt(LocalDateTime.now());

        attachmentMapper.insert(attachment);

        return toVO(attachment);
    }

    // 查询图片列表
    @Override
    public List<FileAttachmentVO> listImages(Long petLostId) {

        checkVisiblePet(petLostId);

        return attachmentMapper
                .selectByPetLostId(petLostId)
                .stream()
                .map(this::toVO)
                .toList();
    }

    // 读取图片内容
    @Override
    public byte[] readImage(
            Long petLostId,
            Long attachmentId) {

        FileAttachment attachment =
                getVisibleAttachment(petLostId, attachmentId);

        Path target = uploadRoot
                .resolve(attachment.getStorageKey())
                .normalize();

        if (!target.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("非法文件路径");
        }

        if (!Files.isRegularFile(
                target, LinkOption.NOFOLLOW_LINKS)) {
            throw new ResourceNotFoundException("图片文件不存在");
        }

        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new IllegalStateException("读取图片失败", e);
        }
    }

    // 读取图片类型
    @Override
    public String getImageContentType(
            Long petLostId,
            Long attachmentId) {

        return getVisibleAttachment(
                petLostId, attachmentId
        ).getContentType();
    }

    // 检查记录存在且未撤回
    private void checkVisiblePet(Long petLostId) {

        if (petLostId == null || petLostId <= 0) {
            throw new IllegalArgumentException("走失记录ID不合法");
        }

        // 这里只查询所属记录是否存在并且可展示
        PetLost petLost = attachmentMapper.lockFreeGetPet(petLostId);

        if (petLost == null
                || "WITHDRAWN".equals(petLost.getStatus())) {
            throw new ResourceNotFoundException(
                    "走失信息不存在或已撤回"
            );
        }
    }

    // 检查图片确实属于指定走失记录
    private FileAttachment getVisibleAttachment(
            Long petLostId,
            Long attachmentId) {

        checkVisiblePet(petLostId);

        if (attachmentId == null || attachmentId <= 0) {
            throw new IllegalArgumentException("图片ID不合法");
        }

        FileAttachment attachment =
                attachmentMapper.selectById(attachmentId);

        if (attachment == null
                || !Objects.equals(
                        attachment.getPetLostId(), petLostId)) {
            throw new ResourceNotFoundException("图片不存在");
        }

        return attachment;
    }

    // 检查图片真实文件头及基本结构
    private String detectImageFormat(byte[] bytes) {

        if (bytes == null
                || bytes.length == 0
                || bytes.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "图片内容为空或超过5MB"
            );
        }

        // JPEG 文件头
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {

            validateDecodedImage(bytes);
            return "JPEG";
        }

        // PNG 文件头
        byte[] pngSignature = {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };

        boolean png = bytes.length >= 8;

        for (int i = 0; png && i < 8; i++) {
            if (bytes[i] != pngSignature[i]) {
                png = false;
            }
        }

        if (png) {
            validateDecodedImage(bytes);
            return "PNG";
        }

        // WebP：RIFF + 文件长度 + WEBP + 合法图片块类型
        if (bytes.length >= 20
                && asciiEquals(bytes, 0, "RIFF")
                && asciiEquals(bytes, 8, "WEBP")) {

            boolean validChunk =
                    asciiEquals(bytes, 12, "VP8 ")
                    || asciiEquals(bytes, 12, "VP8L")
                    || asciiEquals(bytes, 12, "VP8X");

            long riffSize =
                    ((long) bytes[4] & 0xFF)
                    | (((long) bytes[5] & 0xFF) << 8)
                    | (((long) bytes[6] & 0xFF) << 16)
                    | (((long) bytes[7] & 0xFF) << 24);

            if (validChunk && riffSize == bytes.length - 8L) {
                return "WEBP";
            }
        }

        throw new IllegalArgumentException(
                "文件不是有效的JPEG、PNG或WebP图片"
        );
    }

    // JPEG和PNG进一步尝试实际解码
    private void validateDecodedImage(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(
                    new ByteArrayInputStream(bytes)
            );

            if (image == null) {
                throw new IllegalArgumentException(
                        "图片内容无法解码"
                );
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "图片内容损坏", e
            );
        }
    }

    private boolean asciiEquals(
            byte[] bytes,
            int offset,
            String expected) {

        if (bytes.length < offset + expected.length()) {
            return false;
        }

        for (int i = 0; i < expected.length(); i++) {
            if (bytes[offset + i] != (byte) expected.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    // Entity 转换为 VO
    private FileAttachmentVO toVO(FileAttachment attachment) {

        FileAttachmentVO vo = new FileAttachmentVO();

        vo.setId(attachment.getId());
        vo.setPetLostId(attachment.getPetLostId());
        vo.setOriginalFilename(
                attachment.getOriginalFilename()
        );
        vo.setContentType(attachment.getContentType());
        vo.setFileSize(attachment.getFileSize());
        vo.setCreatedAt(attachment.getCreatedAt());

        vo.setUrl(
                "/api/pet-lost/"
                + attachment.getPetLostId()
                + "/images/"
                + attachment.getId()
        );

        return vo;
    }
}
