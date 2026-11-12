package com.pawfind.backend.controller;

import com.pawfind.backend.common.result.Result;
import com.pawfind.backend.entity.DirectMessage;
import com.pawfind.backend.mapper.DirectMessageMapper;
import com.pawfind.backend.service.DirectMessageService;
import com.pawfind.backend.service.NotificationService;
import com.pawfind.backend.vo.PageVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final NotificationService notices;
    private final DirectMessageService direct;

    public MessageController(NotificationService notices, DirectMessageService direct) {
        this.notices = notices;
        this.direct = direct;
    }

    @GetMapping("/unread")
    public Result<NotificationService.Unread> unread(@RequestAttribute Long currentUserId) {
        return Result.success(notices.unread(currentUserId));
    }

    @GetMapping("/notifications")
    public Result<PageVO<NotificationService.Notice>> notices(@RequestAttribute Long currentUserId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(notices.list(currentUserId, page, size));
    }

    @PatchMapping("/notifications/{id}/read")
    public Result<Void> readNotice(@PathVariable Long id, @RequestAttribute Long currentUserId) {
        notices.read(currentUserId, id);
        return Result.success(null);
    }

    @PatchMapping("/notifications/read")
    public Result<Void> readNotices(@Valid @RequestBody ReadThrough dto, @RequestAttribute Long currentUserId) {
        notices.readThrough(currentUserId, dto.upToId());
        return Result.success(null);
    }

    @GetMapping("/recipients")
    public Result<DirectMessageService.Peer> findPeer(
            @RequestParam @NotBlank @Size(max = 50) String username, @RequestAttribute Long currentUserId) {
        return Result.success(direct.findPeer(username, currentUserId));
    }

    @GetMapping("/recipients/{id}")
    public Result<DirectMessageService.Peer> peer(@PathVariable Long id, @RequestAttribute Long currentUserId) {
        return Result.success(direct.peer(id, currentUserId));
    }

    @GetMapping("/conversations")
    public Result<PageVO<DirectMessageMapper.Conversation>> conversations(@RequestAttribute Long currentUserId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(direct.conversations(currentUserId, page, size));
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<DirectMessageService.MessageSlice> thread(@PathVariable Long id,
            @RequestAttribute Long currentUserId, @RequestParam(required = false) Long beforeId,
            @RequestParam(required = false) Long afterId, @RequestParam(defaultValue = "30") int size) {
        return Result.success(direct.thread(currentUserId, id, beforeId, afterId, size));
    }

    @PostMapping("/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<DirectMessage> send(@PathVariable Long id, @Valid @RequestBody SendMessage dto,
                                      @RequestAttribute Long currentUserId) {
        return Result.success(direct.send(currentUserId, id, dto.content()));
    }

    @PatchMapping("/conversations/{id}/read")
    public Result<Void> readConversation(@PathVariable Long id, @Valid @RequestBody ReadThrough dto,
                                         @RequestAttribute Long currentUserId) {
        direct.read(currentUserId, id, dto.upToId());
        return Result.success(null);
    }

    public record SendMessage(@NotBlank(message = "请填写消息内容")
                              @Size(max = 2000, message = "消息不能超过2000个字符") String content) {
    }

    public record ReadThrough(@NotNull @Positive Long upToId) {
    }
}
