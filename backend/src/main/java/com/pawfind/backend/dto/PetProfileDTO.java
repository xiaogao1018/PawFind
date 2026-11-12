package com.pawfind.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PetProfileDTO(
        @NotBlank(message = "请填写宠物名字") @Size(max = 50) String name,
        @NotBlank(message = "请填写宠物种类") @Size(max = 30) String species,
        @Size(max = 50) String breed,
        @PastOrPresent(message = "生日不能晚于今天") LocalDate birthDate,
        @Size(max = 2000) String description) {
}
