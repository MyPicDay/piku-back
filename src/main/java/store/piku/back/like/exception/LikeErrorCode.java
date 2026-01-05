package store.piku.back.like.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LikeErrorCode {

    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "LIKE_404_1", "해당 일기를 찾을 수 없습니다."),
    ALREADY_LIKED(HttpStatus.CONFLICT, "LIKE_409_1", "이미 좋아요한 일기입니다."),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "LIKE_404_2", "좋아요 기록을 찾을 수 없습니다."),
    CANNOT_LIKE_OWN_DIARY(HttpStatus.BAD_REQUEST, "LIKE_400_1", "자신의 일기에는 좋아요할 수 없습니다.");

    private final HttpStatus status;
    private final String errorCode;
    private final String message;
}
