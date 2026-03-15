package com.pikume.back.creative.application.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DiaryIllustrationPromptPolicy")
class DiaryIllustrationPromptPolicyTest {

	private final DiaryIllustrationPromptPolicy policy = new DiaryIllustrationPromptPolicy();

	@Test
	@DisplayName("디자인 텍스트 의도가 없으면 텍스트 금지 규칙을 포함한다")
	void createPromptWithoutDesignTextIntent() {
		String prompt = policy.createPrompt("공원에서 산책을 했다");

		assertThat(prompt).contains("Render no text anywhere");
		assertThat(prompt).contains("공원에서 산책을 했다");
	}

	@Test
	@DisplayName("디자인 텍스트 의도가 있으면 제한적 텍스트 허용 규칙을 포함한다")
	void createPromptWithDesignTextIntent() {
		String prompt = policy.createPrompt("포스터가 붙어있는 카페에 앉아 있었다");

		assertThat(prompt).contains("you may render short, readable text");
		assertThat(prompt).contains("포스터가 붙어있는 카페에 앉아 있었다");
	}
}
