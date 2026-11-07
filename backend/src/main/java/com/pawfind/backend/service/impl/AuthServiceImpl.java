
package com.pawfind.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pawfind.backend.common.exception.UsernameAlreadyExistsException;
import com.pawfind.backend.dto.RegisterDTO;
import com.pawfind.backend.entity.SysUser;
import com.pawfind.backend.mapper.SysUserMapper;
import com.pawfind.backend.service.AuthService;
import com.pawfind.backend.vo.UserInfoVO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;


import com.pawfind.backend.common.exception.InvalidCredentialsException;
import com.pawfind.backend.dto.LoginDTO;
import com.pawfind.backend.util.JwtUtil;
import com.pawfind.backend.vo.LoginVO;
import org.springframework.beans.factory.annotation.Value;


@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder(12);
    private final JwtUtil jwtUtil;
    private final long jwtExpirationMs;


    public AuthServiceImpl(
            SysUserMapper sysUserMapper,
            JwtUtil jwtUtil,
            @Value("${jwt.expiration-ms}") long jwtExpirationMs) {

        this.sysUserMapper = sysUserMapper;
        this.jwtUtil = jwtUtil;
        this.jwtExpirationMs = jwtExpirationMs;
    }


    @Override
    public UserInfoVO register(RegisterDTO dto) {

        // 1. 获取注册信息
        String username = dto.getUsername().trim();
        String password = dto.getPassword();

        // 2. BCrypt 对密码有 72 字节限制
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("密码不能超过72字节");
        }

        // 3. 检查用户名是否已经存在
        LambdaQueryWrapper<SysUser> query =
                new LambdaQueryWrapper<>();
//问数据库有几个username是传来的这个
        query.eq(SysUser::getUsername, username);
        Long count = sysUserMapper.selectCount(query);

        if (count > 0) {
            throw new UsernameAlreadyExistsException("用户名已存在");
        }

        // 4. 创建用户实体
        SysUser user = new SysUser();

        user.setUsername(username);

        // 5. 保存 BCrypt 哈希，而不是明文密码
        user.setPasswordHash(passwordEncoder.encode(password));

        // 6. 设置默认角色和状态
        user.setRole("USER");
        user.setStatus("ACTIVE");

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        // 7. 保存到数据库
        
        try {
            sysUserMapper.insert(user);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new UsernameAlreadyExistsException("用户名已存在");
        }

        // 8. 只返回安全的用户信息
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());

        return vo;
    }


    @Override
    public LoginVO login(LoginDTO dto) {

        // 1. 根据用户名查询用户
        LambdaQueryWrapper<SysUser> query =
                new LambdaQueryWrapper<>();

        query.eq(SysUser::getUsername, dto.getUsername());

        SysUser user = sysUserMapper.selectOne(query);

        // 2. 检查用户名和密码
        if (user == null ||
                !passwordEncoder.matches(
                        dto.getPassword(),
                        user.getPasswordHash())) {

            throw new InvalidCredentialsException(
                    "用户名或密码错误"
            );
        }

        // 3. 检查账号状态
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new InvalidCredentialsException(
                    "账号不可登录"
            );
        }

        // 4. 生成 JWT
        String token = jwtUtil.generateToken(user.getId());

        // 5. 返回登录结果
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setTokenType("Bearer");
        vo.setExpiresIn(jwtExpirationMs / 1000);

        return vo;
    }

}
