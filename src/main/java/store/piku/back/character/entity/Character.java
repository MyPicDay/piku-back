package store.piku.back.character.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.piku.back.character.enums.CharacterCreationType;
import store.piku.back.global.entity.BaseEntity;
import store.piku.back.user.entity.User;

@Getter
@Entity
@Table(name = "characters")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)

public class Character extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "user_id", nullable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private CharacterCreationType type;

    public Character(User user, String imageUrl) {
        this.user = user;
        this.imageUrl = imageUrl;
        this.type = CharacterCreationType.AI_GENERATED;
    }

    public Character(String imageUrl, CharacterCreationType type) {
        this.imageUrl = imageUrl;
        this.type = type;
    }
}
