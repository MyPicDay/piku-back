package store.piku.back.notification.dto.request;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenRequest {

    private String userId;
    private String token;
}
