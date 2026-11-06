
package com.pawfind.backend.service.impl;

import com.pawfind.backend.entity.SysUser;
import com.pawfind.backend.mapper.SysUserMapper;
import com.pawfind.backend.service.UserService;
import org.springframework.stereotype.Service;

//告诉 Spring：这是一个业务层组件/服务类
@Service
//实现 UserService 接口，提供用户查询功能
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    //注入 SysUserMapper，用于数据库操作

    public UserServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    //实现 UserService 接口的 getUserById 方法
    @Override
    public SysUser getUserById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        //表示：Service 不亲自执行 SQL，而是让 Mapper 查询数据库，再返回 SysUser 对象。
        return sysUserMapper.selectById(id);
    }
}
