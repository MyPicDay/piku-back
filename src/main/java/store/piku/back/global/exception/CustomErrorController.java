package store.piku.back.global.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import store.piku.back.global.error.ErrorResponse;

/**
 * Spring Boot 기본 /error 엔드포인트를 커스터마이징
 * SSE 등 비동기 요청에서 발생하는 에러를 안전하게 처리
 */
@Slf4j
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ErrorResponse> handleError(HttpServletRequest request) {
        // 응답이 이미 커밋되었는지 확인
        if (request.isAsyncStarted() || request.getDispatcherType().name().equals("ASYNC")) {
            log.debug("비동기 요청 에러 처리 - 응답 이미 커밋됨, 조용히 처리");
            // 이미 커밋된 응답에는 아무것도 하지 않음
            return null;
        }

        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        if (status != null) {
            try {
                httpStatus = HttpStatus.valueOf(Integer.parseInt(status.toString()));
            } catch (Exception e) {
                log.warn("Invalid HTTP status code: {}", status);
            }
        }

        String errorMessage = message != null ? message.toString() : "알 수 없는 오류가 발생했습니다.";
        
        if (exception != null) {
            log.error("에러 발생: status={}, message={}, exception={}", 
                    httpStatus.value(), errorMessage, exception.getClass().getName());
        } else {
            log.warn("에러 발생: status={}, message={}", httpStatus.value(), errorMessage);
        }

        ErrorResponse errorResponse = new ErrorResponse(httpStatus.value(), errorMessage);
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }
}
