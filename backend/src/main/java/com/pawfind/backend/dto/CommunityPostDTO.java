package com.pawfind.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CommunityPostDTO(
        @NotBlank(message = "请填写帖子标题")
        @Size(max = 120, message = "帖子标题不能超过120个字符") String title,
        @NotBlank(message = "请填写帖子正文")
        @Size(max = 10000, message = "帖子正文不能超过10000个字符") String content,
        @Pattern(regexp = "EXPERIENCE|HELP|QUESTION|KNOWLEDGE", message = "帖子分类不合法") String category) {
}
