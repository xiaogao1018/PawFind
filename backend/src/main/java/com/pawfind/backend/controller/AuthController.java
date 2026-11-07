
package com.pawfind.backend.controller;

import com.pawfind.backend.common.result.Result;
import com.pawfind.backend.dto.RegisterDTO;
import com.pawfind.backend.service.AuthService;
import com.pawfind.backend.vo.UserInfoVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.pawfind.backend.dto.LoginDTO;
import com.pawfind.backend.vo.LoginVO;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Result<UserInfoVO>> register(
            @Valid @RequestBody RegisterDTO dto) {

        UserInfoVO user = authService.register(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Result.success(user));
    }

    @PostMapping("/login")
    public ResponseEntity<Result<LoginVO>> login(
            @Valid @RequestBody LoginDTO dto) {

        LoginVO loginResult = authService.login(dto);

        return ResponseEntity.ok(
                Result.success(loginResult)
        );
    }
}
