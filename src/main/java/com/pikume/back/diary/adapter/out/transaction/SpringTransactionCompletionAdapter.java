package com.pikume.back.diary.adapter.out.transaction;

import com.pikume.back.diary.application.port.out.TransactionCompletionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class SpringTransactionCompletionAdapter implements TransactionCompletionPort {

	@Override
	public void runAfterCommit(Runnable task) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			task.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				task.run();
			}
		});
	}

	@Override
	public void runAfterCompletion(Runnable afterCommit, Runnable afterRollback) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			afterCommit.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				afterCommit.run();
			}

			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) {
					afterRollback.run();
				}
			}
		});
	}
}
