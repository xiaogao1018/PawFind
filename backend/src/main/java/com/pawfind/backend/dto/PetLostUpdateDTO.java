
package com.pawfind.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PetLostUpdateDTO {

    @Size(max = 50, message = "宠物名称不能超过50个字符")
    private String petName;

    @NotBlank(message = "宠物种类不能为空")
    @Size(max = 30, message = "宠物种类不能超过30个字符")
    private String species;

    @Size(max = 2000, message = "描述不能超过2000个字符")
    private String description;

    @NotNull(message = "走失时间不能为空")
    @PastOrPresent(message = "走失时间不能晚于当前时间")
    private LocalDateTime lostTime;

    @NotBlank(message = "走失地点不能为空")
    @Size(max = 255, message = "走失地点不能超过255个字符")
    private String lostLocation;
}
