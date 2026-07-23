package com.pikume.back.social.domain.friend;

import jakarta.persistence.*;
import lombok.Getter;
import com.pikume.back.social.domain.friend.vo.FriendRequestID;

@IdClass(FriendRequestID.class)
@Table(name = "friend_request")
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

	public FriendRequest(String fromUserId, String toUserId) {
		if (fromUserId.equals(toUserId)) {
			throw new IllegalArgumentException("자기 자신에게 친구 요청을 보낼 수 없습니다.");
		}
		this.fromUserId = fromUserId;
		this.toUserId = toUserId;
	}
}
