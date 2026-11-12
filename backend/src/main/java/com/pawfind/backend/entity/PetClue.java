package com.pawfind.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("pet_clue")
public class PetClue {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("lost_id")
    private Long petLostId;
    private Long userId;
    private String content;
    private String contactInfo;
    private LocalDateTime sightingTime;
    private String sightingLocation;
    private String status;
    private LocalDateTime createdAt;
}
