
package com.pawfind.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
//把 Java 类映射到 MySQL 的 sys_user 表
@TableName("sys_user")
public class SysUser {
//表示主键 ID 由 MySQL 自动递增生成。告诉框架哪个字段是主键
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
//密码哈希：存储加密后的密码
    private String passwordHash;

    private String role;

    private String status;
//表示创建时间
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
