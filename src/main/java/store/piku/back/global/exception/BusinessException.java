package store.piku.back.global.exception;

import lombok.Getter;
import store.piku.back.global.error.ErrorCode;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

//    public BusinessException(String message, ErrorCode errorCode) {
//        super(message);
//        this.errorCode = errorCode;
//    }  // 사용 안되어있음.


    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

}
