package com.pikume.back.user.application.exception;

import java.util.Collection;

public class UserAvatarReferenceIntegrityException extends UserException {

	public UserAvatarReferenceIntegrityException(Collection<Long> characterIds) {
		super(
				UserErrorCode.AVATAR_CHARACTER_REFERENCE_INTEGRITY_VIOLATION,
				"사용자 아바타 캐릭터 참조를 해석할 수 없습니다. characterIds=" + characterIds);
	}
}
