package com.pikume.back.social.application.port.out;

public interface VerifySocialParticipantPort {
	boolean participantExists(String userId);
}
