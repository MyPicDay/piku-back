package store.piku.back.diary.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "photos")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String url;

    private Boolean represent;

    @ManyToOne
    @JoinColumn(name="diary_id")
    @JsonBackReference
    private Diary diary;

    public Photo(Diary diary, String url) {
        this.diary = diary;
        this.url = url;
        this.represent = false;
    }

    public void updateRepresent(Boolean represent) {
        this.represent = represent;
    }
}
