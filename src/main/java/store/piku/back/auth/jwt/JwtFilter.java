package store.piku.back.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.user.entity.User;
import store.piku.back.user.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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

        String requestURI = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");
        log.debug("[JWT 필터] 요청 URI : {}", requestURI);
        String originHeader = request.getHeader("Origin");

        log.debug("[JWT 필터] 요청 URI : {}", requestURI);
        log.debug("[JWT 필터] Origin 헤더 : {}", originHeader);


        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.debug("[JWT 필터] Authorization 헤더에서 토큰 추출 성공");

            try {
                if (jwtProvider.validateToken(token)) {
                    String email = jwtProvider.getEmailFromToken(token);
                    log.info("[JWT 필터] 토큰 검증 성공 : 이메일={}", email);

                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> {
                                log.warn("[JWT 필터] 사용자 이메일 DB 조회 실패 : {}", email);
                                return new RuntimeException("유저 없음");
                            });

                    CustomUserDetails userDetails = new CustomUserDetails(user.getId(), user.getEmail(), user.getNickname());

                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("[JWT 필터] SecurityContext 인증 완료 : 사용자 ID={}, 이메일={}",
                            user.getId(), user.getEmail());
                }else{
                    log.warn("[JWT 필터] 토큰 유효성 검사 실패");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Access token has expired.");
                    return;
                }
            }catch (Exception e){
                log.error("[JWT 필터] 토큰 처리 중 오류 발생 : {}", e.getMessage());
            }
        }else{
            log.debug("[JWT 필터] Authorization 헤더 토큰 없음");
        }

        chain.doFilter(request, response);
    }
}

