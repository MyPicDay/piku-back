package store.piku.back.diary.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Status {
    PUBLIC, PRIVATE ,FRIENDS;
}
