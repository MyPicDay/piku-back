package store.piku.back.character.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import store.piku.back.character.entity.Character;
import store.piku.back.character.enums.CharacterCreationType;
import store.piku.back.character.repository.CharacterRepository;
import store.piku.back.file.FileUtil;
import store.piku.back.user.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static store.piku.back.file.FileConstants.CHARACTERS_BASE_DIR_NAME;
import static store.piku.back.file.FileConstants.FIXED_CHARACTER_SUB_DIR_NAME;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final FileUtil fileUtil;

    // 고정 캐릭터 조회
    public List<Character> getFixedCharacters() {
        return characterRepository.findByType(CharacterCreationType.FIXED);
    }

    public Character getCharacterById(Long id) {
        return characterRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Character not found with id: {}", id);
                    return new IllegalArgumentException("Character not found with id: " + id);
                });
    }

    /**
     * AI 캐릭터를 생성하고 저장합니다.
     * @param user 캐릭터를 생성한 사용자
     * @param imageFile AI에 의해 생성된 이미지 파일
     * @param desiredName (선택적) 원하는 파일명
     * @return 저장된 Character 엔티티
     */
    @Transactional
    public Character createAiCharacter(User user, MultipartFile imageFile, String desiredName) {
        if (user == null) {
            throw new IllegalArgumentException("AI 캐릭터 생성을 위해서는 사용자 정보가 필요합니다.");
        }
        // FileUtil.saveCharacterImage는 순수 파일명을 반환
        String savedFileName = fileUtil.saveCharacterImage(imageFile, CharacterCreationType.AI_GENERATED, String.valueOf(user.getId()), desiredName);
        
        Character newCharacter = new Character(user, savedFileName); // 생성자 호출 시 user와 순수 파일명 전달
        // newCharacter.setType(CharacterCreationType.AI_GENERATED); // Character 생성자에서 이미 AI_GENERATED로 설정됨
        return characterRepository.save(newCharacter);
    }

    // 캐릭터 저장 또는 업데이트 (고정 캐릭터 초기화 등에도 사용 가능)
    @Transactional
    public Character saveCharacter(Character character) {
        return characterRepository.save(character);
    }

    /**
     * 파일명을 기반으로 고정 캐릭터 이미지 파일을 Resource로 로드합니다.
     * @param fileName 로드할 이미지 파일명 (예: "character_preset_1.png")
     * @return 로드된 이미지 Resource 객체
     */
    public Resource getFixedCharacterImageAsResource(String fileName) {
        // FIXED 타입 캐릭터는 userId가 필요 없음 (null 전달)
        return fileUtil.loadCharacterImageAsResource(CharacterCreationType.FIXED, null, fileName);
    }

    public boolean isCharacterFixedImageExists(Long characterId) {
        Optional<Character> character = characterRepository.findById(characterId);
        return character.isPresent() && character.get().getType() == CharacterCreationType.FIXED;
    }

    public String getFixedCharacterImageUrl(Long characterId) {
        Optional<Character> character = characterRepository.findById(characterId);
        if (character.isPresent() && character.get().getType() == CharacterCreationType.FIXED) {
            return CHARACTERS_BASE_DIR_NAME + "/" + FIXED_CHARACTER_SUB_DIR_NAME + "/" + character.get().getImageUrl();
        } else {
            log.warn("고정 캐릭터 이미지가 존재하지 않거나 잘못된 캐릭터 ID입니다: {}", characterId);
            return null;
        }
    }



    // 향후 AI 생성 캐릭터 이미지 로드 위한 메소드 (예시)
    // public Resource getAiCharacterImageAsResource(Long userId, String fileName) {
    //     return fileUtil.loadCharacterImageAsResource(CharacterCreationType.AI_GENERATED, String.valueOf(userId), fileName);
    // }
} 