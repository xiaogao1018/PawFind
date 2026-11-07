
package com.pawfind.backend.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginVO {

    private String token;
    private String tokenType;
    private Long expiresIn;
}
