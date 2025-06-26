package store.piku.back.comment.entity;

import jakarta.persistence.*;
import lombok.*;
import store.piku.back.diary.entity.Diary;
import store.piku.back.global.entity.BaseEntity;
import store.piku.back.user.entity.User;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id")
    private Diary diary;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent ;

    @OneToMany(mappedBy = "parent")
    private List<Comment> children = new ArrayList<>( );

    public Comment(String content, User user, Diary diary) {
        this.content = content;
        this.user = user;
        this.diary = diary;
    }


}
