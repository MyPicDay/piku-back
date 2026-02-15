package store.piku.back.recommendation.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScoredDiary {
	private final Long diaryId;
	private final double score;
}
