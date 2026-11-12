package com.pawfind.backend.controller;

import com.pawfind.backend.common.result.Result;
import com.pawfind.backend.dto.CommunityPostDTO;
import com.pawfind.backend.entity.CommunityComment;
import com.pawfind.backend.entity.CommunityPost;
import com.pawfind.backend.service.CommunityService;
import com.pawfind.backend.vo.PageVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community/posts")
public class CommunityController {
    private final CommunityService service;

    public CommunityController(CommunityService service) {
        this.service = service;
    }

    @GetMapping
    public Result<PageVO<CommunityPost>> list(
            @RequestParam(required = false)
            @Pattern(regexp = "EXPERIENCE|HELP|QUESTION|KNOWLEDGE", message = "帖子分类不合法") String category,
            @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return Result.success(service.listPublic(page, size, category));
    }

    @GetMapping("/mine")
    public Result<PageVO<CommunityPost>> mine(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestAttribute Long currentUserId) {
        return Result.success(service.listMine(currentUserId, page, size));
    }

    @GetMapping("/{id}")
    public Result<CommunityPost> get(@PathVariable long id, @RequestAttribute Long currentUserId,
                                     @RequestAttribute String currentUserRole) {
        return Result.success(service.get(id, currentUserId, currentUserRole));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<CommunityPost> create(@Valid @RequestBody CommunityPostDTO dto,
                                        @RequestAttribute Long currentUserId) {
        return Result.success(service.create(dto, currentUserId));
    }

    @PutMapping("/{id}")
    public Result<CommunityPost> update(@PathVariable long id, @Valid @RequestBody CommunityPostDTO dto,
                                        @RequestAttribute Long currentUserId) {
        return Result.success(service.update(id, dto, currentUserId));
    }

    @GetMapping("/{id}/comments")
    public Result<List<CommunityComment>> listComments(@PathVariable long id,
                                                        @RequestAttribute Long currentUserId,
                                                        @RequestAttribute String currentUserRole) {
        return Result.success(service.listComments(id, currentUserId, currentUserRole));
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<CommunityComment> comment(@PathVariable long id, @Valid @RequestBody Comment dto,
                                            @RequestAttribute Long currentUserId) {
        return Result.success(service.comment(id, currentUserId, dto.content()));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable long id, @PathVariable long commentId,
                                       @RequestAttribute Long currentUserId) {
        service.deleteComment(id, commentId, currentUserId);
        return Result.success(null);
    }

    public record Comment(
            @NotBlank(message = "请填写评论内容")
            @Size(max = 2000, message = "评论不能超过2000个字符") String content) {
    }
}
