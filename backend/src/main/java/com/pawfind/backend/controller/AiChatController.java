package com.pawfind.backend.controller;

import com.pawfind.backend.common.result.Result;
import com.pawfind.backend.dto.AiChatRequest;
import com.pawfind.backend.dto.AiSessionCreateDTO;
import com.pawfind.backend.entity.AiChatMessage;
import com.pawfind.backend.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {
    private final AiChatService service;

    public AiChatController(AiChatService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public Result<AiChatService.Status> status() {
        return Result.success(service.status());
    }

    @GetMapping("/sessions")
    public Result<List<AiChatService.SessionSummary>> sessions(
            @RequestAttribute("currentUserId") Long userId) {
        return Result.success(service.listSessions(userId));
    }

    @PostMapping("/sessions")
    public Result<AiChatService.SessionSummary> createSession(
            @RequestAttribute("currentUserId") Long userId,
            @Valid @RequestBody(required = false) AiSessionCreateDTO body) {
        return Result.success(service.createSession(userId, body == null ? null : body.title()));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<AiChatMessage>> messages(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long sessionId) {
        return Result.success(service.listMessages(userId, sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long sessionId) {
        service.deleteSession(userId, sessionId);
        return Result.success(null);
    }

    @PostMapping("/chat")
    public Result<AiChatService.ChatReply> chat(
            @RequestAttribute("currentUserId") Long userId,
            @Valid @RequestBody AiChatRequest request) {
        return Result.success(service.chat(userId, request));
    }

    @PostMapping(value = "/chat/stream", produces = {MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> stream(
            @RequestAttribute("currentUserId") Long userId,
            @Valid @RequestBody AiChatRequest request) {
        AiChatService.Status status = service.status();
        if (!status.configured()) {
            return ResponseEntity.status(503).contentType(MediaType.APPLICATION_JSON)
                    .body(Result.error(503, "AI 尚未启用，请先配置 DeepSeek API Key"));
        }
        Flux<ServerSentEvent<AiChatService.StreamEvent>> body = service.stream(userId, request)
                .map(item -> ServerSentEvent.<AiChatService.StreamEvent>builder(item)
                        .event(item.event()).build());
        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(body);
    }
}
