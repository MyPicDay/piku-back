package com.pikume.back.character.adapter.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.pikume.back.character.application.port.in.ManageCharacterUseCase;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 고정 캐릭터 데이터 초기화
 * Application use case를 통해 MinIO fixed character catalog와 DB를 동기화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CharacterDataInitializer implements CommandLineRunner {

	private final ManageCharacterUseCase manageCharacterUseCase;

	@Override
	public void run(String... args) throws Exception {
		log.info("고정 캐릭터 데이터 동기화를 시작합니다...");

		int newCharactersAdded = manageCharacterUseCase.synchronizeFixedCharactersFromStorageCatalog();

		if (newCharactersAdded > 0) {
			log.info("새로운 고정 캐릭터 {}개가 DB에 추가되었습니다.", newCharactersAdded);
		} else {
			log.info("MinIO catalog에서 DB에 추가할 새로운 고정 캐릭터를 찾지 못했습니다.");
		}
		log.info("고정 캐릭터 데이터 동기화 완료.");
	}
}
