package store.piku.back.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.piku.back.diary.entity.Diary;
import store.piku.back.global.entity.BaseEntity;
import store.piku.back.user.entity.User;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id")
    private Diary relatedDiary;

    private Boolean isRead;

    public Notification(String receiverId, User sender, NotificationType type, Diary relatedDiary) {
        this.receiverId = receiverId;
        this.sender = sender;
        this.type = type;
        this.relatedDiary = relatedDiary;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
