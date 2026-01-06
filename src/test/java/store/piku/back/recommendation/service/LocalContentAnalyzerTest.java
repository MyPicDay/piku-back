package store.piku.back.recommendation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import store.piku.back.recommendation.entity.DiaryMetadata;

import static org.assertj.core.api.Assertions.assertThat;

class LocalContentAnalyzerTest {

	private LocalContentAnalyzer analyzer;

	@BeforeEach
	void setUp() {
		analyzer = new LocalContentAnalyzer();
	}

	@Nested
	@DisplayName("analyze - 일기 내용 분석")
	class Analyze {

		@Test
		@DisplayName("여행 관련 키워드가 포함된 일기는 travel 토픽으로 분류된다")
		void analyzeTravelContent() {
			String content = "오늘 제주도 여행을 다녀왔다. 비행기 타고 호텔에서 묵었는데 너무 좋았어!";

			DiaryMetadata result = analyzer.analyze(1L, content);

			assertThat(result.getPrimaryTopic()).isEqualTo("travel");
			assertThat(result.getTopics()).contains("travel");
		}

		@Test
		@DisplayName("음식 관련 키워드가 포함된 일기는 food 토픽으로 분류된다")
		void analyzeFoodContent() {
			String content = "오늘 친구와 맛집을 갔다. 진짜 맛있는 요리를 먹었다. 카페에서 커피도 마셨어.";

			DiaryMetadata result = analyzer.analyze(1L, content);

			assertThat(result.getPrimaryTopic()).isEqualTo("food");
		}

		@Test
		@DisplayName("운동 관련 키워드가 포함된 일기는 fitness 토픽으로 분류된다")
		void analyzeFitnessContent() {
			String content = "오늘 헬스장에서 운동을 했다. 러닝도 30분 뛰고 근력 운동도 했어.";

			DiaryMetadata result = analyzer.analyze(1L, content);

			assertThat(result.getPrimaryTopic()).isEqualTo("fitness");
		}

		@Test
		@DisplayName("회사 업무 관련 키워드는 work 토픽으로 분류된다")
		void analyzeWorkContent() {
			String content = "오늘 회사에서 회의가 많았다. 프로젝트 마감이 다가와서 야근했다.";

			DiaryMetadata result = analyzer.analyze(1L, content);

			assertThat(result.getPrimaryTopic()).isEqualTo("work");
		}

		@Test
		@DisplayName("여러 토픽이 섞인 일기는 가장 점수가 높은 토픽이 primary가 된다")
		void analyzeMultiTopicContent() {
			String content = "오늘 여행 가서 맛집에서 맛있는 음식을 먹었다. 여행 중 좋은 호텔에서 휴식.";

			DiaryMetadata result = analyzer.analyze(1L, content);

			assertThat(result.getPrimaryTopic()).isIn("travel", "food");
			assertThat(result.getTopics()).contains("travel");
			assertThat(result.getTopics()).contains("food");
		}

		@Test
		@DisplayName("키워드가 없는 일기는 daily 토픽으로 기본 분류된다")
		void analyzeUnknownContent() {
			String content = "그냥 평범한 하루였다.";

			DiaryMetadata result = analyzer.analyze(1L, content);

			assertThat(result.getPrimaryTopic()).isEqualTo("daily");
		}

		@Test
		@DisplayName("빈 문자열은 daily 토픽으로 분류된다")
		void analyzeEmptyContent() {
			DiaryMetadata result = analyzer.analyze(1L, "");

			assertThat(result.getPrimaryTopic()).isEqualTo("daily");
		}
	}

	@Nested
	@DisplayName("calculateQualityScore - 품질 점수 계산")
	class QualityScore {

		@Test
		@DisplayName("20자 미만의 짧은 일기는 낮은 품질 점수를 받는다")
		void shortContentLowScore() {
			DiaryMetadata result = analyzer.analyze(1L, "짧은 글");

			assertThat(result.getQualityScore()).isLessThanOrEqualTo(0.3);
		}

		@Test
		@DisplayName("100자 이상의 일기는 중간 품질 점수를 받는다")
		void mediumContentMediumScore() {
			String content = "오늘은 정말 좋은 하루였다. ".repeat(6);

			DiaryMetadata result = analyzer.analyze(1L, content);

			assertThat(result.getQualityScore()).isGreaterThanOrEqualTo(0.5);
		}

		@Test
		@DisplayName("300자 이상의 길고 상세한 일기는 높은 품질 점수를 받는다")
		void longContentHighScore() {
			String content = "오늘 하루는 정말 의미 있는 하루였다. 아침에 일어나서 운동을 하고, 점심에는 맛있는 음식을 먹었다. ".repeat(5);

			DiaryMetadata result = analyzer.analyze(1L, content);

			assertThat(result.getQualityScore()).isGreaterThanOrEqualTo(0.7);
		}
	}
}
