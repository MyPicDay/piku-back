package store.piku.back.diary.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.piku.back.diary.enums.Status;
import store.piku.back.global.entity.BaseEntity;
import store.piku.back.user.entity.User;

import java.time.LocalDate;

@Entity
@Table
@NoArgsConstructor
@Getter
public class Diary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDate date;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Diary(String content, Status status, LocalDate date, User user) {
        this.content = content;
        this.status = status;
        this.date = date;
        this.user = user;
    }

}
