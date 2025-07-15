package store.piku.back.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NicknameChangeResponseDTO {
    private boolean success;    // 성공 여부
    private String message;     // 실패 사유나 안내 메시지
    private String newNickname; // 변경 시도한 닉네임 (선택적)
}
