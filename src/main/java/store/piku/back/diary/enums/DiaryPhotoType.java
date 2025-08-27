package store.piku.back.diary.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DiaryPhotoType {
    AI_IMAGE("AI_IMAGE"), USER_IMAGE("USER_IMAGE");

    private final String type;

    public static DiaryPhotoType fromString(String type) {
        for (DiaryPhotoType photoType : DiaryPhotoType.values()) {
            if (photoType.getType().equalsIgnoreCase(type)) {
                return photoType;
            }
        }
        throw new IllegalArgumentException("Unknown DiaryPhotoType: " + type);
    }// 차이
}
