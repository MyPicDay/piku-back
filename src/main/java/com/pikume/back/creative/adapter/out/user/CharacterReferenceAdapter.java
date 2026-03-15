package com.pikume.back.creative.adapter.out.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.creative.application.dto.CharacterReferenceImage;
import com.pikume.back.creative.application.port.out.LoadCharacterReferencePort;
import com.pikume.back.global.util.FileUtil;
import com.pikume.back.user.application.port.out.LoadUserPort;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CharacterReferenceAdapter implements LoadCharacterReferencePort {

	private final LoadUserPort loadUserPort;
	private final FileUtil fileUtil;

	@Override
	public Optional<CharacterReferenceImage> findByUserId(String userId) {
		return loadUserPort.findById(userId)
				.filter(user -> user.getAvatar() != null && !user.getAvatar().isBlank())
				.flatMap(user -> {
					String sourcePath = user.getAvatar();
					String imageBase64 = fileUtil.getImageAsBase64(sourcePath);
					if (imageBase64 == null || imageBase64.isBlank()) {
						return Optional.empty();
					}
					return Optional.of(new CharacterReferenceImage(sourcePath, imageBase64));
				});
	}
}
