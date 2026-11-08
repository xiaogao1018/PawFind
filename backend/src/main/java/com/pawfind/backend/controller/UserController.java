
package com.pawfind.backend.controller;

import com.pawfind.backend.entity.SysUser;
import com.pawfind.backend.service.UserService;
import com.pawfind.backend.vo.UserInfoVO;
import com.pawfind.backend.common.result.Result;
import com.pawfind.backend.common.exception.ResourceNotFoundException;
import com.pawfind.backend.common.exception.ForbiddenException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 获取当前登录用户的资料
    @GetMapping("/me")
    public Result<UserInfoVO> getCurrentUser(
            HttpServletRequest request) {

        Long currentUserId =
                (Long) request.getAttribute("currentUserId");

        return getUserInfo(currentUserId);
    }

    // 查询指定用户，但只能查询自己的资料
    @GetMapping("/{id}")
    public Result<UserInfoVO> getUserById(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long currentUserId =
                (Long) request.getAttribute("currentUserId");

        if (currentUserId == null || !currentUserId.equals(id)) {
            throw new ForbiddenException("无权查看其他用户资料");
        }

        return getUserInfo(id);
    }

    // 统一查询并转换为 VO
    private Result<UserInfoVO> getUserInfo(Long id) {

        SysUser user = userService.getUserById(id);

        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());

        return Result.success(vo);
    }
}
