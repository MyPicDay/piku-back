package com.pikume.back.creative.adapter.out.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.pikume.back.creative.application.dto.CharacterReferenceImage;
import com.pikume.back.creative.application.port.out.LoadCharacterReferencePort;
import com.pikume.back.global.port.out.LoadObjectPort;
import com.pikume.back.global.util.CharacterAvatarPathNormalizer;
import com.pikume.back.user.application.dto.UserSummaryView;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class CharacterReferenceAdapter implements LoadCharacterReferencePort {

	private static final int FIXED_CHARACTER_REFERENCE_CACHE_MAX_SIZE = 32;

	private final QueryUserSummaryUseCase queryUserSummaryUseCase;
	private final LoadObjectPort loadObjectPort;
	private final Map<String, String> fixedCharacterReferenceCache = new LinkedHashMap<>(16, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
			return size() > FIXED_CHARACTER_REFERENCE_CACHE_MAX_SIZE;
		}
	};

	@Override
	public Optional<CharacterReferenceImage> findByUserId(String userId) {
		return loadAvatarPath(userId)
				.flatMap(avatarPath -> {
					String sourcePath = avatarPath;
					try {
						sourcePath = CharacterAvatarPathNormalizer.normalizeAvatarPath(avatarPath);
						if (CharacterAvatarPathNormalizer.isAbsoluteUrl(sourcePath)) {
							return Optional.empty();
						}
						String imageBase64 = loadImageBase64(sourcePath);
						if (imageBase64.isBlank()) {
							return Optional.empty();
						}
						return Optional.of(new CharacterReferenceImage(sourcePath, imageBase64));
					} catch (RuntimeException e) {
						log.warn("event=character_reference_image_load_failed outcome=failed userId={} sourcePath={} reason={}",
								userId, sourcePath, e.getMessage());
						return Optional.empty();
					}
				});
	}

	private Optional<String> loadAvatarPath(String userId) {
		Map<String, UserSummaryView> usersById = queryUserSummaryUseCase.getUserSummaries(Set.of(userId));
		UserSummaryView user = usersById != null ? usersById.get(userId) : null;
		if (user == null || user.avatarPath() == null || user.avatarPath().isBlank()) {
			return Optional.empty();
		}
		return Optional.of(user.avatarPath());
	}

	private String loadImageBase64(String objectKey) {
		if (!CharacterAvatarPathNormalizer.isFixedCharacterObjectKey(objectKey)) {
			return loadAndEncodeObject(objectKey);
		}
		synchronized (fixedCharacterReferenceCache) {
			return fixedCharacterReferenceCache.computeIfAbsent(objectKey, this::loadAndEncodeObject);
		}
	}

	private String loadAndEncodeObject(String objectKey) {
		byte[] imageBytes = loadObjectPort.loadObject(objectKey);
		if (imageBytes == null || imageBytes.length == 0) {
			return "";
		}
		return Base64.getEncoder().encodeToString(imageBytes);
	}
}
