
package com.pawfind.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("pet_lost")
public class PetLost {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String petName;
    private String species;
    private String description;

    private LocalDateTime lostTime;
    private String lostLocation;

    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
