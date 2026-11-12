package com.pawfind.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record PetFoundDTO(
        @Size(max = 50, message = "宠物名称不能超过50个字符") String petName,
        @Size(max = 30, message = "宠物种类不能超过30个字符") String species,
        @Size(max = 2000, message = "特征描述不能超过2000个字符") String description,
        @NotNull(message = "请填写发现时间")
        @PastOrPresent(message = "发现时间不能晚于当前时间") LocalDateTime foundTime,
        @NotBlank(message = "请填写发现地点")
        @Size(max = 255, message = "发现地点不能超过255个字符") String foundLocation) {
}
