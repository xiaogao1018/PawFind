
package com.pawfind.backend.service;

import com.pawfind.backend.entity.SysUser;

public interface UserService {

    // 根据用户 ID 查询用户
    //规定用户业务层必须提供根据 ID 查询用户的能力。
    SysUser getUserById(Long id);
}
