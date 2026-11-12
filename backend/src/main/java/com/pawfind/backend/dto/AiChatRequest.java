package com.pawfind.backend.dto;

import com.pawfind.backend.service.AiAssistantType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
        Long sessionId,
        @NotBlank(message = "请输入要咨询的内容")
        @Size(max = 4000, message = "单条消息不能超过4000字")
        String content,
        AiAssistantType assistantType
) {
    public AiAssistantType resolvedType() {
        return assistantType == null ? AiAssistantType.GENERAL : assistantType;
    }
}
