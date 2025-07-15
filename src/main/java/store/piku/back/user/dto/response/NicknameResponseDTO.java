package store.piku.back.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NicknameResponseDTO {

    @Schema(description = "허용 or 거절",example ="true or false")
    private boolean success;
    @Schema(description = "확인메시지")
    private String message;

}
