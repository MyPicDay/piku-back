package com.pikume.back.recommendation.application.port.out;

import com.pikume.back.recommendation.domain.UserPreference;

public interface RecordUserPreferencePort {

	UserPreference recordUserPreference(UserPreference preference);
}
