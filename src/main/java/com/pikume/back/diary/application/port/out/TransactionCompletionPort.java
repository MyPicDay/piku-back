package com.pikume.back.diary.application.port.out;

import java.util.function.Consumer;

public interface TransactionCompletionPort {

	void runAfterCommit(Runnable task, Consumer<RuntimeException> failureHandler);

	void runAfterCompletion(Runnable afterCommit, Runnable afterRollback);
}
