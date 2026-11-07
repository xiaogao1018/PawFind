
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

@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder(12);

    public AuthServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
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
        sysUserMapper.insert(user);

        // 8. 只返回安全的用户信息
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());

        return vo;
    }
}
