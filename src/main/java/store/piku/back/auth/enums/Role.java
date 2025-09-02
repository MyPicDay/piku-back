package store.piku.back.auth.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    USER("ROLE_USER"),
    GUEST("ROLE_GUEST");

    private final String authority;
}
