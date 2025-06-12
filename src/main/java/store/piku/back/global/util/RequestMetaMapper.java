package store.piku.back.global.util;

import jakarta.servlet.http.HttpServletRequest;
import store.piku.back.global.dto.RequestMetaInfo;
import org.springframework.stereotype.Component;

@Component
public class RequestMetaMapper {

    public RequestMetaInfo extractMetaInfo(HttpServletRequest request) {
        return new RequestMetaInfo(
                request.getScheme(),
                request.getServerName(),
                request.getServerPort(),
                request.getHeader("Host"),
                request.getRequestURL().toString(),
                request.getHeader("User-Agent"),
                extractClientIp(request)
        );
    }

    private static String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            // 다중 프록시 환경일 경우, 제일 앞의 IP를 사용
            ip = ip.split(",")[0];
        }
        return ip;
    }
}
