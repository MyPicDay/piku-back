package store.piku.back.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import store.piku.back.auth.constants.AuthConstants;
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.user.entity.User;
import store.piku.back.user.repository.UserRepository;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(AuthConstants.BEARER_PREFIX)) {
            String token = authHeader.substring(AuthConstants.BEARER_PREFIX.length());

            try {
                if (!jwtProvider.validateToken(token)) {
                    log.warn("[JWT 필터] 토큰 유효성 검사 실패");
                    sendUnauthorizedResponse(response, "Access token has expired.");
                    return;
                }

                authenticateUser(token);
            } catch (Exception e) {
                log.error("[JWT 필터] 토큰 처리 중 오류 발생 : {}", e.getMessage());
                sendUnauthorizedResponse(response, "Token processing error.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void authenticateUser(String token) {
        String email = jwtProvider.getEmailFromToken(token);
        log.info("[JWT 필터] 토큰 검증 성공 : 이메일={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[JWT 필터] 사용자 이메일 DB 조회 실패 : {}", email);
                    return new RuntimeException("유저 없음");
                });

        CustomUserDetails userDetails = new CustomUserDetails(
                user.getId(), user.getEmail(), user.getNickname());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("[JWT 필터] SecurityContext 인증 완료 : 사용자 ID={}, 이메일={}",
                user.getId(), user.getEmail());
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(message);
    }
}
