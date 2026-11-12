package com.pawfind.backend.interceptor;

import com.pawfind.backend.common.exception.ForbiddenException;
import com.pawfind.backend.common.exception.InvalidCredentialsException;
import com.pawfind.backend.entity.SysUser;
import com.pawfind.backend.mapper.SysUserMapper;
import com.pawfind.backend.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    private static final PathPattern ADMIN_PATH = PathPatternParser.defaultInstance.parse("/api/admin/**");
    private static final List<PathPattern> PUBLIC_READ_PATHS = List.of(
            "/api/pet-found/{id:[0-9]+}/comments",
            "/api/pet-lost/{id:[0-9]+}/comments",
            "/api/pet-found", "/api/pet-found/{id:[0-9]+}",
            "/api/pet-found/{id:[0-9]+}/images", "/api/pet-found/{id:[0-9]+}/images/{attachmentId:[0-9]+}",
            "/api/pet-lost", "/api/pet-lost/{id:[0-9]+}", "/api/pet-lost/{id:[0-9]+}/clues",
            "/api/pet-lost/{id:[0-9]+}/images", "/api/pet-lost/{id:[0-9]+}/images/{attachmentId:[0-9]+}"
    ).stream().map(PathPatternParser.defaultInstance::parse).toList();

    private final JwtUtil jwtUtil;
    private final SysUserMapper users;

    public JwtInterceptor(JwtUtil jwtUtil, SysUserMapper users) {
        this.jwtUtil = jwtUtil;
        this.users = users;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        if ("OPTIONS".equals(method)) {
            return true;
        }

        PathContainer path = ServletRequestPathUtils.getParsedRequestPath(request).pathWithinApplication();
        boolean publicRead = ("GET".equals(method) || "HEAD".equals(method))
                && PUBLIC_READ_PATHS.stream().anyMatch(pattern -> pattern.matches(path));
        String authorization = request.getHeader("Authorization");
        if (publicRead && authorization == null) {
            return true;
        }
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank()) {
            throw new InvalidCredentialsException("缺少或格式错误的 Token");
        }

        Long userId;
        try {
            userId = jwtUtil.getUserIdFromToken(authorization.substring(7).trim());
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidCredentialsException("Token 无效或已过期");
        }

        SysUser user = users.selectById(userId);
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new InvalidCredentialsException("当前账号不可用");
        }
        if (ADMIN_PATH.matches(path) && !"ADMIN".equals(user.getRole())) {
            throw new ForbiddenException("仅管理员可以访问");
        }

        request.setAttribute("currentUserId", userId);
        request.setAttribute("currentUserRole", user.getRole());
        return true;
    }
}
