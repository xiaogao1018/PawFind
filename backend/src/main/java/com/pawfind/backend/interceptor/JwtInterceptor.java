
package com.pawfind.backend.interceptor;

import tools.jackson.databind.json.JsonMapper;
import com.pawfind.backend.common.result.Result;
import com.pawfind.backend.util.JwtUtil;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final JsonMapper objectMapper;

    public JwtInterceptor(
            JwtUtil jwtUtil,
            JsonMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

             
        // 放行浏览器的 CORS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
   

        // 1. 获取请求头中的 Token
        String authHeader = request.getHeader("Authorization");

        // 2. 检查 Token 是否存在
        if (authHeader == null
                || !authHeader.startsWith("Bearer ")
                || authHeader.substring(7).isBlank()) {

            writeUnauthorized(response, "缺少或格式错误的 Token");
            return false;
        }

        // 3. 去掉 Bearer 前缀
        String token = authHeader.substring(7).trim();

        try {
            // 4. 验证 Token，获取用户 ID
            Long userId = jwtUtil.getUserIdFromToken(token);

            // 5. 将当前用户 ID 保存到请求中
            request.setAttribute("currentUserId", userId);

            // 6. 验证成功，允许请求继续
            return true;

        } catch (JwtException | IllegalArgumentException e) {

            writeUnauthorized(response, "Token 无效或已过期");
            return false;
        }
    }

    // 统一返回 401 JSON
    private void writeUnauthorized(
            HttpServletResponse response,
            String message) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        objectMapper.writeValue(
                response.getWriter(),
                Result.error(401, message)
        );
    }
}
