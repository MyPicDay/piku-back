package store.piku.back.global.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DiscordEmbed {
    private String title;
    private String description;
    private int color;
    private List<EmbedField> fields;
} 