//VO（View Object）：用来组织返回给前端展示的数据。
package com.pawfind.backend.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserInfoVO {

    private Long id;

    private String username;

    private String role;
}
