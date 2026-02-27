package com.pikume.back.social.domain.friend.vo;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class FriendRequestID implements Serializable {

	private String fromUserId;
	private String toUserId;
}
