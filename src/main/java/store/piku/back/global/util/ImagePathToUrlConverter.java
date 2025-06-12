package store.piku.back.global.util;

import store.piku.back.global.dto.RequestMetaInfo;
import org.springframework.stereotype.Component;

@Component
public class ImagePathToUrlConverter {

    /**
     * Diary 이미지 경로를 전체 URL로 변환합니다. (DiaryController 참고)
     *
     * @param imagePath DB에 저장된 이미지 경로 (예: "userId/filename.png")
     * @param requestMetaInfo HttpRequest 정보 (예: scheme="http", domain="localhost", port=8080)
     * @return 완성된 URL (예: "http://localhost:8080/api/diaries/images/userId/filename.png"), 변환 불가 시 빈 문자열
     */
    public String diaryImageUrl(String imagePath, RequestMetaInfo requestMetaInfo){
        if (imagePath == null || imagePath.isEmpty() || requestMetaInfo == null) {
            // TODO: 기본 이미지 URL 반환 등 예외 처리
            return "";
        }
        return String.format("%s://%s:%d/api/diaries/images/%s",
                requestMetaInfo.scheme(),
                requestMetaInfo.domain(),
                requestMetaInfo.port(),
                imagePath);
    }

    public String extractImagePathFromUrl(String fullUrl, RequestMetaInfo requestMetaInfo) {
        if (fullUrl == null || fullUrl.isEmpty() || requestMetaInfo == null) {
            return "";
        }

        // 앞부분: "http://localhost:8080/api/diaries/images/"
        String baseUrlPrefix = String.format("%s://%s:%d/api/diaries/images/",
                requestMetaInfo.scheme(),
                requestMetaInfo.domain(),
                requestMetaInfo.port());

        if (fullUrl.startsWith(baseUrlPrefix)) {
            return fullUrl.substring(baseUrlPrefix.length());
        }

        // 매칭되지 않으면 빈 문자열 반환
        return "";
    }


    /**
     * Character 이미지 경로를 전체 URL로 변환합니다. (CharacterController 참고)
     *
     * @param imagePath DB에 저장된 이미지 경로 (예: "character_image.png")
     * @param requestMetaInfo HttpRequest 정보 (예: scheme="http", domain="localhost", port=8080)
     * @return 완성된 URL (예: "http://localhost:8080/api/characters/fixed/character_image.png"), 변환 불가 시 빈 문자열
     */
    public String fixedCharacterImageUrl(String imagePath, RequestMetaInfo requestMetaInfo){
        if (imagePath == null || imagePath.isEmpty() || requestMetaInfo == null) {
            // TODO: 기본 이미지 URL 반환 등 예외 처리
            return "";
        }
        return String.format("%s://%s:%d/api/characters/fixed/%s",
                requestMetaInfo.scheme(),
                requestMetaInfo.domain(),
                requestMetaInfo.port(),
                imagePath);
    }

    /**
     * Character 이미지 경로를 전체 URL로 변환합니다. (CharacterController 참고)
     *
     * @param imagePath DB에 저장된 이미지 경로 (예: "characters/fixed/base_image_1.png")
     * @param requestMetaInfo HttpRequest 정보 (예: scheme="http", domain="localhost", port=8080)
     * @return 완성된 URL (예: "http://localhost:8080/api/characters/fixed/character_image.png"), 변환 불가 시 빈 문자열
     */
    public String userAvatarImageUrl(String imagePath, RequestMetaInfo requestMetaInfo){
        if (imagePath == null || imagePath.isEmpty() || requestMetaInfo == null) {
            // TODO: 기본 이미지 URL 반환 등 예외 처리
            return "";
        }
        return String.format("%s://%s:%d/api/%s",
                requestMetaInfo.scheme(),
                requestMetaInfo.domain(),
                requestMetaInfo.port(),
                imagePath);
    }
}
