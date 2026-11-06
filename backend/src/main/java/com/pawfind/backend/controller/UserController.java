
package com.pawfind.backend.controller;

import com.pawfind.backend.entity.SysUser;
import com.pawfind.backend.service.UserService;
import com.pawfind.backend.vo.UserInfoVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


//声明这是接收 HTTP 请求的控制器
@RestController
//定义接口的公共路径
@RequestMapping("/api/users")
public class UserController {

    //注入 UserService，用于业务处理
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    //处理 GET 请求，获取用户信息
    @GetMapping("/{id}")
    public UserInfoVO getUserById(@PathVariable Long id) {
        
        SysUser user = userService.getUserById(id);

        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());

        return vo;
    }
}
