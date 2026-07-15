package com.pikume.back.diary.application.port.out;

public interface AnalyzeDiaryContentPort {

	void analyze(Long diaryId, String content);
}
