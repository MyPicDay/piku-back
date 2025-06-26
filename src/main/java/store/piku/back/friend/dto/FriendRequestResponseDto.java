package store.piku.back.friend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class FriendRequestResponseDto {

    private boolean isAccepted;
    private String message;
}
