package com.pikume.back.social.domain.friend;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import com.pikume.back.social.domain.friend.vo.FriendRequestID;

@IdClass(FriendRequestID.class)
@Table(name = "friend_request")
@AllArgsConstructor
@Entity
@Getter
public class FriendRequest {

	@Id
	@Column(name = "from_user_id")
	private String fromUserId;

	@Id
	@Column(name = "to_user_id")
	private String toUserId;

	public FriendRequest() {
	}
}
