package com.pikume.back.user.application.service;

import com.pikume.back.character.adapter.out.persistence.CharacterJpaRepository;
import com.pikume.back.character.domain.Character;
import com.pikume.back.testsupport.FixedCharacterCatalogIsolationConfiguration;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.application.dto.UserIdentityView;
import com.pikume.back.user.application.port.in.QueryUserIdentityUseCase;
import com.pikume.back.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Import(FixedCharacterCatalogIsolationConfiguration.class)
@DisplayName("User identity Character integration")
class UserIdentityQueryServiceIntegrationTest {

	private static final String IMAGE_REFERENCE = "public/characters/fixed/identity-test.webp";

	@Autowired
	private QueryUserIdentityUseCase queryUserIdentityUseCase;

	@Autowired
	private CharacterJpaRepository characterJpaRepository;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Test
	@DisplayName("저장된 Character의 실제 ID로 사용자 아바타 참조를 해석한다")
	void resolvesUserAvatarReferenceFromPersistedCharacterId() {
		Character character = characterJpaRepository.saveAndFlush(Character.fixed(IMAGE_REFERENCE));
		User user = userJpaRepository.saveAndFlush(new User(
				"identity-user@example.com",
				"encoded-password",
				"identity-user",
				character.getId()));

		UserIdentityView result = queryUserIdentityUseCase.queryUserIdentityById(user.getId()).orElseThrow();

		assertThat(result.id()).isEqualTo(user.getId());
		assertThat(result.nickname()).isEqualTo("identity-user");
		assertThat(result.avatarReference().value()).isEqualTo(IMAGE_REFERENCE);
		assertThat(result.avatarReference().absoluteUrl()).isFalse();
		assertThat(result.avatarReference().publiclyAccessible()).isTrue();
	}
}
