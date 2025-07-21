package store.piku.back.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.piku.back.global.entity.BaseEntity;

@Entity
@Table(name = "notification")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String receiverId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;
    private String message;
    private Boolean isRead;
    private String relatedId; // 관련 게시물, 관련 친구 id

    public void markAsRead() {
        this.isRead = true;
    }
}
