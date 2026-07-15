package com.pikume.back.diary.application.port.out;

public interface TransactionCompletionPort {

	void runAfterCommit(Runnable task);

	void runAfterCompletion(Runnable afterCommit, Runnable afterRollback);
}
