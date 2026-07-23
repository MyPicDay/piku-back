package com.pikume.back.social.application.port.out;

import com.pikume.back.social.application.readmodel.SocialParticipantProfile;

import java.util.Map;
import java.util.Set;

public interface LoadSocialParticipantProfilesPort {
	Map<String, SocialParticipantProfile> loadProfiles(Set<String> userIds);
}
