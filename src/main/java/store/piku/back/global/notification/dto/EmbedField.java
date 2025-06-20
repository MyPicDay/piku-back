package store.piku.back.global.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EmbedField {
    private String name;
    private String value;
    private boolean inline;
} 