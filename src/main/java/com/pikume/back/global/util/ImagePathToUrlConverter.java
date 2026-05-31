package com.pikume.back.global.util;

import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.port.out.ResolveImageUrlPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ImagePathToUrlConverter {

    private final ResolveImageUrlPort resolveImageUrlPort;

    public ImagePathToUrlConverter(ResolveImageUrlPort resolveImageUrlPort) {
        this.resolveImageUrlPort = resolveImageUrlPort;
    }

    /**
     * Diary 이미지 경로를 전체 URL로 변환합니다. (DiaryController 참고)
     *
     * @param imagePath DB에 저장된 이미지 경로 (예: "userId/filename.png")
     * @param requestMetaInfo HttpRequest 정보 (예: scheme="http", domain="localhost", port=8080)
     * @return 완성된 URL (예: "http://localhost:8080/api/diary/images/userId/filename.png"), 변환 불가 시 빈 문자열
     */
    public String diaryImageUrl(String imagePath, RequestMetaInfo requestMetaInfo){
        if (imagePath == null || imagePath.isEmpty() || requestMetaInfo == null) {
            // TODO: 기본 이미지 URL 반환 등 예외 처리
            return "";
        }
        return String.format("%s://%s:%d/api/diary/images/%s",
                requestMetaInfo.scheme(),
                requestMetaInfo.domain(),
                requestMetaInfo.port(),
                imagePath);
    }

    public String extractImagePathFromUrl(String fullUrl, RequestMetaInfo requestMetaInfo) {
        if (fullUrl == null || fullUrl.isEmpty() || requestMetaInfo == null) {
            return "";
        }

        // 앞부분: "http://localhost:8080/api/diary/images/"
        String baseUrlPrefix = String.format("%s://%s:%d/api/diary/images/",
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
     * Character 이미지 경로를 storage public URL로 변환합니다.
     *
     * @param imagePath DB에 저장된 이미지 경로 (예: "character_image.png")
     * @param requestMetaInfo HttpRequest 정보
     * @return 완성된 storage public URL, 변환 불가 시 빈 문자열
     */
    public String fixedCharacterImageUrl(String imagePath, RequestMetaInfo requestMetaInfo){
        if (imagePath == null || imagePath.isBlank()) {
            // TODO: 기본 이미지 URL 반환 등 예외 처리
            return "";
        }
        String objectKey;
        try {
            objectKey = CharacterAvatarPathNormalizer.normalizeFixedCharacterObjectKey(imagePath);
        } catch (IllegalArgumentException e) {
            log.warn("event=fixed_character_image_url_convert_failed outcome=skipped imagePath={} reason={}",
                    imagePath,
                    e.getMessage());
            return "";
        }
        if (CharacterAvatarPathNormalizer.isAbsoluteUrl(objectKey)) {
            return objectKey;
        }
        return resolveImageUrlPort.getPhotoUrl(objectKey, true);
    }

    /**
     * Character 이미지 경로를 storage public URL로 변환합니다.
     *
     * @param imagePath DB에 저장된 이미지 경로 (예: "characters/fixed/base_image_1.png")
     * @param requestMetaInfo HttpRequest 정보
     * @return 완성된 storage public URL, 변환 불가 시 빈 문자열
     */
    public String userAvatarImageUrl(String imagePath, RequestMetaInfo requestMetaInfo){
        if (imagePath == null || imagePath.isBlank()) {
            // TODO: 기본 이미지 URL 반환 등 예외 처리
            return "";
        }
        String normalizedPath;
        try {
            normalizedPath = CharacterAvatarPathNormalizer.normalizeAvatarPath(imagePath);
        } catch (IllegalArgumentException e) {
            log.warn("event=user_avatar_image_url_convert_failed outcome=skipped imagePath={} reason={}",
                    imagePath,
                    e.getMessage());
            return "";
        }
        if (CharacterAvatarPathNormalizer.isAbsoluteUrl(normalizedPath)) {
            return normalizedPath;
        }
        return resolveImageUrlPort.getPhotoUrl(
                normalizedPath,
                CharacterAvatarPathNormalizer.isPublicObjectKey(normalizedPath));
    }
}
