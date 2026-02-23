package store.piku.back.diary.application.port.in;

public interface DeleteDiaryUseCase {
	void deleteDiary(Long diaryId, String userId);
}
