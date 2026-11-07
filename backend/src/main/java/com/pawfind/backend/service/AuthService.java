
package com.pawfind.backend.service;

import com.pawfind.backend.dto.RegisterDTO;
import com.pawfind.backend.vo.UserInfoVO;
import com.pawfind.backend.dto.LoginDTO;
import com.pawfind.backend.vo.LoginVO;

public interface AuthService {
//“给我一份用户注册表 RegisterDTO，我完成注册后，返回一份不包含密码的用户信息 UserInfoVO。”
    UserInfoVO register(RegisterDTO dto);

    LoginVO login(LoginDTO dto);
}
