package store.piku.back.character.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import store.piku.back.character.entity.Character;
import store.piku.back.character.enums.CharacterCreationType;
import store.piku.back.character.service.CharacterService;
import store.piku.back.file.FileConstants;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CharacterDataInitializer implements CommandLineRunner {

    private final CharacterService characterService;

    private static final List<String> SUPPORTED_IMAGE_EXTENSIONS = List.of(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp");

    @Override
    public void run(String... args) throws Exception {
        log.info("고정 캐릭터 데이터 동기화를 시작합니다...");

        Set<String> dbImageUrls = loadExistingFixedCharacterImageUrlsFromDb();
        int newCharactersAdded = synchronizeFileSystemCharactersToDb(dbImageUrls);
        logMissingCharacterFiles(dbImageUrls);

        if (newCharactersAdded > 0) {
            log.info("새로운 고정 캐릭터 {}개가 DB에 추가되었습니다.", newCharactersAdded);
        } else {
            log.info("파일 시스템에서 DB에 추가할 새로운 고정 캐릭터를 찾지 못했습니다.");
        }
        log.info("고정 캐릭터 데이터 동기화 완료.");
    }

    /**
     * 데이터베이스에서 기존 고정 캐릭터들의 이미지 URL(순수 파일명) 목록을 로드합니다.
     * @return DB에 저장된 고정 캐릭터 이미지 URL(순수 파일명)의 Set
     */
    private Set<String> loadExistingFixedCharacterImageUrlsFromDb() {
        List<Character> dbFixedCharacters = characterService.getFixedCharacters();
        Set<String> dbImageFileNames = dbFixedCharacters.stream()
                .map(Character::getImageUrl)
                .collect(Collectors.toSet());
        log.info("DB에 등록된 기존 고정 캐릭터 파일명 수: {}", dbImageFileNames.size());
        return dbImageFileNames;
    }

    /**
     * 파일 시스템의 고정 캐릭터 디렉토리를 스캔하여, DB에 없는 새로운 캐릭터를 추가합니다.
     * @param dbImageFileNames DB에 이미 등록된 고정 캐릭터 이미지 파일명 Set
     * @return 새로 추가된 캐릭터의 수
     */
    private int synchronizeFileSystemCharactersToDb(Set<String> dbImageFileNames) {
        int newCharactersAdded = 0;
        Path fixedCharacterDir = Paths.get(FileConstants.CHARACTERS_BASE_DIR_NAME, FileConstants.FIXED_CHARACTER_SUB_DIR_NAME);

        if (Files.notExists(fixedCharacterDir) || !Files.isDirectory(fixedCharacterDir)) {
            log.warn("고정 캐릭터 디렉토리 '{}'를 찾을 수 없거나 디렉토리가 아닙니다. 파일 기반 추가 작업을 진행할 수 없습니다.", fixedCharacterDir);
            return 0;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(fixedCharacterDir)) {
            for (Path imagePath : stream) {
                if (processImageFile(imagePath, dbImageFileNames)) {
                    newCharactersAdded++;
                }
            }
        } catch (IOException e) {
            log.error("고정 캐릭터 디렉토리 '{}' 읽기 중 오류 발생: {}. 파일 기반 추가 작업을 중단합니다.", fixedCharacterDir, e.getMessage(), e);
        }
        return newCharactersAdded;
    }

    /**
     * 개별 이미지 파일을 처리하여 DB에 없는 경우 추가하고, 추가 여부를 반환합니다.
     * @param imagePath 처리할 이미지 파일 경로
     * @param dbImageFileNames DB에 이미 등록된 이미지 파일명 Set
     * @return DB에 새로 추가되었으면 true, 아니면 false
     */
    private boolean processImageFile(Path imagePath, Set<String> dbImageFileNames) {
        if (!Files.isRegularFile(imagePath)) {
            return false;
        }

        String imageName = imagePath.getFileName().toString();
        String lowerCaseImageName = imageName.toLowerCase();

        boolean isSupportedImage = SUPPORTED_IMAGE_EXTENSIONS.stream()
                .anyMatch(lowerCaseImageName::endsWith);

        if (isSupportedImage) {
            if (!dbImageFileNames.contains(imageName)) {
                Character newCharacter = new Character(imageName, CharacterCreationType.FIXED);
                characterService.saveCharacter(newCharacter);
                log.info("새로운 고정 캐릭터 DB 추가: 파일명 = {}, 실제 파일 위치: {}", imageName, imagePath);
                return true;
            }
        } else {
            log.trace("지원하지 않는 파일 타입이거나 이미지 파일이 아닙니다: {}", imagePath);
        }
        return false;
    }

    /**
     * DB에는 등록되어 있으나 실제 파일 시스템에는 존재하지 않는 고정 캐릭터 이미지 파일들을 로깅합니다.
     * @param dbImageFileNames DB에 등록된 고정 캐릭터 이미지 파일명 Set
     */
    private void logMissingCharacterFiles(Set<String> dbImageFileNames) {
        for (String dbFileName : dbImageFileNames) {
            Path correspondingFilePath = Paths.get(FileConstants.CHARACTERS_BASE_DIR_NAME, FileConstants.FIXED_CHARACTER_SUB_DIR_NAME, dbFileName);
            if (!Files.exists(correspondingFilePath)) {
                log.warn("DB에 등록된 고정 캐릭터 파일명 '{}'에 해당하는 실제 이미지 파일을 '{}' 경로에서 찾을 수 없습니다. 확인이 필요합니다.", dbFileName, correspondingFilePath);
            }
        }
    }
} 