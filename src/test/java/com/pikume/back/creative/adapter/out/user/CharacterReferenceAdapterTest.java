package com.pikume.back.creative.adapter.out.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.global.util.FileUtil;
import com.pikume.back.user.application.port.out.LoadUserPort;
import com.pikume.back.user.domain.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterReferenceAdapter")
class CharacterReferenceAdapterTest {

	@InjectMocks
	private CharacterReferenceAdapter characterReferenceAdapter;

	@Mock
	private LoadUserPort loadUserPort;

	@Mock
	private FileUtil fileUtil;

	@Test
	@DisplayName("사용자 아바타를 creative 참조 이미지로 변환한다")
	void returnsCharacterReferenceImage() {
		User user = mock(User.class);
		given(user.getAvatar()).willReturn("avatar.png");
		given(loadUserPort.findById("user-1")).willReturn(Optional.of(user));
		given(fileUtil.getImageAsBase64("avatar.png")).willReturn("base64-avatar");

		var result = characterReferenceAdapter.findByUserId("user-1");

		assertThat(result).isPresent();
		assertThat(result.get().sourcePath()).isEqualTo("avatar.png");
		assertThat(result.get().imageBase64()).isEqualTo("base64-avatar");
	}

	@Test
	@DisplayName("아바타 이미지를 읽지 못하면 빈 결과를 반환한다")
	void returnsEmptyWhenAvatarCannotBeRead() {
		User user = mock(User.class);
		given(user.getAvatar()).willReturn("missing.png");
		given(loadUserPort.findById("user-1")).willReturn(Optional.of(user));
		given(fileUtil.getImageAsBase64("missing.png")).willReturn(null);

		var result = characterReferenceAdapter.findByUserId("user-1");

		assertThat(result).isEmpty();
	}
}
