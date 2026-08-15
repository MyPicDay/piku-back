package com.pikume.back.user.auth.application.port.out;

public interface CheckSignUpCharacterSelectionPort {

	boolean isSelectableFixedCharacter(Long characterId);
}
