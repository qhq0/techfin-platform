package com.ccb.techfin.controller.sxd.interceptor;

import com.ccb.techfin.common.util.AesUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Token 拦截器：从请求头 Authorization: Bearer <encrypted-token> 中提取并解密 token。
 * 解密后的用户编号存入 request attribute（key="userId"），供后续业务使用。
 */
@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {

    private final String aesKey;

    public TokenInterceptor(@Value("${aes.key}") String aesKey) {
        this.aesKey = aesKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader)) {
            log.warn("Missing Authorization header for request: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String token;
        if (authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        } else {
            token = authHeader.trim();
        }

        if (!StringUtils.hasText(token)) {
            log.warn("Empty token in Authorization header for request: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        try {
            String plainText = AesUtils.decrypt(token, aesKey);
            // plainText 格式：8 位用户编号 + key，取前 8 位作为用户编号
            if (plainText.length() < 8) {
                log.warn("Decrypted token too short: length={}", plainText.length());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            String userId = plainText.substring(0, 8);
            request.setAttribute("userId", userId);
            log.debug("Token decrypted successfully: userId={}", userId);
        } catch (Exception e) {
            log.warn("Failed to decrypt token for request: {}", request.getRequestURI(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        return true;
    }
}