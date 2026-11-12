package com.pawfind.backend.controller;

import com.pawfind.backend.common.result.Result;
import com.pawfind.backend.entity.AdminOperationLog;
import com.pawfind.backend.entity.CommunityPost;
import com.pawfind.backend.entity.PetLost;
import com.pawfind.backend.entity.PetFound;
import com.pawfind.backend.entity.CommunityComment;
import com.pawfind.backend.service.AdminService;
import com.pawfind.backend.service.AdminService.AdminUser;
import com.pawfind.backend.vo.PageVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/community/posts")
    public Result<PageVO<CommunityPost>> listPosts(
            @RequestParam(required = false)
            @Pattern(regexp = "PENDING|APPROVED|REJECTED", message = "审核状态不合法") String auditStatus,
            @RequestParam(required = false)
            @Pattern(regexp = "VISIBLE|HIDDEN", message = "展示状态不合法") String visibilityStatus,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(service.listPosts(auditStatus, visibilityStatus, page, size));
    }

    @PatchMapping("/community/posts/{id}/audit")
    public Result<CommunityPost> audit(@PathVariable long id, @Valid @RequestBody Audit dto,
                                       @RequestAttribute Long currentUserId) {
        return Result.success(service.audit(id, dto.auditStatus(), dto.reason(), currentUserId));
    }

    @PatchMapping("/community/posts/{id}/visibility")
    public Result<CommunityPost> visibility(@PathVariable long id, @Valid @RequestBody Visibility dto,
                                            @RequestAttribute Long currentUserId) {
        return Result.success(service.setVisibility(id, dto.visibilityStatus(), dto.reason(), currentUserId));
    }

    @GetMapping("/users")
    public Result<PageVO<AdminUser>> listUsers(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return Result.success(service.listUsers(page, size));
    }

    @PatchMapping("/users/{id}/status")
    public Result<AdminUser> userStatus(@PathVariable Long id, @Valid @RequestBody UserStatus dto,
                                       @RequestAttribute Long currentUserId) {
        return Result.success(service.updateUserStatus(id, dto.status(), currentUserId));
    }

    @GetMapping("/operation-logs")
    public Result<PageVO<AdminOperationLog>> logs(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return Result.success(service.listLogs(page, size));
    }

    @GetMapping("/pet-lost")
    public Result<PageVO<PetLost>> lost(
            @RequestParam(required = false) @Pattern(regexp = "VISIBLE|HIDDEN") String visibilityStatus,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(service.listLost(visibilityStatus, page, size));
    }

    @GetMapping("/pet-found")
    public Result<PageVO<PetFound>> found(
            @RequestParam(required = false) @Pattern(regexp = "VISIBLE|HIDDEN") String visibilityStatus,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(service.listFound(visibilityStatus, page, size));
    }

    @PatchMapping("/pet-lost/{id}/visibility")
    public Result<Void> lostVisibility(@PathVariable Long id, @Valid @RequestBody Visibility dto,
                                        @RequestAttribute Long currentUserId) {
        service.recordVisibility(true, id, dto.visibilityStatus(), dto.reason(), currentUserId);
        return Result.success(null);
    }

    @PatchMapping("/pet-found/{id}/visibility")
    public Result<Void> foundVisibility(@PathVariable Long id, @Valid @RequestBody Visibility dto,
                                         @RequestAttribute Long currentUserId) {
        service.recordVisibility(false, id, dto.visibilityStatus(), dto.reason(), currentUserId);
        return Result.success(null);
    }

    @GetMapping("/community/comments")
    public Result<PageVO<CommunityComment>> comments(
            @RequestParam(required = false) @Pattern(regexp = "VISIBLE|HIDDEN|DELETED") String visibilityStatus,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(service.listComments(visibilityStatus, page, size));
    }

    @PatchMapping("/community/comments/{id}/visibility")
    public Result<Void> commentVisibility(@PathVariable Long id, @Valid @RequestBody Visibility dto,
                                           @RequestAttribute Long currentUserId) {
        service.commentVisibility(id, dto.visibilityStatus(), dto.reason(), currentUserId);
        return Result.success(null);
    }

    public record Audit(
            @NotBlank(message = "请填写审核结果")
            @Pattern(regexp = "APPROVED|REJECTED", message = "审核结果不合法") String auditStatus,
            @Size(max = 255, message = "原因不能超过255个字符") String reason) {
    }

    public record Visibility(
            @NotBlank(message = "请填写展示状态")
            @Pattern(regexp = "VISIBLE|HIDDEN", message = "展示状态不合法") String visibilityStatus,
            @Size(max = 255, message = "原因不能超过255个字符") String reason) {
    }

    public record UserStatus(
            @NotBlank(message = "请填写账号状态")
            @Pattern(regexp = "ACTIVE|DISABLED", message = "账号状态不合法") String status) {
    }
}
