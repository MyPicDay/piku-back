package store.piku.back.comment.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode {
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_404_1","해당 댓글을 찾을 수 없습니다."),
    DELETED_COMMENT(HttpStatus.BAD_REQUEST, "COMMENT_404_2", "이미 삭제된 댓글입니다."),
    INVALID_PARENT_COMMENT(HttpStatus.BAD_REQUEST, "COMMENT_400_1","대댓글에 대댓글을 달 수 없습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "COMMENT_302", "권한이 없는 사용자입니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "DIARY", "다이어리 정보가 유효하지 않습니다."),
    PARENT_COMMENT_NOT_IN_SAME_DIARY(HttpStatus.BAD_REQUEST, "PARENT_DIARY_404", "댓글이 다이어리에 소속되어 있지 않습니다."),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DB_500_1", "데이터베이스 처리 중 오류가 발생했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_500_1","서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String errorCode;
    private final String message;
}
