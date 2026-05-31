package com.pikume.back.character.adapter.config;

import com.pikume.back.character.application.port.in.ManageCharacterUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterDataInitializer")
class CharacterDataInitializerTest {

	@Mock
	private ManageCharacterUseCase manageCharacterUseCase;

	@Test
	@DisplayName("Application use case로 fixed character catalog 동기화를 위임한다")
	void delegatesFixedCharacterCatalogSynchronizationToUseCase() throws Exception {
		CharacterDataInitializer initializer = new CharacterDataInitializer(manageCharacterUseCase);

		initializer.run();

		verify(manageCharacterUseCase).synchronizeFixedCharactersFromStorageCatalog();
	}

	@Test
	@DisplayName("initializer는 catalog 조회 포트를 직접 사용하지 않는다")
	void initializerDoesNotDependOnStorageCatalogPort() {
		Class<?>[] constructorParameterTypes = CharacterDataInitializer.class.getDeclaredConstructors()[0].getParameterTypes();

		then(manageCharacterUseCase).shouldHaveNoInteractions();
		org.assertj.core.api.Assertions.assertThat(constructorParameterTypes)
				.containsExactly(ManageCharacterUseCase.class);
	}
}
