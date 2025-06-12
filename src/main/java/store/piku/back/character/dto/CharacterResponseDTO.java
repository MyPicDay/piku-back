package store.piku.back.character.dto;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import store.piku.back.character.entity.Character;
import store.piku.back.character.enums.CharacterCreationType;
import store.piku.back.file.FileConstants;
import store.piku.back.user.entity.User;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@Getter
public class CharacterResponseDTO {
    private final Long id;
    private final String displayImageUrl;
    private final CharacterCreationType type;

    public CharacterResponseDTO(Long id, String displayImageUrl, CharacterCreationType type) {
        this.id = id;
        this.displayImageUrl = displayImageUrl;
        this.type = type;
    }

    public static CharacterResponseDTO fromEntity(Character character) {
        if (character == null) return null;

        String characterImageBaseApiUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .pathSegment("api", "characters")
                .toUriString();

        String finalImageUrl = null;
        if (character.getImageUrl() != null && !character.getImageUrl().isEmpty()) {
            if (character.getType() == CharacterCreationType.FIXED) {
                finalImageUrl = characterImageBaseApiUrl + "/" + FileConstants.FIXED_CHARACTER_SUB_DIR_NAME + "/" + character.getImageUrl();
            } else if (character.getType() == CharacterCreationType.AI_GENERATED) {
                User user = character.getUser();
                if (user != null && user.getId() != null) {
                    finalImageUrl = characterImageBaseApiUrl + "/" + user.getId() + "/" + character.getImageUrl();
                } else {
                    log.warn("AI 생성 캐릭터의 사용자 정보가 없어 이미지 URL을 완전하게 생성할 수 없습니다: 캐릭터 ID = {}", character.getId());
                }
            }
        }

        return new CharacterResponseDTO(
                character.getId(),
                finalImageUrl,
                character.getType()
        );
    }
} 