
package com.pawfind.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pawfind.backend.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

//告诉 Spring，这个接口是 MyBatis 的数据库访问接口。
//BaseMapper 是 MyBatis-Plus 提供的通用 Mapper，提供了常见的 CRUD 操作
//泛型为 SysUser 类，表示操作 SysUser 表
//@Mapper 注解：告诉 Spring Boot 这是一个 MyBatis Mapper
@Mapper

//BaseMapper（框架提供的通用能力）：提供了常见的 CRUD 操作
//extends：继承接口定义的功能
//BaseMapper<SysUser>
//指定操作的是 SysUser 用户实体
public interface SysUserMapper extends BaseMapper<SysUser> {
}
