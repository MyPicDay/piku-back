package com.pikume.back.diary.adapter.out.transaction;

import com.pikume.back.diary.application.port.out.TransactionCompletionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Consumer;

@Component
public class SpringTransactionCompletionAdapter implements TransactionCompletionPort {
	private final TransactionTemplate requiresNewTransaction;

	public SpringTransactionCompletionAdapter(PlatformTransactionManager transactionManager) {
		this.requiresNewTransaction = new TransactionTemplate(transactionManager);
		this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@Override
	public void runAfterCommit(Runnable task, Consumer<RuntimeException> failureHandler) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			executeInNewTransaction(task, failureHandler);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				executeInNewTransaction(task, failureHandler);
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

	private void executeInNewTransaction(Runnable task, Consumer<RuntimeException> failureHandler) {
		try {
			requiresNewTransaction.executeWithoutResult(status -> task.run());
		} catch (RuntimeException exception) {
			failureHandler.accept(exception);
		}
	}
}
