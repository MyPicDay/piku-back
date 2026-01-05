package store.piku.back.diary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false)
    private Long diaryId;

    @Column(nullable = false)
    private LocalDateTime clickedAt;

    private Integer viewDurationSeconds;

    @Builder
    public FeedClick(String userId, Long diaryId) {
        this.userId = userId;
        this.diaryId = diaryId;
        this.clickedAt = LocalDateTime.now();
    }

    public void updateViewDuration(Integer seconds) {
        this.viewDurationSeconds = seconds;
    }
}
