package com.pawfind.backend.controller;

import com.pawfind.backend.common.result.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {
    @GetMapping("/status")
    public Result<Status> status() {
        return Result.success(new Status(false, "AI 助手正在准备中，敬请期待"));
    }

    // Replace this response with text/event-stream when a model provider is configured.
    @PostMapping("/chat/stream")
    public ResponseEntity<Result<Void>> stream() {
        return ResponseEntity.status(503).contentType(MediaType.APPLICATION_JSON).body(Result.error(503, "AI 助手尚未开放，请稍后再试"));
    }

    public record Status(boolean configured, String message) {
    }
}
