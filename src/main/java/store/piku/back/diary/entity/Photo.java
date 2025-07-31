package store.piku.back.diary.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.piku.back.global.entity.BaseEntity;


@Entity
@Table(name = "photos")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Photo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;

    private Boolean represent;

    private Integer photoOrder;

    @ManyToOne
    @JoinColumn(name="diary_id")
    @JsonBackReference
    private Diary diary;


    public Photo(Diary diary, String url, Integer photoOrder) {
        this.diary = diary;
        this.url = url;
        this.represent = false;
        this.photoOrder = photoOrder;
    }

    public void updateRepresent(Boolean represent) {
        this.represent = represent;
    }
    public void updatePhotoOrder(Integer photoOrder) {
        this.photoOrder = photoOrder;
    }
    public void changeRepresent(boolean value) {
        this.represent = value;
    }

}
