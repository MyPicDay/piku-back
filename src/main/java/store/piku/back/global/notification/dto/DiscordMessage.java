package store.piku.back.global.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DiscordMessage {
    private String content;
    private List<DiscordEmbed> embeds;
} 