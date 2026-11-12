package com.pawfind.backend.dto;

import jakarta.validation.constraints.Size;

public record AiSessionCreateDTO(
        @Size(max = 100, message = "会话标题不能超过100字") String title
) {
}
