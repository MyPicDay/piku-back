package com.pikume.back.recommendation.adapter.out.analyzer;

import org.springframework.stereotype.Component;
import com.pikume.back.recommendation.application.dto.DiaryContentAnalysis;
import com.pikume.back.recommendation.application.port.out.ContentAnalyzerPort;

import java.util.*;

/**
 * 로컬 키워드 기반 콘텐츠 분석기
 */
@Component
public class LocalContentAnalyzerAdapter implements ContentAnalyzerPort {

	private static final Map<String, List<String>> TOPIC_KEYWORDS = new LinkedHashMap<>();
	private static final String DEFAULT_TOPIC = "daily";

	static {
		TOPIC_KEYWORDS.put("travel", Arrays.asList("여행", "비행기", "호텔", "관광", "해외", "제주", "숙소", "휴가", "여행지", "공항"));
		TOPIC_KEYWORDS.put("food", Arrays.asList("맛집", "음식", "요리", "카페", "커피", "먹었", "맛있", "식당", "레스토랑", "디저트"));
		TOPIC_KEYWORDS.put("fitness", Arrays.asList("헬스", "운동", "달리기", "러닝", "등산", "헬스장", "근력", "다이어트", "요가", "필라테스"));
		TOPIC_KEYWORDS.put("work", Arrays.asList("회사", "출근", "퇴근", "업무", "회의", "프로젝트", "야근", "마감", "직장", "동료"));
		TOPIC_KEYWORDS.put("family", Arrays.asList("가족", "부모님", "엄마", "아빠", "형제", "자매", "할머니", "할아버지", "아이", "자녀"));
		TOPIC_KEYWORDS.put("friends", Arrays.asList("친구", "모임", "약속", "만남", "우정", "같이", "함께", "동창"));
		TOPIC_KEYWORDS.put("hobby", Arrays.asList("게임", "영화", "책", "독서", "음악", "그림", "사진", "드라마", "취미"));
		TOPIC_KEYWORDS.put("nature", Arrays.asList("바다", "산", "공원", "하늘", "꽃", "나무", "자연", "숲", "강"));
		TOPIC_KEYWORDS.put("reflection", Arrays.asList("생각", "느낌", "감사", "행복", "힘들", "슬픔", "기쁨", "반성", "다짐"));
	}

	@Override
	public DiaryContentAnalysis analyze(String content) {
		if (content == null || content.isBlank()) {
			return new DiaryContentAnalysis(
					DEFAULT_TOPIC,
					Map.of(DEFAULT_TOPIC, 1.0),
					calculateQualityScore(content));
		}

		Map<String, Double> topicScores = calculateTopicScores(content);
		String primaryTopic = determinePrimaryTopic(topicScores);
		double qualityScore = calculateQualityScore(content);

		return new DiaryContentAnalysis(primaryTopic, scoresOrDailyDefault(topicScores), qualityScore);
	}

	private Map<String, Double> calculateTopicScores(String content) {
		Map<String, Double> scores = new LinkedHashMap<>();
		String lowerContent = content.toLowerCase();

		for (Map.Entry<String, List<String>> entry : TOPIC_KEYWORDS.entrySet()) {
			String topic = entry.getKey();
			List<String> keywords = entry.getValue();

			long matchCount = keywords.stream()
					.filter(lowerContent::contains)
					.count();

			if (matchCount > 0) {
				double score = Math.min(1.0, Math.round(matchCount * 0.3 * 10.0) / 10.0);
				scores.put(topic, score);
			}
		}

		return scores;
	}

	private String determinePrimaryTopic(Map<String, Double> topicScores) {
		if (topicScores.isEmpty()) {
			return DEFAULT_TOPIC;
		}

		return topicScores.entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.orElse(DEFAULT_TOPIC);
	}

	private Map<String, Double> scoresOrDailyDefault(Map<String, Double> topicScores) {
		return topicScores.isEmpty() ? Map.of(DEFAULT_TOPIC, 1.0) : topicScores;
	}

	private double calculateQualityScore(String content) {
		if (content == null || content.isBlank()) {
			return 0.1;
		}

		int length = content.length();

		if (length < 20) {
			return 0.2;
		} else if (length < 50) {
			return 0.3;
		} else if (length < 100) {
			return 0.5;
		} else if (length < 200) {
			return 0.6;
		} else if (length < 300) {
			return 0.7;
		} else {
			return 0.9;
		}
	}

}
