package com.pawfind.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("pet_profile")
public class PetProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String species;
    private String breed;
    private LocalDate birthDate;
    private String description;
    private LocalDateTime createdAt;
}
