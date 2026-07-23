package com.pikume.back.recommendation.application.port.in;

public interface RecordTopicInteractionUseCase {

	void recordClick(String userId, String topic);

	void recordLike(String userId, String topic);

	void recordView(String userId, String topic);

	void recordOther(String userId, String topic);
}
