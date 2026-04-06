package com.pikume.back.character.adapter.out.storage;

import com.pikume.back.character.application.exception.FixedCharacterImageNotFoundException;
import com.pikume.back.character.domain.vo.CharacterCreationType;
import com.pikume.back.global.util.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterImageStorageAdapter")
class CharacterImageStorageAdapterTest {

	@Mock
	private FileUtil fileUtil;

	@Mock
	private Resource resource;

	private CharacterImageStorageAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new CharacterImageStorageAdapter(fileUtil);
	}

	@Test
	@DisplayName("리소스가 없거나 읽을 수 없으면 FixedCharacterImageNotFoundException을 던진다")
	void loadCharacterImageThrowsNotFoundWhenResourceMissing() throws Exception {
		given(fileUtil.loadCharacterImageAsResource(CharacterCreationType.FIXED, null, "missing.png"))
				.willReturn(resource);
		given(resource.exists()).willReturn(false);

		assertThatThrownBy(() -> adapter.loadCharacterImage(CharacterCreationType.FIXED, null, "missing.png"))
				.isInstanceOf(FixedCharacterImageNotFoundException.class);
	}

	@Test
	@DisplayName("리소스 읽기 중 런타임 오류는 not-found로 바꾸지 않고 전파한다")
	void loadCharacterImagePropagatesRuntimeFailure() {
		given(fileUtil.loadCharacterImageAsResource(CharacterCreationType.FIXED, null, "broken.png"))
				.willThrow(new IllegalStateException("broken"));

		assertThatThrownBy(() -> adapter.loadCharacterImage(CharacterCreationType.FIXED, null, "broken.png"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("broken");
	}

	@Test
	@DisplayName("리소스 읽기 중 IOException은 내부 오류로 래핑한다")
	void loadCharacterImageWrapsIoException() throws Exception {
		given(fileUtil.loadCharacterImageAsResource(CharacterCreationType.FIXED, null, "io.png"))
				.willReturn(resource);
		given(resource.exists()).willReturn(true);
		given(resource.isReadable()).willReturn(true);
		given(resource.getInputStream()).willThrow(new IOException("io fail"));

		assertThatThrownBy(() -> adapter.loadCharacterImage(CharacterCreationType.FIXED, null, "io.png"))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("캐릭터 이미지를 읽는 중 오류가 발생했습니다.");
	}
}
