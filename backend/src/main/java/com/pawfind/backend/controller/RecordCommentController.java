package com.pawfind.backend.controller;

import com.pawfind.backend.common.result.Result;
import com.pawfind.backend.service.RecordCommentService;
import com.pawfind.backend.vo.PageVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/pet-lost/{id}/comments", "/api/pet-found/{id}/comments"})
public class RecordCommentController {
    private final RecordCommentService service;

    public RecordCommentController(RecordCommentService service) {
        this.service = service;
    }

    private boolean isLost(HttpServletRequest request) {
        String pattern = (String) request.getAttribute(
                org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern.startsWith("/api/pet-lost/");
    }

    @GetMapping
    public Result<PageVO<RecordCommentService.Comment>> list(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        return Result.success(service.list(isLost(request), id, page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<RecordCommentService.Comment> create(@PathVariable Long id,
            @RequestAttribute Long currentUserId, @Valid @RequestBody MessageController.SendMessage dto,
            HttpServletRequest request) {
        return Result.success(service.create(isLost(request), id, currentUserId, dto.content()));
    }
}
