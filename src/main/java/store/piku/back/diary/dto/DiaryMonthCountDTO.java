package store.piku.back.diary.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaryMonthCountDTO {
    private int year; // 연도
    private int month; // 월
    private Long count; // 해당 월의 일기 개수

    // JPA 쿼리 결과를 매핑하기 위한 생성자 (Object[] 배열 사용)
    public DiaryMonthCountDTO(Integer year, Integer month, Long count) {
        this.year = year != null ? year : 0;
        this.month = month != null ? month : 0;
        this.count = count != null ? count : 0L;
    }
}
