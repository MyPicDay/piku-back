package com.pikume.back.character.application.service;

import com.pikume.back.character.application.port.out.CanonicalizeFixedCharacterObjectKeyPort;
import com.pikume.back.character.application.port.out.LoadFixedCharacterAssetsPort;
import com.pikume.back.character.application.port.out.LoadFixedCharactersPort;
import com.pikume.back.character.application.port.out.RecordFixedCharacterPort;
import com.pikume.back.character.domain.Character;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("FixedCharacterCatalogSynchronizationService")
class FixedCharacterCatalogSynchronizationServiceTest {

	@InjectMocks
	private FixedCharacterCatalogSynchronizationService service;

	@Mock
	private LoadFixedCharactersPort loadFixedCharactersPort;

	@Mock
	private RecordFixedCharacterPort recordFixedCharacterPort;

	@Mock
	private LoadFixedCharacterAssetsPort loadFixedCharacterAssetsPort;

	@Mock
	private CanonicalizeFixedCharacterObjectKeyPort canonicalizeFixedCharacterObjectKeyPort;

	@Test
	@DisplayName("Storage catalog에서 DB에 없는 고정 캐릭터만 기록한다")
	void recordsOnlyMissingCatalogEntries() {
		given(loadFixedCharactersPort.loadFixedCharacters())
				.willReturn(List.of(Character.fixed("base_image_1.webp")));
		given(loadFixedCharacterAssetsPort.loadFixedCharacterObjectKeys())
				.willReturn(List.of("base_image_1.webp", "base_image_2.webp"));
		given(canonicalizeFixedCharacterObjectKeyPort.canonicalizeFixedCharacterObjectKey("base_image_1.webp"))
				.willReturn("public/characters/fixed/base_image_1.webp");
		given(canonicalizeFixedCharacterObjectKeyPort.canonicalizeFixedCharacterObjectKey("base_image_2.webp"))
				.willReturn("public/characters/fixed/base_image_2.webp");

		int recorded = service.synchronizeFixedCharacterCatalog();

		assertThat(recorded).isEqualTo(1);
		then(recordFixedCharacterPort).should().recordFixedCharacter(argThat(character ->
				character.getImageReference().equals("public/characters/fixed/base_image_2.webp")));
	}

	@Test
	@DisplayName("Storage catalog 조회 실패는 기존 DB catalog를 유지하고 추가하지 않는다")
	void keepsExistingCatalogWhenStorageFails() {
		given(loadFixedCharactersPort.loadFixedCharacters()).willReturn(List.of());
		given(loadFixedCharacterAssetsPort.loadFixedCharacterObjectKeys())
				.willThrow(new IllegalStateException("storage unavailable"));

		assertThat(service.synchronizeFixedCharacterCatalog()).isZero();
		then(recordFixedCharacterPort).shouldHaveNoInteractions();
	}
}
