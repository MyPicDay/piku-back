package store.piku.back.character.adapter.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import store.piku.back.character.application.port.in.GetCharacterUseCase;
import store.piku.back.character.application.port.in.ManageCharacterUseCase;
import store.piku.back.character.domain.Character;
import store.piku.back.character.domain.vo.CharacterCreationType;
import store.piku.back.global.util.FileConstants;
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

/**
 * 고정 캐릭터 데이터 초기화
 * 파일 시스템의 이미지와 DB를 동기화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CharacterDataInitializer implements CommandLineRunner {

	private final GetCharacterUseCase getCharacterUseCase;
	private final ManageCharacterUseCase manageCharacterUseCase;

	private static final List<String> SUPPORTED_IMAGE_EXTENSIONS = List.of(".png", ".jpg", ".jpeg", ".gif", ".bmp",
			".webp");

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

	private Set<String> loadExistingFixedCharacterImageUrlsFromDb() {
		List<Character> dbFixedCharacters = getCharacterUseCase.getFixedCharacters();
		Set<String> dbImageFileNames = dbFixedCharacters.stream()
				.map(Character::getImageUrl)
				.collect(Collectors.toSet());
		log.info("DB에 등록된 기존 고정 캐릭터 파일명 수: {}", dbImageFileNames.size());
		return dbImageFileNames;
	}

	private int synchronizeFileSystemCharactersToDb(Set<String> dbImageFileNames) {
		int newCharactersAdded = 0;
		Path fixedCharacterDir = Paths.get(FileConstants.CHARACTERS_BASE_DIR_NAME,
				FileConstants.FIXED_CHARACTER_SUB_DIR_NAME);

		if (Files.notExists(fixedCharacterDir) || !Files.isDirectory(fixedCharacterDir)) {
			log.warn("고정 캐릭터 디렉토리 '{}'를 찾을 수 없거나 디렉토리가 아닙니다.", fixedCharacterDir);
			return 0;
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(fixedCharacterDir)) {
			for (Path imagePath : stream) {
				if (processImageFile(imagePath, dbImageFileNames)) {
					newCharactersAdded++;
				}
			}
		} catch (IOException e) {
			log.error("고정 캐릭터 디렉토리 '{}' 읽기 중 오류 발생: {}", fixedCharacterDir, e.getMessage(), e);
		}
		return newCharactersAdded;
	}

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
				manageCharacterUseCase.saveCharacter(newCharacter);
				log.info("새로운 고정 캐릭터 DB 추가: 파일명 = {}, 실제 파일 위치: {}", imageName, imagePath);
				return true;
			}
		} else {
			log.trace("지원하지 않는 파일 타입이거나 이미지 파일이 아닙니다: {}", imagePath);
		}
		return false;
	}

	private void logMissingCharacterFiles(Set<String> dbImageFileNames) {
		for (String dbFileName : dbImageFileNames) {
			Path correspondingFilePath = Paths.get(FileConstants.CHARACTERS_BASE_DIR_NAME,
					FileConstants.FIXED_CHARACTER_SUB_DIR_NAME, dbFileName);
			if (!Files.exists(correspondingFilePath)) {
				log.warn("DB에 등록된 고정 캐릭터 파일명 '{}'에 해당하는 실제 이미지 파일을 '{}' 경로에서 찾을 수 없습니다.", dbFileName, correspondingFilePath);
			}
		}
	}
}
