package store.piku.back.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CookieUtils {

    /**
     * HttpServletRequest에서 쿠키 이름으로 값을 찾아 반환합니다.
     *
     * @param request HttpServletRequest 객체
     * @param key 찾고자 하는 쿠키 이름
     * @return 쿠키 값, 없으면 null 반환
     */
    public String getCookieValue(HttpServletRequest request, String key) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(key)) {
                return cookie.getValue();
            }
        }

        return null; // 해당 키의 쿠키가 없는 경우
    }
}
