package store.piku.back.friend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class FriendsDto {

     private String userId;
     private String nickname;
     private String avatar;

}
