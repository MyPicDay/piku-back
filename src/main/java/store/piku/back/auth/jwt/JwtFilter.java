package store.piku.back.auth.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import store.piku.back.auth.enums.Role;
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.user.entity.User;
import store.piku.back.user.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

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
        log.debug("[JWT 필터] 요청 URI : {}", requestURI);
        String originHeader = request.getHeader("Origin");
        log.debug("[JWT 필터] Origin 헤더 : {}", originHeader);


        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.debug("[JWT 필터] Authorization 헤더에서 토큰 추출 성공");

            try {
                if (jwtProvider.validateToken(token)) {
                    Claims claims = jwtProvider.getClaims(token);
                    authenticate(claims);
                } else {
                    log.warn("[JWT 필터] 토큰 유효성 검사 실패");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Access token has expired.");
                    return;
                }
            } catch (Exception e) {
                log.error("[JWT 필터] 토큰 처리 중 오류 발생 : {}", e.getMessage());
            }
        } else {
            log.debug("[JWT 필터] Authorization 헤더 토큰 없음");
        }

        chain.doFilter(request, response);
    }

    private void authenticate(Claims claims) {
        String roleString = claims.get("role", String.class);
        Role role = Role.valueOf(roleString);
        CustomUserDetails userDetails;

        if (role == Role.GUEST) {
            userDetails = createGuestDetails(claims);
        } else {
            userDetails = createUserDetails(claims);
        }
        setAuthenticationInContext(userDetails);
    }

    private CustomUserDetails createGuestDetails(Claims claims) {
        String guestId = (String) claims.get("guestId");
        log.info("[JWT 필터] 게스트 토큰 검증 성공 : guestId={}", guestId);
        return new CustomUserDetails(
            guestId, "anonymous", "익명 사용자",
            Role.GUEST
        );
    }

    private CustomUserDetails createUserDetails(Claims claims) {
        String email = claims.getSubject();
        log.info("[JWT 필터] 토큰 검증 성공 : 이메일={}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[JWT 필터] 사용자 이메일 DB 조회 실패 : {}", email);
                    return new RuntimeException("유저 없음");
                });
        return new CustomUserDetails(user.getId(), user.getEmail(), user.getNickname(), Role.USER);
    }

    private void setAuthenticationInContext(CustomUserDetails userDetails) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);


        if(userDetails.getRole() == Role.GUEST){
            log.info("[JWT 필터] SecurityContext 인증 완료 (게스트) : guestId={}", userDetails.getId());
        } else {
            log.info("[JWT 필터] SecurityContext 인증 완료 : 사용자 ID={}, 이메일={}",
                    userDetails.getId(), userDetails.getEmail());
        }
    }
}

